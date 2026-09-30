package com.hevyclone.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Authenticated session against the self-hosted Supabase (GoTrue). */
class CloudSession(
    val email: String,
    val userId: String,
    @Volatile var access: String,
    @Volatile var refresh: String,
    @Volatile var expiresAt: Long,
)

/**
 * Account + cloud sync. One full snapshot (workouts, routines, settings, tombstones)
 * per account on the server; last-write-wins by client timestamp, union-merge on
 * conflict so a fresh device never loses local data on first login.
 */
object Cloud {
    private const val BASE = "https://api.webtvmedia.net"
    private const val ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzc2ODY2NDUyLCJleHAiOjIwOTIyMjY0NTJ9.jOfn90sK6YeY6LRRuwzdiZpiO-s8pN4Ozr418B8iRXE"
    private const val PREFS = "cloud"

    private lateinit var prefs: android.content.SharedPreferences
    private var appCtx: Context? = null
    private val json = Json { ignoreUnknownKeys = true }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ---- UI state ----
    var session by mutableStateOf<CloudSession?>(null)
        private set
    var skipped by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)
    var syncStatus by mutableStateOf<String?>(null)
    var syncTick by mutableIntStateOf(0)

    // ---- sync meta (persisted) ----
    private var dirtyAt = 0L        // local mutation clock (last change not confirmed pushed)
    private var pushedTs = 0L       // ts of last confirmed push
    private var lastSeenRemoteTs = 0L
    private var lastAccount: String? = null
    @Volatile private var syncing = false
    private var debounce: Job? = null

    // ---- tombstones: deletions that must propagate to other devices ----
    private var delW = mutableListOf<Long>()
    private var delR = mutableListOf<String>()

    fun init(ctx: Context) {
        if (this::prefs.isInitialized) return
        appCtx = ctx.applicationContext
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        AvatarCache.reload(ctx)
        dirtyAt = prefs.getLong("dirtyAt", 0L)
        pushedTs = prefs.getLong("pushedTs", 0L)
        lastSeenRemoteTs = prefs.getLong("lastSeenRemoteTs", 0L)
        lastAccount = prefs.getString("lastAccount", null)
        skipped = prefs.getBoolean("skipped", false)
        delW = prefs.getString("delW", null)?.let { runCatching { json.decodeFromString<List<Long>>(it) }.getOrNull() }?.toMutableList() ?: mutableListOf()
        delR = prefs.getString("delR", null)?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }?.toMutableList() ?: mutableListOf()
        loadSession()
    }

    // ---- session at rest: AndroidKeyStore AES-GCM box (tokens never on disk in clear) ----

    private fun loadSession() {
        prefs.getString("session_box", null)?.let { boxed ->
            val blob = SecretBox.decrypt(boxed)
            if (blob != null) {
                runCatching {
                    val o = json.parseToJsonElement(blob).jsonObject
                    session = CloudSession(
                        email = o["email"]?.jsonPrimitive?.content ?: "",
                        userId = o["userId"]?.jsonPrimitive?.content ?: "",
                        access = o["access"]?.jsonPrimitive?.content ?: "",
                        refresh = o["refresh"]?.jsonPrimitive?.content ?: "",
                        expiresAt = o["expiresAt"]?.jsonPrimitive?.long ?: 0L,
                    )
                }
                return
            }
            // Undecryptable box (keystore invalidated, e.g. device wipe/restore) → session lost, user re-logs in.
            prefs.edit().remove("session_box").apply()
        }
        // Migration from the legacy plaintext storage (≤ v1.34): re-box, then scrub the clear copies.
        val legacyAccess = prefs.getString("access", null)
        if (legacyAccess != null) {
            session = CloudSession(
                email = prefs.getString("email", "") ?: "",
                userId = prefs.getString("userId", "") ?: "",
                access = legacyAccess,
                refresh = prefs.getString("refresh", "") ?: "",
                expiresAt = prefs.getLong("expiresAt", 0L),
            )
            persistSession()
        }
    }

    private object SecretBox {
        private const val TAG = "SecretBox"
        private const val ALIAS = "hevy_cloud_master"

        private fun cipher(): javax.crypto.Cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")

        private fun key(): javax.crypto.SecretKey {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getKey(ALIAS, null) as? javax.crypto.SecretKey)?.let { return it }
            val gen = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
            gen.init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return gen.generateKey()
        }

        fun encrypt(plain: String): String? = runCatching {
            val c = cipher()
            c.init(javax.crypto.Cipher.ENCRYPT_MODE, key())
            val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(c.iv, android.util.Base64.NO_WRAP) + ":" +
                android.util.Base64.encodeToString(ct, android.util.Base64.NO_WRAP)
        }.onFailure { android.util.Log.w(TAG, "encrypt failed", it) }.getOrNull()

        fun decrypt(boxed: String): String? = runCatching {
            val parts = boxed.split(":", limit = 2)
            if (parts.size != 2) return null
            val c = cipher()
            c.init(
                javax.crypto.Cipher.DECRYPT_MODE, key(),
                javax.crypto.spec.GCMParameterSpec(128, android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)),
            )
            String(c.doFinal(android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)), Charsets.UTF_8)
        }.onFailure { android.util.Log.w(TAG, "decrypt failed", it) }.getOrNull()
    }

    // ================= auth =================

    /** Returns null on success, else a user-facing (FR-first via L10n at call site) error. */
    suspend fun signUp(email: String, password: String): String? = authenticate("signup", email, password)
    suspend fun signIn(email: String, password: String): String? = authenticate("token?grant_type=password", email, password)

    private suspend fun authenticate(kind: String, emailRaw: String, password: String): String? {
        val email = emailRaw.trim().lowercase()
        busy = true
        authError = null
        try {
            val body = buildJsonObject { put("email", email); put("password", password) }.toString()
            val (code, resp) = withContext(Dispatchers.IO) { http("POST", "/auth/v1/$kind", body, null) }
            if (code !in 200..299) return mapAuthError(code, resp).also { authError = it }
            val obj = json.parseToJsonElement(resp).jsonObject
            val token = obj["access_token"]?.jsonPrimitive?.content
            if (token == null) {
                // account created but not confirmed (should not happen: server autoconfirms)
                return "Compte créé mais non confirmé — contacte l'administrateur.".also { authError = it }
            }
            applySession(
                email = email,
                userId = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: "",
                token = token,
                refresh = obj["refresh_token"]?.jsonPrimitive?.content ?: "",
                expiresIn = obj["expires_in"]?.jsonPrimitive?.long ?: 3600L,
            )
            return null
        } catch (e: Exception) {
            return "Connexion impossible — vérifie Internet.".also { authError = it }
        } finally {
            busy = false
        }
    }

    private suspend fun applySession(email: String, userId: String, token: String, refresh: String, expiresIn: Long) {
        session = CloudSession(email, userId, token, refresh, System.currentTimeMillis() + expiresIn * 1000L)
        onLoggedIn(email)
        persistSession()
        syncNow()
    }

    private suspend fun onLoggedIn(email: String) {
        if (lastAccount != null && lastAccount != email) {
            // Account switch: this device currently holds the previous account's data.
            withContext(Dispatchers.Main) { Repo.wipe(markDirty = false) }
            clearTombstones()
            dirtyAt = 0L; pushedTs = 0L; lastSeenRemoteTs = 0L
        } else if (lastAccount == null && (Repo.workouts.isNotEmpty() || Repo.routines.isNotEmpty())) {
            // First bind of an existing local profile → make sure local data is pushed (merged with remote).
            dirtyAt = System.currentTimeMillis()
        }
        lastAccount = email
        skipped = false
        persistMeta()
    }

    fun signOut() {
        val s = session ?: return
        val refresh = s.refresh
        val access = s.access
        scope.launch(Dispatchers.IO) {
            runCatching { http("POST", "/auth/v1/logout", "{}", access) }
        }
        prefs.edit()
            .remove("session_box")
            .remove("access").remove("refresh").remove("expiresAt").remove("userId").remove("email")
            .remove("pkce_verifier").remove("pkce_state").remove("pkce_ts")
            .apply()
        session = null
        skipped = false
        syncStatus = null
    }

    // ================= Google sign-in (PKCE + state via browser / custom tab) =================

    private const val REDIRECT = "hevyclone://auth-callback"
    private const val PKCE_TTL_MS = 10 * 60_000L

    /** Opens the browser on the GoTrue /authorize endpoint with a fresh PKCE pair + CSRF state. */
    fun startGoogleAuth(ctx: Context) {
        val verifier = generatePkceVerifier()
        val state = generatePkceVerifier()
        prefs.edit()
            .putString("pkce_verifier", verifier)
            .putString("pkce_state", state)
            .putLong("pkce_ts", System.currentTimeMillis())
            .apply()
        val challenge = pkceChallenge(verifier)
        val url = "$BASE/auth/v1/authorize?provider=google&redirect_to=" +
            java.net.URLEncoder.encode(REDIRECT, "UTF-8") +
            "&flow_type=pkce&code_challenge=$challenge&code_challenge_method=s256&state=" +
            java.net.URLEncoder.encode(state, "UTF-8")
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Deep-link entry: hevyclone://auth-callback?code=...&state=... → exchange for a session.
     * The state must match the one issued in startGoogleAuth (single-use, 10 min TTL):
     * a forged or replayed redirect from another app is rejected.
     */
    fun handleAuthRedirect(uri: android.net.Uri?): Boolean {
        if (uri?.host != "auth-callback") return false
        val err = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error")
        if (err != null) { authError = err.take(120); clearPkce(); return true }
        val verifier = prefs.getString("pkce_verifier", null)
        val savedState = prefs.getString("pkce_state", null)
        val issuedAt = prefs.getLong("pkce_ts", 0L)
        clearPkce()
        val state = uri.getQueryParameter("state")
        if (verifier == null || savedState == null || state != savedState || System.currentTimeMillis() - issuedAt > PKCE_TTL_MS) {
            authError = "Session de connexion expirée — réessaie."
            return true
        }
        val code = uri.getQueryParameter("code") ?: return true
        busy = true
        authError = null
        scope.launch {
            try {
                val body = buildJsonObject { put("auth_code", code); put("code_verifier", verifier) }.toString()
                val (httpCode, resp) = withContext(Dispatchers.IO) { http("POST", "/auth/v1/token?grant_type=pkce", body, null) }
                if (httpCode !in 200..299) {
                    authError = "Connexion Google échouée (${httpCode})"
                    return@launch
                }
                applySessionResponse(resp)
            } catch (e: Exception) {
                authError = "Connexion impossible — vérifie Internet."
            } finally {
                busy = false
            }
        }
        return true
    }

    private fun clearPkce() {
        prefs.edit().remove("pkce_verifier").remove("pkce_state").remove("pkce_ts").apply()
    }

    private suspend fun applySessionResponse(resp: String) {
        val obj = json.parseToJsonElement(resp).jsonObject
        val token = obj["access_token"]?.jsonPrimitive?.content ?: run { authError = "Session Google invalide."; return }
        val email = obj["user"]?.jsonObject?.get("email")?.jsonPrimitive?.content ?: ""
        applySession(
            email = email,
            userId = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: "",
            token = token,
            refresh = obj["refresh_token"]?.jsonPrimitive?.content ?: "",
            expiresIn = obj["expires_in"]?.jsonPrimitive?.long ?: 3600L,
        )
    }

    private fun generatePkceVerifier(): String {
        val bytes = ByteArray(48)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    private fun pkceChallenge(verifier: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return android.util.Base64.encodeToString(digest, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    // ================= avatar storage =================

    /** Uploads the local avatar JPEG; returns the public URL on success. */
    fun uploadAvatar(bytes: ByteArray): String? {
        val s = session ?: return null
        val obj = "/object/avatars/${s.userId}.jpg"
        val (code, _) = http("POST", "/storage/v1$obj", null, s.access, bytes, extraHeaders = mapOf("Content-Type" to "image/jpeg", "x-upsert" to "true"))
        if (code !in 200..299) return null
        return "$BASE/storage/v1/object/public/avatars/${s.userId}.jpg"
    }

    fun downloadAvatar(url: String): ByteArray? {
        val conn = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull() ?: return null
        return try {
            conn.connectTimeout = 12000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Liftbook/1.35")
            if (conn.responseCode !in 200..299) null else conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) { null } finally { conn.disconnect() }
    }

    private suspend fun refreshIfNeeded() {
        val s = session ?: return
        if (s.expiresAt - System.currentTimeMillis() > 60_000L) return
        try {
            val body = buildJsonObject { put("refresh_token", s.refresh) }.toString()
            val (code, resp) = withContext(Dispatchers.IO) { http("POST", "/auth/v1/token?grant_type=refresh_token", body, null) }
            if (code !in 200..299) { signOut(); return }
            val obj = json.parseToJsonElement(resp).jsonObject
            s.access = obj["access_token"]?.jsonPrimitive?.content ?: s.access
            s.refresh = obj["refresh_token"]?.jsonPrimitive?.content ?: s.refresh
            s.expiresAt = System.currentTimeMillis() + (obj["expires_in"]?.jsonPrimitive?.long ?: 3600L) * 1000L
            persistSession()
        } catch (e: Exception) { /* keep session, retry later */ }
    }

    private fun mapAuthError(code: Int, body: String): String {
        val b = body.lowercase()
        return when {
            code == 429 -> "Trop d'essais — réessaie dans un instant."
            b.contains("already") || b.contains("registered") -> "Cet email a déjà un compte. Connecte-toi."
            b.contains("invalid_grant") || b.contains("invalid login") -> "Email ou mot de passe incorrect."
            b.contains("at least") && b.contains("character") -> "Mot de passe trop court (6 caractères minimum)."
            b.contains("validation") && b.contains("email") -> "Adresse email invalide."
            code >= 500 -> "Serveur indisponible — réessaie plus tard."
            else -> "Erreur inattendue ($code)."
        }
    }

    // ================= sync =================

    /** Called by Repo on any persistent data mutation. Debounced push. */
    fun markDirty() {
        dirtyAt = System.currentTimeMillis()
        persistMeta()
        requestSync()
    }

    fun currentTombW(): List<Long> = delW.toList()
    fun currentTombR(): List<String> = delR.toList()

    fun tombstoneWorkout(startedAt: Long) {
        if (this::prefs.isInitialized) {
            if (!delW.contains(startedAt)) delW.add(startedAt)
            if (delW.size > 400) delW.removeAt(0)
            persistMeta()
        }
    }

    fun tombstoneRoutine(name: String) {
        if (this::prefs.isInitialized) {
            if (!delR.contains(name)) delR.add(name)
            if (delR.size > 400) delR.removeAt(0)
            persistMeta()
        }
    }

    private fun clearTombstones() { delW.clear(); delR.clear(); persistMeta() }
    private fun adoptTombstones(p: SyncPayload) {
        delW = p.delW.toMutableList(); delR = p.delR.toMutableList(); persistMeta()
    }
    private fun mergeTombstones(p: SyncPayload) {
        p.delW.forEach { if (!delW.contains(it)) delW.add(it) }
        p.delR.forEach { if (!delR.contains(it)) delR.add(it) }
        persistMeta()
    }

    fun requestSync(debounceMs: Long = 3000) {
        if (session == null) return
        debounce?.cancel()
        debounce = scope.launch {
            delay(debounceMs)
            syncNow()
        }
    }

    fun syncNow() {
        if (session == null || syncing) return
        syncing = true
        scope.launch {
            try {
                refreshIfNeeded()
                val (remote, remoteTs) = withContext(Dispatchers.IO) { pull() }
                val dirty = dirtyAt > pushedTs
                if (!dirty) {
                    if (remote != null && remoteTs > lastSeenRemoteTs) {
                        val oldAvatar = Repo.settings.avatarUrl
                        withContext(Dispatchers.Main) { Repo.replaceAll(remote) }
                        adoptTombstones(remote)
                        lastSeenRemoteTs = remoteTs
                        pushedTs = remoteTs
                        dirtyAt = remoteTs
                        persistMeta()
                        maybeFetchAvatar(oldAvatar)
                        syncStatus = stamp("Synchronisé")
                    }
                } else {
                    if (remote != null && remoteTs > lastSeenRemoteTs) {
                        // Remote moved since we last saw it (other device) → merge before pushing.
                        withContext(Dispatchers.Main) { Repo.mergeRemote(remote) }
                        mergeTombstones(remote)
                        lastSeenRemoteTs = remoteTs
                    }
                    val ts = maxOf(System.currentTimeMillis(), lastSeenRemoteTs + 1)
                    val payload = withContext(Dispatchers.Main) { Repo.snapshot() }
                    val stored = withContext(Dispatchers.IO) { push(payload, ts) }
                    pushedTs = ts
                    dirtyAt = ts
                    lastSeenRemoteTs = stored
                    persistMeta()
                    syncStatus = stamp("Synchronisé")
                }
            } catch (e: Exception) {
                syncStatus = stamp("Sync échouée (${e.message?.take(60) ?: "réseau"})")
            } finally {
                syncing = false
                syncTick++
            }
        }
    }

    private fun stamp(s: String): String =
        s + " · " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    /** Another device changed the profile photo → download it for local display. */
    private fun maybeFetchAvatar(previousUrl: String) {
        val url = Repo.settings.avatarUrl
        val ctx = appCtx ?: return
        if (url.isBlank() || url == previousUrl) return
        scope.launch(Dispatchers.IO) {
            val bytes = downloadAvatar(url)
            if (bytes != null) {
                AvatarCache.file(ctx).writeBytes(bytes)
                AvatarCache.reload(ctx)
                withContext(Dispatchers.Main) { Repo.touchPublic() }
            }
        }
    }

    /** GET own snapshot row; returns (payload?, client_ts). */
    private fun pull(): Pair<SyncPayload?, Long> {
        val s = session ?: throw IllegalStateException("no session")
        val (code, resp) = http("GET", "/rest/v1/hevy_snapshots?select=data,client_ts", null, s.access)
        if (code == 401) throw IllegalStateException("unauthorized")
        if (code !in 200..299) throw IllegalStateException("pull $code")
        val arr = json.parseToJsonElement(resp).jsonArray
        if (arr.isEmpty()) return null to 0L
        val row = arr[0].jsonObject
        val ts = row["client_ts"]?.jsonPrimitive?.long ?: 0L
        val data = row["data"] as? JsonObject ?: return null to ts
        val payload = runCatching { json.decodeFromJsonElement(SyncPayload.serializer(), data) }.getOrNull()
        return payload to ts
    }

    /** Push full snapshot with LWW guard; returns the ts now stored server-side. */
    private fun push(payload: SyncPayload, ts: Long): Long {
        val s = session ?: throw IllegalStateException("no session")
        val body = buildJsonObject {
            put("p_data", json.parseToJsonElement(json.encodeToString(SyncPayload.serializer(), payload)))
            put("p_client_ts", ts)
        }.toString()
        val (code, resp) = http("POST", "/rest/v1/rpc/hevy_push_snapshot", body, s.access)
        if (code == 401) throw IllegalStateException("unauthorized")
        if (code !in 200..299) throw IllegalStateException("push $code")
        return runCatching { json.parseToJsonElement(resp).jsonPrimitive.long }.getOrDefault(ts)
    }

    // ================= persistence =================

    private fun persistSession() {
        val s = session ?: return
        val blob = buildJsonObject {
            put("email", s.email)
            put("userId", s.userId)
            put("access", s.access)
            put("refresh", s.refresh)
            put("expiresAt", s.expiresAt)
        }.toString()
        val boxed = SecretBox.encrypt(blob)
        if (boxed == null) {
            // KeyStore failure: fail secure — session stays in memory only, nothing hits disk in clear.
            prefs.edit().remove("session_box").apply()
            return
        }
        prefs.edit()
            .putString("session_box", boxed)
            // legacy plaintext keys (≤ v1.34) are scrubbed once the box exists
            .remove("access").remove("refresh").remove("expiresAt").remove("userId").remove("email")
            .apply()
    }

    private fun persistMeta() {
        if (!this::prefs.isInitialized) return
        prefs.edit()
            .putLong("dirtyAt", dirtyAt)
            .putLong("pushedTs", pushedTs)
            .putLong("lastSeenRemoteTs", lastSeenRemoteTs)
            .putString("lastAccount", lastAccount)
            .putBoolean("skipped", skipped)
            .putString("delW", json.encodeToString(delW.toList()))
            .putString("delR", json.encodeToString(delR.toList()))
            .apply()
    }

    fun persistSkip() { persistMeta() }

    // ================= http =================

    private fun http(method: String, path: String, body: String?, bearer: String?, raw: ByteArray? = null, extraHeaders: Map<String, String> = emptyMap()): Pair<Int, String> {
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 25000
            setRequestProperty("apikey", ANON)
            if (!extraHeaders.containsKey("Content-Type")) setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Liftbook/1.35")
            bearer?.let { setRequestProperty("Authorization", "Bearer $it") }
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null || raw != null) doOutput = true
        }
        try {
            if (raw != null) conn.outputStream.use { it.write(raw) }
            else if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to resp
        } finally {
            conn.disconnect()
        }
    }
}

/** Locally cached profile photo (filesDir/avatar.jpg), decoded once per change. */
object AvatarCache {
    @Volatile var bmp: android.graphics.Bitmap? = null
        private set
    fun file(ctx: Context) = java.io.File(ctx.filesDir, "avatar.jpg")
    fun reload(ctx: Context) {
        bmp = runCatching { android.graphics.BitmapFactory.decodeFile(file(ctx).absolutePath) }.getOrNull()
    }
}
