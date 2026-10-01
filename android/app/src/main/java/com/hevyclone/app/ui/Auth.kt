package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Cloud
import com.hevyclone.app.data.GoogleSignInNative
import kotlinx.coroutines.launch

/** Minimal SVG path-data parser (M L H V C S Z, absolute/relative) — enough for brand marks. */
fun pathNodesOf(d: String): List<androidx.compose.ui.graphics.vector.PathNode> {
    val b = androidx.compose.ui.graphics.vector.PathBuilder()
    val tokens = Regex("[MmLlHhVvCcSsZz]|-?\\d*\\.?\\d+(?:e[-+]?\\d+)?").findAll(d).map { it.value }.toList()
    var i = 0
    var cx = 0f; var cy = 0f
    var lastCx = 0f; var lastCy = 0f; var startX = 0f; var startY = 0f
    fun num(): Float = tokens[i++].toFloat()
    while (i < tokens.size) {
        when (val c = tokens[i++]) {
            "M", "m" -> {
                val rel = c == "m"
                var x = num(); var y = num()
                if (rel) { x += cx; y += cy }
                b.moveTo(x, y)
                cx = x; cy = y; startX = x; startY = y
                // implicit following pairs are lineTo
                while (i < tokens.size && !Regex("[MmLlHhVvCcSsZz]").matches(tokens[i])) {
                    var lx = num(); var ly = num()
                    if (rel) { lx += cx; ly += cy }
                    b.lineTo(lx, ly)
                    cx = lx; cy = ly
                }
            }
            "L", "l" -> {
                val rel = c == "l"
                var x = num(); var y = num()
                if (rel) { x += cx; y += cy }
                b.lineTo(x, y); cx = x; cy = y
                while (i < tokens.size && !Regex("[MmLlHhVvCcSsZz]").matches(tokens[i])) {
                    var lx = num(); var ly = num()
                    if (rel) { lx += cx; ly += cy }
                    b.lineTo(lx, ly)
                    cx = lx; cy = ly
                }
            }
            "H", "h" -> {
                val rel = c == "h"
                while (i < tokens.size && !Regex("[MmLlHhVvCcSsZz]").matches(tokens[i])) {
                    var x = num(); if (rel) x += cx
                    b.lineTo(x, cy); cx = x
                }
            }
            "V", "v" -> {
                val rel = c == "v"
                while (i < tokens.size && !Regex("[MmLlHhVvCcSsZz]").matches(tokens[i])) {
                    var y = num(); if (rel) y += cy
                    b.lineTo(cx, y); cy = y
                }
            }
            "C", "c" -> {
                val rel = c == "c"
                fun n(base: Float) = num() + if (rel) base else 0f
                val x1 = n(cx); val y1 = n(cy); val x2 = n(cx); val y2 = n(cy); val x = n(cx); val y = n(cy)
                b.curveTo(x1, y1, x2, y2, x, y)
                lastCx = x2; lastCy = y2; cx = x; cy = y
                while (i < tokens.size && !Regex("[MmLlHhVvCcSsZz]").matches(tokens[i])) {
                    val a1 = n(cx); val b1 = n(cy); val a2 = n(cx); val b2 = n(cy); val ax = n(cx); val ay = n(cy)
                    b.curveTo(a1, b1, a2, b2, ax, ay)
                    lastCx = a2; lastCy = b2; cx = ax; cy = ay
                }
            }
            "S", "s" -> {
                val rel = c == "s"
                fun n(base: Float) = num() + if (rel) base else 0f
                val x2 = n(cx); val y2 = n(cy); val x = n(cx); val y = n(cy)
                val x1 = 2 * cx - lastCx; val y1 = 2 * cy - lastCy
                b.curveTo(x1, y1, x2, y2, x, y)
                lastCx = x2; lastCy = y2; cx = x; cy = y
            }
            "Z", "z" -> { b.close(); cx = startX; cy = startY }
        }
    }
    return b.nodes
}

/** Official 4-color Google "G" — canonical brand path data, no asset file needed. */
val GoogleG: ImageVector by lazy {
    ImageVector.Builder(name = "GoogleG", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 48f, viewportHeight = 48f).apply {
        addPath(pathNodesOf("M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"), fill = SolidColor(Color(0xFFEA4335)))
        addPath(pathNodesOf("M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"), fill = SolidColor(Color(0xFF4285F4)))
        addPath(pathNodesOf("M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"), fill = SolidColor(Color(0xFFFBBC05)))
        addPath(pathNodesOf("M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"), fill = SolidColor(Color(0xFF34A853)))
    }.build()
}

