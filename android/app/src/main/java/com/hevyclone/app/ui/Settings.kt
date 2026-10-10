package com.hevyclone.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.AvatarCache
import com.hevyclone.app.data.Cloud
import com.hevyclone.app.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full settings page — plain black, hairline rows, blue reserved for actions. */
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val rev = Repo.rev
    var editName by remember { mutableStateOf(false) }
    var editHandle by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ---------- pickers (parse + import run off the main thread: full Hevy exports
    //              are several MB and used to freeze/ANR the UI) ----------
    val hevyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val msg = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching ""
                    val csvs: List<String> = if (bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
                        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { z ->
                            val out = mutableListOf<String>()
                            var e = z.nextEntry
                            while (e != null) {
                                if (!e.isDirectory && e.name.endsWith(".csv", true)) out.add(String(z.readBytes()))
                                e = z.nextEntry
                            }
                            out
                        }
                    } else listOf(String(bytes))
                    var ws = listOf<com.hevyclone.app.data.Workout>()
                    var rs = listOf<com.hevyclone.app.data.Routine>()
                    for (csv in csvs) {
                        val (w, r) = com.hevyclone.app.data.Calc.parseHevyCsv(csv)
                        ws += w; rs += r
                    }
                    if (ws.isEmpty() && rs.isEmpty()) {
                        val head = csvs.firstOrNull()?.lineSequence()?.firstOrNull()?.take(90) ?: ""
                        L10n.s("No Hevy data found — header:", "Aucune donnée Hevy trouvée — en-tête :") + " " + head
                    } else {
                        val n = Repo.importHevy(ws, rs)
                        L10n.s("%1\$d workouts imported from Hevy (routines rebuilt)", "%1\$d séances importées (routines reconstruites)").format(n)
                    }
                }.getOrElse { L10n.s("Import failed (check format)", "Import échoué (vérifie le format)") }
            }
            if (msg.isNotEmpty()) toast(ctx, msg)
        }
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val msg = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    .getOrNull()?.let { text ->
                        var n = Repo.importCsv(text)
                        if (n == 0) {
                            val (ws, rs) = com.hevyclone.app.data.Calc.parseHevyCsv(text)
                            if (ws.isNotEmpty() || rs.isNotEmpty()) n = Repo.importHevy(ws, rs)
                        }
                        if (n > 0) L10n.s("%1\$d workouts imported", "%1\$d séances importées").format(n)
                        else L10n.s("Nothing imported (check format)", "Rien d'importé (vérifie le format)")
                    } ?: L10n.s("Import failed (check format)", "Import échoué (vérifie le format)")
            }
            toast(ctx, msg)
        }
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    .getOrNull()?.let { text -> Repo.restoreBackup(text) } ?: false
            }
            toast(ctx, if (ok) L10n.s("Backup restored", "Sauvegarde restaurée") else L10n.s("Invalid backup file", "Fichier de sauvegarde invalide"))
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { applyProfilePhoto(ctx, uri) }
                if (!ok) toast(ctx, L10n.s("Could not read this image", "Impossible de lire cette image"))
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Text(L10n.s("Settings", "Réglages", "Ajustes", "Einstellungen"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            // ---- COMPTE ----
            item {
                SectionHeader(L10n.s("ACCOUNT", "COMPTE", "CUENTA", "KONTO"))
                val sess = Cloud.session
                if (sess != null) {
                    Row(
                        Modifier.fillMaxWidth().clickable { photoPicker.launch("image/*") }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarImg(Repo.settings.profileName.trim().take(1).uppercase(), 56.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(L10n.s("Profile photo", "Photo de profil"), fontSize = 15.5.sp)
                            Text(L10n.s("Tap to change", "Touche pour changer"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    RowDivider()
                    SettingsRow(
                        L10n.s("Sync now", "Synchroniser maintenant"),
                        value = Cloud.syncStatus ?: "",
                        onClick = { Cloud.requestSync(0); toast(ctx, L10n.s("Syncing…", "Synchronisation…")) },
                    )
                    RowDivider()
                    SettingsRow(sess.email, value = "", onClick = {})
                    RowDivider()
                    SettingsRow(L10n.s("Sign out", "Se déconnecter", "Cerrar sesión", "Abmelden"), red = true, onClick = { Cloud.signOut(); Nav.toTab(Screen.ProfileTab) })
                } else {
                    SettingsRow(
                        L10n.s("Sign in or create an account", "Se connecter ou créer un compte", "Iniciar sesión o crear una cuenta", "Anmelden oder Konto erstellen"),
                        blue = true,
                        onClick = { Cloud.skipped = false; Cloud.persistSkip() },
                    )
                }
            }
            // ---- PROFIL ----
            item {
                SectionHeader(L10n.s("PROFILE", "PROFIL", "PERFIL", "PROFIL"))
                SettingsRow(L10n.s("Name", "Nom"), value = Repo.settings.profileName, onClick = { editName = true })
                RowDivider()
                SettingsRow(L10n.s("Username", "Pseudo"), value = "@" + Repo.settings.handle, onClick = { editHandle = true })
            }
            // ---- GÉNÉRAL ----
            item {
                SectionHeader(L10n.s("GENERAL", "GÉNÉRAL", "GENERAL", "ALLGEMEIN"))
                SettingsRow(L10n.s("Units", "Unités", "Unidades", "Einheiten"), value = Repo.settings.unit, onClick = { Repo.setUnit(if (Repo.settings.unit == "kg") "lb" else "kg") })
                RowDivider()
                SettingsRow(L10n.s("Rest timer", "Minuteur de repos", "Temporizador de descanso", "Ruhe-Timer"), value = restLabel(Repo.settings.restSec), onClick = {
                    val next = listOf(60, 90, 120, 180).firstOrNull { it > Repo.settings.restSec } ?: 60
                    Repo.setRest(next)
                })
                RowDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L10n.s("Accent color", "Couleur d'accent", "Color de acento", "Akzentfarbe"), fontSize = 15.5.sp, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "blue" to Color(0xFF028CFD),
                            "teal" to Color(0xFF20B49A),
                            "violet" to Color(0xFF7C5CFF),
                            "orange" to Color(0xFFFF7A45),
                        ).forEach { (key, color) ->
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (Repo.settings.accent == key) 2.5.dp else 0.dp,
                                        MaterialTheme.colorScheme.onBackground,
                                        CircleShape,
                                    )
                                    .clickable {
                                        Repo.setAccent(key)
                                        (ctx as? android.app.Activity)?.recreate()
                                    },
                            )
                        }
                    }
                }
            }
            // ---- DONNÉES ----
            item {
                SectionHeader(L10n.s("DATA", "DONNÉES", "DATOS", "DATEN"))
                SettingsRow(L10n.s("Import from Hevy (account export)", "Importer depuis Hevy (export du compte)"), onClick = { hevyPicker.launch("*/*") })
                RowDivider()
                SettingsRow(L10n.s("Import workouts (CSV)", "Importer des séances (CSV)"), onClick = { csvPicker.launch("text/*") })
                RowDivider()
                SettingsRow(L10n.s("Export workouts (CSV)", "Exporter les séances (CSV)"), onClick = { exportCsv(ctx) })
                RowDivider()
                SettingsRow(L10n.s("Backup data (JSON)", "Sauvegarder les données (JSON)"), onClick = { shareBackup(ctx) })
                RowDivider()
                SettingsRow(L10n.s("Restore backup", "Restaurer une sauvegarde"), onClick = { restorePicker.launch("*/*") })
                RowDivider()
                SettingsRow(L10n.s("Erase all data", "Tout effacer"), red = true, onClick = { confirmWipe = true })
            }
            item {
                Text(
                    "Liftbook " + com.hevyclone.app.BuildConfig.VERSION_NAME + (Cloud.session?.let { " · " + L10n.s("synced as", "synchronisé en tant que") + " " + it.email } ?: ""),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 30.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    if (editName) {
        var v by remember { mutableStateOf(Repo.settings.profileName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(L10n.s("Name", "Nom"), fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = v, onValueChange = { v = it.take(30) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (v.isNotBlank()) Repo.setProfile(v.trim(), Repo.settings.handle); editName = false }) { Text(L10n.s("Save", "Enregistrer")) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
    if (editHandle) {
        var v by remember { mutableStateOf(Repo.settings.handle) }
        AlertDialog(
            onDismissRequest = { editHandle = false },
            title = { Text(L10n.s("Username", "Pseudo"), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = v, onValueChange = { v = it.filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }.take(20) },
                    singleLine = true, prefix = { Text("@") }, modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = { if (v.isNotBlank()) Repo.setProfile(Repo.settings.profileName, v.trim()); editHandle = false }) { Text(L10n.s("Save", "Enregistrer")) } },
            dismissButton = { TextButton(onClick = { editHandle = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(L10n.s("Erase all data?", "Tout effacer ?"), fontWeight = FontWeight.Bold) },
            text = { Text(L10n.s("All workouts, routines and records will be permanently deleted from this device and your account.", "Toutes les séances, routines et records seront définitivement supprimés de cet appareil et de ton compte.")) },
            confirmButton = {
                TextButton(onClick = { confirmWipe = false; Repo.wipe(); Nav.toTab(Screen.HomeTab) }) {
                    Text(L10n.s("Erase", "Effacer"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.surface)
}

@Composable
private fun SettingsRow(title: String, value: String = "", red: Boolean = false, blue: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 15.5.sp,
            color = when {
                red -> MaterialTheme.colorScheme.error
                blue -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (value.isNotBlank()) {
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp, maxLines = 1)
        }
    }
}

private fun restLabel(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "${if (m > 0) "${m}min " else ""}${s}s"
}

/** Downscale + save the picked image locally, then push it to the account storage (if signed in). */
private fun applyProfilePhoto(ctx: android.content.Context, uri: android.net.Uri): Boolean {
    val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return false
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0) return false
    var sample = 1
    while (bounds.outWidth / sample > 640 || bounds.outHeight / sample > 640) sample *= 2
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return false
    // center-crop to a square
    val side = minOf(bmp.width, bmp.height)
    val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
    val out = java.io.ByteArrayOutputStream()
    square.compress(Bitmap.CompressFormat.JPEG, 88, out)
    val jpg = out.toByteArray()
    AvatarCache.file(ctx).writeBytes(jpg)
    AvatarCache.reload(ctx)
    Repo.touchPublic()
    val s = Cloud.session
    if (s != null) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val url = Cloud.uploadAvatar(jpg)
            if (url != null) Repo.setAvatar(url)
            else android.util.Log.w("LiftbookPhoto", "avatar upload failed")
        }
    }
    return true
}
