package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Cloud
import kotlinx.coroutines.launch

/** Login / signup gate shown at launch until an account session exists (or the user skips). */
@Composable
fun AuthScreen() {
    var mode by remember { mutableStateOf(0) } // 0 = sign in, 1 = create account
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        Spacer(Modifier.height(58.dp))
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            if (mode == 0) L10n.s("Welcome back", "Content de te revoir", "Bienvenido de nuevo", "Willkommen zurück")
            else L10n.s("Create an account", "Créer un compte", "Crear una cuenta", "Konto erstellen"),
            fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            L10n.s(
                "Sign in to sync your workouts, routines and settings across all your devices.",
                "Connecte-toi pour retrouver tes séances, routines et réglages sur tous tes appareils.",
                "Inicia sesión para sincronizar tus entrenamientos y rutinas en todos tus dispositivos.",
                "Melde dich an, um Workouts, Routinen und Einstellungen auf allen Geräten zu synchronisieren.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(30.dp))
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
        Spacer(Modifier.height(22.dp))
        val canSubmit = email.isNotBlank() && pw.isNotBlank() && !Cloud.busy
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
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
        Spacer(Modifier.height(44.dp))
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
