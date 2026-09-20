@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hevyclone.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ProfileScreen() {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val st = Repo.settings
    val unit = st.unit
    val showSettings = remember { mutableStateOf(false) }
    var heatYear by remember { mutableStateOf(LocalDate.now().year) }

    // cached aggregates
    val totalVol = remember(rev) { Calc.totalVol(Repo.workouts) }
    val totalSets = remember(rev) { Repo.workouts.sumOf { Calc.setsDone(it) } }
    val totalReps = remember(rev) { Repo.workouts.sumOf { Calc.reps(it) } }
    val dayVolumes = remember(rev) {
        Repo.workouts.groupBy { Calc.dayKey(it.startedAt) }
            .mapValues { (_, ws) -> ws.sumOf { Calc.vol(it) } }
    }
    val dist = remember(rev) { Calc.muscleDist(Repo.workouts, 15) }
    val maxVol = dist.firstOrNull()?.second ?: 1.0

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(st.profileName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(L10n.s("Community", "Communauté"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                IconButton(onClick = { showSettings.value = true }) { Icon(Icons.Rounded.Settings, null) }
            }
        }
        item(key = "volume-heat") {
            SectionTitle(
                L10n.s("Volume", "Volume"),
                "$heatYear",
                onYearTap = { heatYear = if (heatYear > LocalDate.now().year - 5) heatYear - 1 else LocalDate.now().year },
            )
            HeatmapYear(dayVolumes, heatYear)
        }
        item(key = "totals") {
            Text(
                buildString {
                    append("${Calc.fmtVol(totalVol, unit)} kg")
                    append("  ·  ")
                    append(
                        (if (totalSets > 1) L10n.s("%1\$d series", "%1\$d séries") else L10n.s("%1\$d series", "%1\$d série")).format(totalSets)
                    )
                    append("  ·  ")
                    append(L10n.s("%1\$d reps", "%1\$d réps").format(totalReps))
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
        item(key = "month-stats") {
            val monthStart = remember {
                val d = LocalDate.now().withDayOfMonth(1)
                d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val month = remember(rev) { Repo.workouts.filter { it.startedAt >= monthStart } }
            if (month.isNotEmpty()) {
                val mVol = month.sumOf { Calc.vol(it) }
                val mPrs = month.sumOf { it.prs.size }
                Text(
                    L10n.s("This month", "Ce mois-ci") + " : ${month.size} " +
                        L10n.s("workouts", "séances") + " · ${Calc.fmtVol(mVol, unit)} kg · ${mPrs} " +
                        L10n.s("PRs", "records"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                )
            }
        }
        item(key = "rolling-week") {
            val now = System.currentTimeMillis()
            val last7 = remember(rev) { Repo.workouts.filter { it.startedAt >= now - 7L * 86400000L } }
            val prev7 = remember(rev) { Repo.workouts.filter { it.startedAt in (now - 14L * 86400000L) until (now - 7L * 86400000L) } }
            if (last7.isNotEmpty() || prev7.isNotEmpty()) {
                val v1 = last7.sumOf { Calc.vol(it) }
                val v2 = prev7.sumOf { Calc.vol(it) }
                val delta = if (v2 > 0) ((v1 - v2) / v2 * 100).toInt() else null
                Text(
                    L10n.s("Last 7 days", "7 derniers jours") + " : ${Calc.fmtVol(v1, unit)} kg · ${last7.size} " +
                        L10n.s("workouts", "séances") +
                        (delta?.let { "  (" + (if (it >= 0) "+" else "") + "$it% " + L10n.s("vs previous week", "vs semaine précédente") + ")" } ?: "") +
                        "\n" + L10n.s("Previous 7 days", "7 jours précédents") + " : ${Calc.fmtVol(v2, unit)} kg · ${prev7.size} " + L10n.s("workouts", "séances"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                )
            }
        }
        item(key = "monthly-chart") {
            val monthly = remember(rev) {
                val cal = java.time.LocalDate.now().withDayOfMonth(1)
                (5 downTo 0).map { back ->
                    val m = cal.minusMonths(back.toLong())
                    val start = m.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val end = m.plusMonths(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val label = m.format(java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.FRANCE))
                    label to Repo.workouts.filter { it.startedAt in start until end }.sumOf { Calc.vol(it) }
                }
            }
            val maxV = (monthly.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    monthly.forEach { (label, vol) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                                val frac = ((vol / maxV).toFloat()).coerceIn(0f, 1f)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((frac * 76f).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label.take(3), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item(key = "history-link") {
            Spacer(Modifier.height(14.dp))
            AppCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Nav.push(Screen.History) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(L10n.s("Workout history", "Historique des séances"), fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
        item(key = "muscle-title") {
            Text(
                L10n.s("Volume by muscle group", "Volume par groupe musculaire"),
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp),
            )
        }
        if (dist.isEmpty()) {
            item(key = "empty") {
                EmptyState(L10n.s("No data yet.\nYour stats will appear after your first workout.", "Aucune donnée.\nTes stats apparaîtront après ta première séance."))
            }
        } else {
            items(dist.size, key = { dist[it].first }) { i ->
                val (muscle, vol) = dist[i]
                Row(
                    Modifier.fillMaxWidth().animateItemPlacement().padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IllIcon(muscle, 34.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(muscleName(muscle), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("${Calc.fmtVol(vol, unit)} kg", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((vol / maxVol).toFloat().coerceIn(0.02f, 1f))
                                    .animateContentSize()
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
        item(key = "foot") {
            Text(
                L10n.s("Local app · your data stays on this device.", "Application locale · tes données restent sur cet appareil."),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
    if (showSettings.value) SettingsSheet(onClose = { showSettings.value = false })
}

@Composable
private fun SectionTitle(title: String, year: String, onYearTap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { onYearTap() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(year, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(" ˅", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
    }
}

/** GitHub-style year heatmap of daily volume, blue intensity in 4 steps. */
@Composable
private fun HeatmapYear(dayVolumes: Map<String, Double>, year: Int) {
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val days: List<LocalDate> = remember(year) {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        generateSequence(start) { if (it < end) it.plusDays(1) else null }.toList()
    }
    val maxDay = remember(dayVolumes) { dayVolumes.values.maxOrNull() ?: 1.0 }
    val thisYear = LocalDate.now().year

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            drawHeatmap(days, dayVolumes, maxDay, accent, empty, thisYear)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                LocalDate.of(year, 1, 1).format(java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.FRANCE)),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp,
            )
            Text(
                LocalDate.of(year, 12, 31).format(java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.FRANCE)),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp,
            )
        }
    }
}

private fun DrawScope.drawHeatmap(
    days: List<LocalDate>,
    dayVolumes: Map<String, Double>,
    maxDay: Double,
    accent: Color,
    empty: Color,
    currentYear: Int,
) {
    if (days.isEmpty()) return
    val first = days[0]
    val lead = (first.dayOfWeek.value + 6) % 7   // Monday-first
    val weeks = ((lead + days.size) + 6) / 7
    val gap = 2f
    val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
    val today = LocalDate.now()
    days.forEach { d ->
        val idx = d.dayOfYear - 1 + lead
        val w = idx / 7
        val dow = idx % 7
        val v = dayVolumes[d.toString()] ?: 0.0
        val future = d > today
        val color = when {
            future -> empty.copy(alpha = 0.35f)
            v <= 0.0 -> empty
            else -> accent.copy(alpha = (0.30f + 0.70f * ((v / maxDay).coerceIn(0.0, 1.0)).toFloat()))
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(w * (cell + gap), dow * (cell + gap)),
            size = Size(cell, cell),
            cornerRadius = CornerRadius(cell * 0.22f),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val st = Repo.settings
    var confirm by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(bottom = 30.dp)) {
            Text("Réglages", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            Text("PROFIL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
            var nameField by remember { mutableStateOf(Repo.settings.profileName) }
            var handleField by remember { mutableStateOf(Repo.settings.handle) }
            androidx.compose.material3.OutlinedTextField(
                value = nameField,
                onValueChange = { nameField = it },
                label = { Text(L10n.s("Name", "Nom")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            androidx.compose.material3.OutlinedTextField(
                value = handleField,
                onValueChange = { handleField = it.filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }.take(20) },
                label = { Text(L10n.s("Username", "Pseudo")) },
                singleLine = true,
                prefix = { Text("@") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            if (nameField.isNotBlank() && handleField.isNotBlank() && (nameField != Repo.settings.profileName || handleField != Repo.settings.handle)) {
                androidx.compose.material3.Button(
                    onClick = {
                        Repo.settings.profileName = nameField.trim()
                        Repo.settings.handle = handleField.trim()
                        Repo.touchPublic()
                        onClose()
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text(L10n.s("Save profile", "Enregistrer le profil")) }
            }
            Text("UNITÉS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
                listOf("kg", "lb").forEach { u ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (st.unit == u) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickableNoRipple { Repo.setUnit(u) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(u, color = if (st.unit == u) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Text("MINUTEUR DE REPOS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { sec ->
                    Box(Modifier.weight(1f)) { Chip("${sec}s", st.restSec == sec, onClick = { Repo.setRest(sec) }) }
                }
            }
            Text("COULEUR D'ACCENT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    "blue" to androidx.compose.ui.graphics.Color(0xFF028CFD),
                    "teal" to androidx.compose.ui.graphics.Color(0xFF20B49A),
                    "violet" to androidx.compose.ui.graphics.Color(0xFF7C5CFF),
                    "orange" to androidx.compose.ui.graphics.Color(0xFFFF7A45),
                ).forEach { (key, color) ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .border(
                                if (st.accent == key) 3.dp else 1.dp,
                                if (st.accent == key) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                                androidx.compose.foundation.shape.CircleShape,
                            )
                            .clickable { Repo.setAccent(key); onClose(); (ctx as? android.app.Activity)?.recreate() },
                    )
                }
            }
            Text("DONNÉES", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            // Hevy / Strong full-account import (.csv or the exported .zip)
            val hevyPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
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
                            toast(ctx, L10n.s("No Hevy data found — header:", "Aucune donnée Hevy trouvée — en-tête :") + " " + head)
                        } else {
                            val n = Repo.importHevy(ws, rs)
                            toast(ctx, L10n.s(
                                "%1\$d workouts + %2\$d routines imported from Hevy",
                                "%1\$d séances + %2\$d routines importées depuis Hevy"
                            ).format(n, rs.size))
                        }
                    }.onFailure { toast(ctx, L10n.s("Import failed (check format)", "Import échoué (vérifie le format)")) }
                }
            }
            TextButton(onClick = { hevyPicker.launch("*/*") }, modifier = Modifier.padding(start = 8.dp)) {
                Text(L10n.s("Import from Hevy (account export)", "Importer depuis Hevy (export du compte)"), fontWeight = FontWeight.SemiBold)
            }
            val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()?.let { text ->
                        var n = Repo.importCsv(text)
                        if (n == 0) {
                            // fallback: a Hevy account export fed to the generic importer
                            val (ws, rs) = com.hevyclone.app.data.Calc.parseHevyCsv(text)
                            if (ws.isNotEmpty() || rs.isNotEmpty()) n = Repo.importHevy(ws, rs)
                        }
                        toast(ctx, if (n > 0) L10n.s("%1\$d workouts imported", "%1\$d séances importées").format(n)
                               else L10n.s("Nothing imported (check format)", "Rien d'importé (vérifie le format)"))
                    }
                }
            }
            TextButton(onClick = { filePicker.launch("text/*") }, modifier = Modifier.padding(start = 8.dp)) {
                Text(L10n.s("Import workouts (CSV)", "Importer des séances (CSV)"), fontWeight = FontWeight.SemiBold)
            }
            val restorePicker = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()?.let { text ->
                        val ok = Repo.restoreBackup(text)
                        toast(ctx, if (ok) L10n.s("Backup restored", "Sauvegarde restaurée") else L10n.s("Invalid backup file", "Fichier de sauvegarde invalide"))
                    }
                }
            }
            TextButton(onClick = { shareBackup(ctx) }, modifier = Modifier.padding(start = 8.dp)) {
                Text(L10n.s("Backup data (JSON)", "Sauvegarder les données (JSON)"), fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { restorePicker.launch("*/*") }, modifier = Modifier.padding(start = 8.dp)) {
                Text(L10n.s("Restore backup", "Restaurer une sauvegarde"), fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { exportCsv(ctx) }, modifier = Modifier.padding(start = 8.dp)) {
                Text(L10n.s("Export workouts (CSV)", "Exporter les séances (CSV)"), fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { confirm = true }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Tout effacer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Tout effacer ?", fontWeight = FontWeight.Bold) },
            text = { Text("Toutes les séances, routines et records seront définitivement supprimés de cet appareil.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    Repo.wipe()
                    onClose()
                }) { Text("Effacer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler") } },
        )
    }
}
