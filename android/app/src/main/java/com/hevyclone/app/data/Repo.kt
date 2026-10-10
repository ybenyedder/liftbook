package com.hevyclone.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json

/** SQLite persistence. Workouts/routines/photos are stored as JSON blobs (single source of truth = in-memory lists). */
class Db(ctx: Context) : SQLiteOpenHelper(ctx, "hevy.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workouts(id INTEGER PRIMARY KEY AUTOINCREMENT, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, name TEXT NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE routines(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, pos INTEGER NOT NULL DEFAULT 0, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.execSQL("CREATE TABLE photos(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, json TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE routines ADD COLUMN pos INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE routines SET pos = id")
        }
        if (oldV < 3) db.execSQL("CREATE TABLE photos(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, json TEXT NOT NULL)")
    }
}

/** In-memory repository with write-through persistence; `rev` bumps trigger Compose recomposition. */
object Repo {
    private lateinit var db: Db
    private val json = Json { ignoreUnknownKeys = true }

    val workouts = mutableListOf<Workout>()     // sorted by startedAt ASC
    val routines = mutableListOf<Routine>()
    val photos = mutableListOf<ProgressPhoto>() // sorted by ts ASC
    val customs = mutableListOf<CustomExDef>()  // user-created exercises
    var settings = Settings()
    var draft: Draft? = null
    var prCache: Map<String, PrBest> = emptyMap()
    var rev by mutableStateOf(0)

    // ---- perf caches (invalidated on touch) ----
    private var descCache: List<Workout>? = null
    private var prevCache = HashMap<String, List<String>>()

    private fun touch() {
        descCache = null
        prevCache.clear()
        persistDraft()
        rev++
    }

    private var draftPrefs: android.content.SharedPreferences? = null

    fun persistDraftNow() { persistDraft() }

    private fun persistDraft() {
        val prefs = draftPrefs ?: return
        prefs.edit().apply {
            if (draft != null) putString("draft", json.encodeToString(Draft.serializer(), draft!!))
            else remove("draft")
        }.apply()
    }
    fun touchPublic() { touch() }

    /** Names of the stock catalog (captured before any custom is registered). */
    private var builtinNames: Set<String> = emptySet()

    fun init(ctx: Context) {
        if (this::db.isInitialized) return
        appCtx = ctx.applicationContext
        builtinNames = EX.keys.toSet()
        draftPrefs = ctx.getSharedPreferences("draft", Context.MODE_PRIVATE)
        draft = draftPrefs?.getString("draft", null)?.let {
            runCatching { json.decodeFromString<Draft>(it) }.getOrNull()
        }
        db = Db(ctx)
        val sRow = db.readableDatabase.rawQuery("SELECT v FROM settings WHERE k='settings'", null).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
        settings = sRow?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings(since = System.currentTimeMillis())
        db.readableDatabase.rawQuery("SELECT v FROM settings WHERE k='customs'", null).use { c ->
            if (c.moveToFirst()) runCatching { json.decodeFromString<List<CustomExDef>>(c.getString(0)) }.getOrNull()?.let { customs.addAll(it) }
        }
        for (cx in customs) registerCustomCatalog(cx)
        db.readableDatabase.rawQuery("SELECT json FROM workouts ORDER BY startedAt ASC", null).use { c ->
            while (c.moveToNext()) runCatching { json.decodeFromString<Workout>(c.getString(0)) }.getOrNull()?.let { workouts.add(it) }
        }
        db.readableDatabase.rawQuery("SELECT json FROM routines ORDER BY pos ASC, id ASC", null).use { c ->
            while (c.moveToNext()) runCatching { json.decodeFromString<Routine>(c.getString(0)) }.getOrNull()?.let { routines.add(it) }
        }
        db.readableDatabase.rawQuery("SELECT json FROM photos ORDER BY ts ASC", null).use { c ->
            while (c.moveToNext()) runCatching { json.decodeFromString<ProgressPhoto>(c.getString(0)) }.getOrNull()?.let { photos.add(it) }
        }
        prCache = Calc.rebuildPrs(workouts)
        persistSettings()
        touch()
    }