/** Login / signup gate shown at launch until an account session exists (or the user skips). */
@Composable
fun AuthScreen() {
    var mode by remember { mutableStateOf(0) } // 0 = sign in, 1 = create account
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    fun submit() {
        val mail = email.trim()
        if (!mail.contains('@') || mail.length < 5) {
            Cloud.authError = L10n.s("Enter a valid email address", "Entre une adresse email valide")
            return
        }
        if (pw.length < 6) {
            Cloud.authError = L10n.s("Password must be 6+ characters", "Mot de passe de 6 caractères minimum")
            return
        }
        Cloud.authError = null
        scope.launch {
            val err = if (mode == 0) Cloud.signIn(mail, pw) else Cloud.signUp(mail, pw)
            if (err != null) Cloud.authError = err
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(46.dp))
        // logo + wordmark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Liftbook", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(L10n.s("Workout tracker", "Carnet de musculation"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(34.dp))
        Text(
            if (mode == 0) L10n.s("Welcome back", "Content de te revoir", "Bienvenido de nuevo", "Willkommen zurück")
            else L10n.s("Create an account", "Créer un compte", "Crear una cuenta", "Konto erstellen"),
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            L10n.s(
                "Your workouts and routines, synced on all your devices.",
                "Tes séances et tes routines, synchronisées sur tous tes appareils.",
                "Tus entrenamientos y rutinas, sincronizados en todos tus dispositivos.",
                "Deine Workouts und Routinen, auf allen Geräten synchronisiert.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(26.dp))
        AuthField(
            value = email,
            onChange = { email = it; Cloud.authError = null },
            label = L10n.s("Email", "Email", "Correo electrónico", "E-Mail"),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(12.dp))
        AuthField(
            value = pw,
            onChange = { pw = it; Cloud.authError = null },
            label = L10n.s("Password", "Mot de passe", "Contraseña", "Passwort"),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            password = true,
            showPw = showPw,
            onTogglePw = { showPw = !showPw },
            onDone = { submit() },
        )
        if (Cloud.authError != null) {
            Spacer(Modifier.height(10.dp))
            Text(Cloud.authError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.5.sp)
        }
        Spacer(Modifier.height(20.dp))
        val canSubmit = email.isNotBlank() && pw.isNotBlank() && !Cloud.busy
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable(enabled = canSubmit) { submit() },
            contentAlignment = Alignment.Center,
        ) {
            if (Cloud.busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 2.5.dp)
            else Text(
                if (mode == 0) L10n.s("Sign in", "Se connecter", "Iniciar sesión", "Anmelden")
                else L10n.s("Create account", "Créer un compte", "Crear cuenta", "Konto erstellen"),
                color = if (canSubmit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        // "ou" divider
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text(
                L10n.s("or", "ou", "o", "oder"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(18.dp))
        // Google — bouton blanc Hevy ; connexion native (Credential Manager, sans navigateur)
        val googleScope = rememberCoroutineScope()
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .clickable(enabled = !Cloud.busy) {
                    GoogleSignInNative.launch(ctx, googleScope) { result ->
                        when (result) {
                            GoogleSignInNative.NO_ACCOUNT -> {
                                // compte Google présent mais pas visible du système (ou absent) :
                                // ouvrir la connexion Google dans le navigateur — elle accepte n'importe
                                // quel compte Google ET permet d'en créer un
                                Cloud.authError = L10n.s(
                                    "Opening Google sign-in in the browser — sign in with your Google account there (you can also create one). Tip: add the account in Android Settings → Accounts for one-tap sign-in",
                                    "Ouverture de la connexion Google dans le navigateur — connecte-toi avec ton compte Google (tu peux aussi en créer un). Astuce : ajoute le compte dans Réglages Android → Comptes pour la connexion en un geste",
                                    "Abriendo el inicio de sesión de Google en el navegador — inicia sesión con tu cuenta de Google (también puedes crear una). Consejo: añade la cuenta en Ajustes de Android → Cuentas",
                                    "Google-Anmeldung im Browser wird geöffnet — melde dich mit deinem Google-Konto an (du kannst auch eines erstellen). Tipp: Konto unter Android-Einstellungen → Konten hinzufügen",
                                )
                                Cloud.startGoogleAuth(ctx)
                            }
                            GoogleSignInNative.CANCELLED, GoogleSignInNative.ERROR -> {
                                // annulation volontaire ou échec natif : Cloud.authError porte la cause,
                                // on reste sur l'écran (pas de rebond silencieux vers le navigateur)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(GoogleG, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    L10n.s("Continue with Google", "Continuer avec Google", "Continuar con Google", "Mit Google fortfahren"),
                    fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color(0xFF1F1F1F),
                )
            }
        }
        if (Cloud.googlePending) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    L10n.s(
                        "Finishing Google sign-in…",
                        "Connexion Google en cours…",
                        "Completando el acceso con Google…",
                        "Google-Anmeldung läuft…",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                if (mode == 0) L10n.s("No account yet? ", "Pas encore de compte ? ", "¿Sin cuenta? ", "Noch kein Konto? ")
                else L10n.s("Already have an account? ", "Déjà un compte ? ", "¿Ya tienes cuenta? ", "Schon ein Konto? "),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
            )
            Text(
                if (mode == 0) L10n.s("Sign up", "S'inscrire", "Registrarse", "Registrieren")
                else L10n.s("Sign in", "Se connecter", "Iniciar sesión", "Anmelden"),
                color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { mode = 1 - mode; Cloud.authError = null },
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            L10n.s("Continue without an account", "Continuer sans compte", "Continuar sin cuenta", "Ohne Konto fortfahren"),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable {
                Cloud.skipped = true
                Cloud.persistSkip()
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    password: Boolean = false,
    showPw: Boolean = false,
    onTogglePw: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { if (password) onDone() }),
        visualTransformation = if (password && !showPw) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) {
            {
                androidx.compose.material3.IconButton(onClick = onTogglePw) {
                    Icon(if (showPw) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