    private fun persistWorkout(w: Workout) {
        val cv = ContentValues().apply {
            put("id", w.id); put("startedAt", w.startedAt); put("endedAt", w.endedAt)
            put("name", w.name); put("json", json.encodeToString(Workout.serializer(), w))
        }
        db.writableDatabase.insertWithOnConflict("workouts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun persistRoutine(r: Routine) {
        val cv = ContentValues().apply {
            put("id", r.id); put("name", r.name); put("pos", r.pos)
            put("json", json.encodeToString(Routine.serializer(), r))
        }
        db.writableDatabase.insertWithOnConflict("routines", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun persistPhoto(p: ProgressPhoto) {
        val cv = ContentValues().apply {
            put("id", p.id); put("ts", p.ts)
            put("json", json.encodeToString(ProgressPhoto.serializer(), p))
        }
        db.writableDatabase.insertWithOnConflict("photos", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun persistSettings() {
        val cv = ContentValues().apply { put("k", "settings"); put("v", json.encodeToString(Settings.serializer(), settings)) }
        db.writableDatabase.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun nextWorkoutId(): Long = (workouts.maxOfOrNull { it.id } ?: 0L) + 1
    fun nextRoutineId(): Long = (routines.maxOfOrNull { it.id } ?: 0L) + 1
    fun nextPos(): Int = (routines.maxOfOrNull { it.pos } ?: 0) + 1

    fun workoutById(id: Long): Workout? = workouts.firstOrNull { it.id == id }
    fun routineById(id: Long): Routine? = routines.firstOrNull { it.id == id }
    fun workoutsDesc(): List<Workout> {
        descCache?.let { return it }
        val sorted = workouts.sortedByDescending { it.startedAt }
        descCache = sorted
        return sorted
    }

    // ---------- draft lifecycle ----------

    fun startWorkout(routineId: Long?) {
        val r = routineId?.let { routineById(it) }
        val exs = r?.exercises?.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, done = false) }.toMutableList())
        }?.toMutableList() ?: mutableListOf()
        draft = Draft("workout", routineId, r?.name ?: "Séance", System.currentTimeMillis(), "", exs)
        touch()
    }

    fun startRoutine(routineId: Long?) {
        val r = routineId?.let { routineById(it) }
        val exs = r?.exercises?.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.mins, it.km, it.done) }.toMutableList())
        }?.toMutableList() ?: mutableListOf()
        draft = Draft("routine", routineId, r?.name ?: "Nouvelle Routine", null, "", exs)
        touch()
    }

    /** Start a new workout with the exercises of a past workout (null = most recent). */
    fun startRepeat(workoutId: Long? = null) {
        val last = workoutId?.let { workoutById(it) } ?: workouts.maxByOrNull { it.startedAt } ?: return
        draft = Draft(
            "workout", null, last.name, System.currentTimeMillis(), last.notes,
            last.exercises.map { ex ->
                ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, done = false) }.toMutableList())
            }.toMutableList(),
        )
        touch()
    }

    fun addExToDraft(name: String) {
        val d = draft ?: return
        d.exercises.add(ExEntry(name, EX[name]?.muscle ?: "", "", false, null, mutableListOf(SetEntry(null, null, done = false))))
        touch()
    }

    /** Routine identity is its name in the sync merge — never allow duplicates. */
    fun uniqueRoutineName(base: String): String {
        val b = base.trim().ifEmpty { "Routine" }
        if (routines.none { it.name == b }) return b
        var i = 2
        while (routines.any { it.name == "$b ($i)" }) i++
        return "$b ($i)"
    }

    fun saveRoutine(name: String): Routine? {
        val d = draft ?: return null
        if (d.exercises.isEmpty()) return null
        val r: Routine = if (d.routineId != null) {
            val existing = routineById(d.routineId!!) ?: return null
            existing.name = uniqueRoutineName(name)
            existing.exercises = d.exercises
            existing
        } else {
            Routine(nextRoutineId(), uniqueRoutineName(name), d.exercises, nextPos()).also { routines.add(it) }
        }
        persistRoutine(r)
        draft = null
        touch()
        Cloud.markDirty()
        return r
    }

    fun finishWorkout(name: String): Workout? {
        val d = draft ?: return null
        val now = System.currentTimeMillis()
        // records are evaluated against history strictly before this session
        val prs = mutableListOf<PrRec>()
        for (ex in d.exercises) {
            val done = ex.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 && it.done }
            if (done.isEmpty()) continue
            val prev = prFor(ex.name)
            val pw = prev?.weight ?: 0.0
            val pe = prev?.e1rm ?: 0.0
            val bw = done.maxOf { it.kg!! }
            val be = done.maxOf { Calc.e1rm(it.kg!!, it.reps!!) }
            if (bw > pw) prs.add(PrRec(ex.name, "Weight", bw))
            if (be > pe) prs.add(PrRec(ex.name, "Est. 1RM", be))
        }
        val w = Workout(
            id = nextWorkoutId(),
            name = name.trim().ifEmpty { "Séance" },
            startedAt = d.startedAt ?: now - 3600000,
            endedAt = now,
            exercises = d.exercises,
            notes = d.notes,
            prs = prs.toMutableList(),
        )
        workouts.add(w)
        prCache = Calc.rebuildPrs(workouts)
        persistWorkout(w)
        draft = null
        touch()
        Cloud.markDirty()
        return w
    }

    fun discardDraft() { draft = null; touch() }

    /**
     * True when a workout started from a routine changed structurally vs that routine:
     * exercises added/removed/replaced/reordered, sets added/removed, rest or notes edited.
     * Weights/reps are ignored on purpose — they change every session.
     */
    fun draftDiffersFromRoutine(): Boolean {
        val d = draft ?: return false
        val r = d.routineId?.let { routineById(it) } ?: return false
        if (d.exercises.size != r.exercises.size) return true
        d.exercises.forEachIndexed { i, de ->
            val re = r.exercises[i]
            if (de.name != re.name || de.notes != re.notes || de.superset != re.superset ||
                de.restSec != re.restSec || de.sets.size != re.sets.size) return true
        }
        return false
    }

    /** Copy the workout's structure back into the routine it was started from (sets kept as template, done=false). */
    fun updateRoutineFromDraft() {
        val d = draft ?: return
        val r = d.routineId?.let { routineById(it) } ?: return
        r.exercises = d.exercises.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec,
                ex.sets.map { SetEntry(it.kg, it.reps, done = false) }.toMutableList())
        }.toMutableList()
        persistRoutine(r)
        touch()
        Cloud.markDirty()
    }

    fun restoreWorkout(w: Workout) {
        // Undo of a delete: the tombstone must go too, or the next sync merge would
        // silently delete the restored workout again (it rides in delW).
        Cloud.untombstoneWorkout(w.startedAt)
        workouts.removeAll { it.id == w.id }
        workouts.add(w)
        workouts.sortBy { it.startedAt }
        persistWorkout(w)
        prCache = Calc.rebuildPrs(workouts)
        touch()
        Cloud.markDirty()
    }

    fun deleteWorkout(id: Long) {
        workoutById(id)?.let { Cloud.tombstoneWorkout(it.startedAt) }
        workouts.removeAll { it.id == id }
        db.writableDatabase.delete("workouts", "id=?", arrayOf(id.toString()))
        prCache = Calc.rebuildPrs(workouts)
        touch()
        Cloud.markDirty()
    }

    fun routineFromWorkout(workoutId: Long, name: String): Routine? {
        val w = workoutById(workoutId) ?: return null
        val r = Routine(nextRoutineId(), uniqueRoutineName(name.ifEmpty { w.name }), w.exercises.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.mins, it.km, it.done) }.toMutableList())
        }.toMutableList(), nextPos())
        routines.add(r)
        persistRoutine(r)
        touch()
        Cloud.markDirty()
        return r
    }

    fun renameRoutine(id: Long, name: String) {
        val r = routineById(id) ?: return
        val n = name.trim()
        if (n.isEmpty() || n == r.name) return
        r.name = uniqueRoutineName(n)
        persistRoutine(r)
        touch()
        Cloud.markDirty()
    }

    fun duplicateRoutine(id: Long): Routine? {
        val r = routineById(id) ?: return null
        val copy = Routine(nextRoutineId(), uniqueRoutineName(r.name), r.exercises.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.mins, it.km, it.done) }.toMutableList())
        }.toMutableList(), nextPos())
        routines.add(copy)
        persistRoutine(copy)
        touch()
        Cloud.markDirty()
        return copy
    }

    fun deleteRoutine(id: Long) {
        routineById(id)?.let { Cloud.tombstoneRoutine(it.name) }
        routines.removeAll { it.id == id }
        db.writableDatabase.delete("routines", "id=?", arrayOf(id.toString()))
        touch()
        Cloud.markDirty()
    }

    /** Drag & drop reorder (indices in routines list == manual pos order). */
    fun moveRoutine(from: Int, to: Int) {
        if (from == to || from !in routines.indices || to !in routines.indices) return
        val item = routines.removeAt(from)
        routines.add(to, item)
        for (i in routines.indices) routines[i].pos = i
        for (r in routines) persistRoutine(r)
        touch()
        Cloud.markDirty()
    }

    // ---------- custom exercises ----------

    /** Inserts a custom exercise into the global catalog (EX/EXERCISES) so every lookup works. */
    private fun registerCustomCatalog(cx: CustomExDef) {
        if (EX.containsKey(cx.name)) return
        val def = ExerciseDef(cx.name, cx.muscle, cx.equip)
        EXERCISES.add(def)
        EX[cx.name] = def
    }

    /** Case/accent-insensitive identity for exercise names (creation collision check). */
    private fun normName(s: String): String = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]"), "")

    /** True when a custom name would shadow a catalog exercise (EN key OR localized display name). */
    fun customNameTaken(raw: String): Boolean {
        val n = normName(raw)
        if (n.isEmpty()) return false
        return EXERCISES.any { normName(it.name) == n || normName(L10nData.name(it.name)) == n }
    }

    fun addCustom(nameRaw: String, muscle: String, equip: String): CustomExDef? {
        val name = nameRaw.trim()
        if (name.isEmpty() || customNameTaken(name)) return null
        val cx = CustomExDef(name, muscle, equip)
        customs.add(cx)
        registerCustomCatalog(cx)
        db.writableDatabase.insertWithOnConflict(
            "settings", null,
            ContentValues().apply {
                put("k", "customs")
                put("v", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CustomExDef.serializer()), customs))
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        touch()
        Cloud.markDirty()
        return cx
    }

    /** Remove a user-created exercise (catalog + persisted list). History/routines keep the
     *  name — they still display — but the exercise leaves search, picker and stats catalog.
     *  The deletion propagates to other devices via the delC tombstone. */
    fun deleteCustom(name: String) {
        if (customs.none { it.name == name }) return
        Cloud.tombstoneCustom(name)
        customs.removeAll { it.name == name }
        if (name !in builtinNames) {
            EXERCISES.removeAll { it.name == name }
            EX.remove(name)
        }
        persistCustoms()
        touch()
        Cloud.markDirty()
    }

    // ---------- progress photos ----------

    fun nextPhotoId(): Long = (photos.maxOfOrNull { it.id } ?: 0L) + 1

    /** Local pixel store: filesDir/progress/<id>.jpg */
    fun photoFile(id: Long): java.io.File? {
        val ctx = appCtx ?: return null
        return java.io.File(java.io.File(ctx.filesDir, "progress"), "$id.jpg")
    }

    private var appCtx: Context? = null

    /**
     * Import a picked image as a new progress photo: downscale to ≤1440px long side,
     * keep aspect ratio (no crop), JPEG 86. Returns the photo or null on decode failure.
     */
    fun addPhotoFromUri(ctx: Context, uri: android.net.Uri, ts: Long = System.currentTimeMillis(), wId: Long? = null): ProgressPhoto? {
        val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return null
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1440) sample *= 2
        val src = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val id = nextPhotoId()
        val dir = java.io.File(ctx.filesDir, "progress").apply { mkdirs() }
        val f = java.io.File(dir, "$id.jpg")
        val ok = runCatching { f.outputStream().use { src.compress(android.graphics.Bitmap.CompressFormat.JPEG, 86, it) } }.getOrDefault(false)
        if (!ok) return null
        val p = ProgressPhoto(id, ts, wId)
        photos.add(p)
        photos.sortBy { it.ts }
        persistPhoto(p)
        touch()
        Cloud.markDirty()
        return p
    }

    /** Adopt raw bytes (cloud download) as the local file of an already-known photo. */
    fun savePhotoBytes(id: Long, bytes: ByteArray) {
        val f = photoFile(id) ?: return
        f.parentFile?.mkdirs()
        runCatching { f.writeBytes(bytes) }
    }

    fun updatePhoto(id: Long, note: String? = null, kg: Double? = null, kgSet: Boolean = false) {
        val p = photos.firstOrNull { it.id == id } ?: return
        if (note != null) p.note = note.trim()
        if (kgSet) p.kg = kg
        persistPhoto(p)
        touch()
        Cloud.markDirty()
    }

    fun linkPhotoToWorkout(photoId: Long, wId: Long) {
        val p = photos.firstOrNull { it.id == photoId } ?: return
        p.wId = wId
        persistPhoto(p)
        touch()
        Cloud.markDirty()
    }

    /** Remote path confirmed by the uploader → recorded so other devices can fetch it. */
    fun setPhotoRemote(id: Long, remote: String) {
        val p = photos.firstOrNull { it.id == id } ?: return
        if (p.remote == remote) return
        p.remote = remote
        persistPhoto(p)
        Cloud.markDirty()
    }

    fun deletePhoto(id: Long) {
        val p = photos.firstOrNull { it.id == id } ?: return
        Cloud.tombstonePhoto(id)
        if (p.remote.isNotEmpty()) Cloud.deletePhotoObject(p.remote)
        photos.removeAll { it.id == id }
        db.writableDatabase.delete("photos", "id=?", arrayOf(id.toString()))
        photoFile(id)?.delete()
        touch()
        Cloud.markDirty()
    }

    fun photosDesc(): List<ProgressPhoto> = photos.sortedByDescending { it.ts }

    fun photoById(id: Long): ProgressPhoto? = photos.firstOrNull { it.id == id }

    fun photoForWorkout(wId: Long): ProgressPhoto? = photos.lastOrNull { it.wId == wId }

    // ---------- settings ----------

    fun setUnit(u: String) { settings.unit = u; persistSettings(); touch(); Cloud.markDirty() }    fun setRest(sec: Int) { settings.restSec = sec; persistSettings(); touch(); Cloud.markDirty() }
    fun setAccent(a: String) { settings.accent = a; persistSettings(); touch(); Cloud.markDirty() }
    fun setTheme(t: String) { settings.theme = t; persistSettings(); touch(); Cloud.markDirty() }
    fun setProfile(name: String, handle: String) {
        settings.profileName = name; settings.handle = handle
        persistSettings(); touch(); Cloud.markDirty()
    }

    fun setAvatar(url: String) {
        settings.avatarUrl = url
        persistSettings(); touch(); Cloud.markDirty()
    }

    fun backupJson(): String = json.encodeToString(
        BackupData.serializer(), BackupData(workouts.toList(), routines.toList(), photos.toList())
    )

    fun restoreBackup(content: String): Boolean {
        val data = runCatching { json.decodeFromString<BackupData>(content) }.getOrNull() ?: return false
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        db.writableDatabase.delete("photos", null, null)
        workouts.clear(); routines.clear(); photos.clear()
        data.workouts.forEach { workouts.add(it); persistWorkout(it) }
        data.routines.forEachIndexed { i, r -> r.pos = i; routines.add(r); persistRoutine(r) }
        data.photos.forEach { photos.add(it); persistPhoto(it) }
        photos.sortBy { it.ts }
        prCache = Calc.rebuildPrs(workouts)
        touch()
        Cloud.markDirty()
        return true
    }

    fun importCsv(content: String): Int {
        val imported = Calc.parseCsv(content)
        val knownDates = workouts.map { it.startedAt }.toSet()
        var added = 0
        for (w in imported) {
            if (w.startedAt in knownDates) continue // re-import of the same export: skip twins
            val fixed = w.copy(id = nextWorkoutId()) // re-id: parser ids are fixed offsets and would collide in the DB/LazyColumn keys
            workouts.add(fixed)
            persistWorkout(fixed)
            added++
        }
        if (added > 0) {
            workouts.sortBy { it.startedAt }
            prCache = Calc.rebuildPrs(workouts)
            touch()
            Cloud.markDirty()
        }
        return added
    }

    /** Merge a Hevy/Strong export: workouts are added once per start date, routines appended.
     * Hevy never exports templates, so routines are also rebuilt from distinct workout names. */
    fun importHevy(newWorkouts: List<Workout>, newRoutines: List<Routine>): Int {
        val knownDates = workouts.map { it.startedAt }.toSet()
        var added = 0
        for (w in newWorkouts) {
            if (w.startedAt in knownDates) continue
            val fixed = w.copy(id = nextWorkoutId())
            workouts.add(fixed)
            persistWorkout(fixed)
            added++
        }
        val allRoutines = newRoutines +
            Calc.routinesFromWorkouts(newWorkouts, routines.map { it.name }.toSet())
        for (r in allRoutines) {
            val fixed = r.copy(id = nextRoutineId(), pos = nextPos())
            routines.add(fixed)
            persistRoutine(fixed)
        }
        if (added > 0 || allRoutines.isNotEmpty()) {
            workouts.sortBy { it.startedAt }
            prCache = Calc.rebuildPrs(workouts)
            touch()
            Cloud.markDirty()
        }
        return added
    }

    fun wipe(markDirty: Boolean = true) {
        // Queue remote photo removal + tombstones so "Erase all" propagates to other
        // devices (only for the signed-in erase; account-switch wipe keeps the cloud copy).
        if (markDirty) {
            for (p in photos) {
                Cloud.tombstonePhoto(p.id)
                if (p.remote.isNotEmpty()) Cloud.deletePhotoObject(p.remote)
            }
            customs.forEach { Cloud.tombstoneCustom(it.name) }
        }
        // Drop customs from the persisted settings row AND the global catalog — otherwise
        // they resurrect on next launch ("Tout effacer" ghost exercises). Built-in catalog
        // names are never removed (a legacy custom could share a built-in name).
        val gone = customs.map { it.name }.toSet().filter { it !in builtinNames }
        db.writableDatabase.delete("settings", "k=?", arrayOf("customs"))
        EXERCISES.removeAll { gone.contains(it.name) }
        gone.forEach { EX.remove(it) }
        workouts.clear(); routines.clear(); photos.clear(); customs.clear(); draft = null
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        db.writableDatabase.delete("photos", null, null)
        appCtx?.let { java.io.File(it.filesDir, "progress").deleteRecursively() }
        prCache = emptyMap()
        touch()
        if (markDirty) Cloud.markDirty()
    }

    // ---------- cloud sync ----------

    /** Full snapshot of everything that syncs. */
    fun snapshot(): SyncPayload = SyncPayload(
        workouts = workouts.toList(),
        routines = routines.toList(),
        settings = settings,
        delW = Cloud.currentTombW(),
        delR = Cloud.currentTombR(),
        photos = photos.toList(),
        delP = Cloud.currentTombP(),
        customs = customs.toList(),
        delC = Cloud.currentTombC(),
    )

    /** Replace local state wholesale with a remote snapshot (clean pull). No dirty marking. */
    fun replaceAll(p: SyncPayload) {
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        db.writableDatabase.delete("photos", null, null)
        workouts.clear(); routines.clear(); photos.clear()
        p.workouts.forEach { workouts.add(it); persistWorkout(it) }
        p.routines.forEachIndexed { i, r -> r.pos = i; routines.add(r); persistRoutine(r) }
        p.photos.forEach { photos.add(it); persistPhoto(it) }
        photos.sortBy { it.ts }
        applyCustoms(p.customs)
        p.settings?.let {
            settings = it
            persistSettings()
        }
        prCache = Calc.rebuildPrs(workouts)
        touch()
    }

    /** Union-merge a remote snapshot into local (conflict / first login). Local deletions win;
     *  remote deletions are applied; duplicates skipped by startedAt (workouts), name (routines),
     *  id (photos), name (customs). Returns true when anything changed. */
    fun mergeRemote(p: SyncPayload): Boolean {
        val localDelW = Cloud.currentTombW().toSet()
        val localDelR = Cloud.currentTombR().toSet()
        val localDelP = Cloud.currentTombP().toSet()
        val localDelC = Cloud.currentTombC().toSet()
        val remoteDelW = p.delW.toSet()
        val remoteDelR = p.delR.toSet()
        val remoteDelP = p.delP.toSet()
        val remoteDelC = p.delC.toSet()

        val keepW = workouts.filter { it.startedAt !in remoteDelW }
        val keepR = routines.filter { it.name !in remoteDelR }
        val keepP = photos.filter { it.id !in remoteDelP }
        val keepC = customs.filter { it.name !in remoteDelC }
        val knownW = keepW.map { it.startedAt }.toSet()
        val knownR = keepR.map { it.name }.toSet()
        val knownP = keepP.map { it.id }.toSet()
        val addW = p.workouts.filter { it.startedAt !in localDelW && it.startedAt !in knownW }
        val addR = p.routines.filter { it.name !in localDelR && it.name !in knownR }
        val addP = p.photos.filter { it.id !in localDelP && it.id !in knownP }
        val knownC = keepC.map { it.name }.toSet()
        val addC = p.customs.filter { it.name !in localDelC && it.name !in knownC }

        if (keepW.size == workouts.size && keepR.size == routines.size && keepP.size == photos.size && keepC.size == customs.size &&
            addW.isEmpty() && addR.isEmpty() && addP.isEmpty() && addC.isEmpty()
        ) return false

        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        db.writableDatabase.delete("photos", null, null)
        workouts.clear(); routines.clear(); photos.clear()
        keepW.forEach { workouts.add(it); persistWorkout(it) }
        addW.forEach { val f = it.copy(id = nextWorkoutId()); workouts.add(f); persistWorkout(f) }
        workouts.sortBy { it.startedAt }
        keepR.forEach { routines.add(it); persistRoutine(it) }
        addR.forEach { val f = it.copy(id = nextRoutineId(), pos = nextPos()); routines.add(f); persistRoutine(f) }
        keepP.forEach { photos.add(it); persistPhoto(it) }
        addP.forEach { photos.add(it); persistPhoto(it) }
        photos.sortBy { it.ts }
        applyCustoms(keepC + addC)
        // customs dropped by a remote deletion must leave the global catalog too
        remoteDelC.forEach { gone ->
            if (gone !in builtinNames) {
                EXERCISES.removeAll { it.name == gone }
                EX.remove(gone)
            }
        }
        prCache = Calc.rebuildPrs(workouts)
        touch()
        return true
    }

    /** Replace/merge the custom custom-exercise list (from a remote snapshot) and re-register the catalog. */
    private fun applyCustoms(list: List<CustomExDef>) {
        customs.clear()
        customs.addAll(list.distinctBy { it.name })
        persistCustoms()
        for (cx in customs) registerCustomCatalog(cx)
    }

    private fun persistCustoms() {
        db.writableDatabase.insertWithOnConflict(
            "settings", null,
            ContentValues().apply {
                put("k", "customs")
                put("v", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CustomExDef.serializer()), customs))
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    // ---------- queries ----------

    fun prFor(name: String): PrBest? = prCache[name]

    fun e1rmSeries(name: String): List<Pair<String, Double>> = Calc.e1rmSeries(name, workouts)

    /** Previous performance of an exercise: per set index, "82.5 × 8" in display units. Cached. */
    fun prevFor(name: String): List<String>? {
        prevCache[name]?.let { return it.ifEmpty { null } }
        var result: List<String>? = null
        for (w in workoutsDesc()) {
            val ex = w.exercises.firstOrNull { e -> e.name == name && e.sets.any { it.kg != null || it.reps != null || it.mins != null || it.km != null } }
            if (ex != null) {
                val cardio = Calc.isCardioName(name)
                result = ex.sets.map { s ->
                    if (s.kg == null && s.reps == null && s.mins == null && s.km == null) "—"
                    else if (cardio) Calc.fmtCardioSet(s.mins, s.km)
                    else "${Calc.fmtKg(s.kg, settings.unit)}${Calc.unitLabel(settings.unit)} × ${s.reps ?: "—"}"
                }
                break
            }
        }
        prevCache[name] = result ?: emptyList()
        return result
    }

    fun routineLastPerformed(r: Routine): Long? =
        workoutsDesc().firstOrNull { w -> w.name == r.name && w.exercises.any { e -> r.exercises.any { it.name == e.name } } }?.startedAt
    
    /** Per-set "previous" strings for an exercise, computed strictly before the given timestamp. */
    fun prevSetsBefore(beforeMs: Long, name: String): List<String>? {
        for (w in workouts.sortedByDescending { it.startedAt }) {
            if (w.startedAt >= beforeMs) continue
            val ex = w.exercises.firstOrNull { e -> e.name == name && e.sets.any { it.kg != null || it.reps != null || it.mins != null || it.km != null } } ?: continue
            val cardio = Calc.isCardioName(name)
            return ex.sets.map { s ->
                if (s.kg == null && s.reps == null && s.mins == null && s.km == null) "—"
                else if (cardio) Calc.fmtCardioSet(s.mins, s.km)
                else "${Calc.fmtKg(s.kg, settings.unit)}${Calc.unitLabel(settings.unit)} × ${s.reps ?: "—"}"
            }
        }
        return null
    }
}
