package com.hevyclone.app.ui

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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.Workout
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryScreen() {
    val rev = Repo.rev
    val now = java.time.LocalDate.now()
    var viewYear by remember { mutableStateOf(now.year) }
    var viewMonth by remember { mutableStateOf(now.monthValue) }
    var showCalendar by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) } // 0=Toutes 1=Semaine 2=Mois 3=Année
    val byDay = remember(rev) { Repo.workouts.groupBy { Calc.dayKey(it.startedAt) } }
    val cutoff = remember(filter) {
        val now = System.currentTimeMillis()
        when (filter) {
            1 -> now - 7L * 86400000L
            2 -> now - 30L * 86400000L
            3 -> now - 365L * 86400000L
            else -> 0L
        }
    }
    val filteredWorkouts = remember(rev, query, filter) {
        Repo.workoutsDesc().filter { w ->
            w.startedAt >= cutoff && (
                query.isBlank() ||
                com.hevyclone.app.data.L10nData.matches(query, "") .let { true } && (
                    w.name.lowercase().contains(query.lowercase()) ||
                    w.exercises.any { com.hevyclone.app.data.L10nData.matches(query, it.name) }
                )
            )
        }
    }
    val groups = remember(rev, query, filter) { buildGroups(filteredWorkouts) }
    val month = YearMonth.of(viewYear, viewMonth)
    val today = LocalDate.now()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(L10n.s("History", "Historique"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showCalendar = !showCalendar }) { Icon(Icons.Rounded.CalendarMonth, null) }
            }
        }
        item(key = "search") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text(
                            L10n.s("Search workouts", "Rechercher des séances"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                        )
                        inner()
                    },
                )
            }
        }
        item(key = "filters") {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    L10n.s("All", "Toutes"),
                    L10n.s("This week", "Cette semaine"),
                    L10n.s("This month", "Ce mois"),
                    L10n.s("This year", "Cette année"),
                ).forEachIndexed { i, label ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (filter == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                if (filter == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { filter = i }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            label,
                            color = if (filter == i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        if (showCalendar) item { CalendarCard(month, byDay, today, onPrev = {
            if (viewMonth == 1) { viewMonth = 12; viewYear-- } else viewMonth--
        }, onNext = {
            if (viewMonth == 12) { viewMonth = 1; viewYear++ } else viewMonth++
        }, onDay = { key ->
            byDay[key]?.firstOrNull()?.let { Nav.push(Screen.WorkoutDetail(it.id)) }
        }) }
        if (groups.isEmpty()) item { EmptyState("Aucune séance enregistrée.") }
        else {
            groups.forEach { g ->
                item(key = "label-${g.first}") {
                    Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(g.first, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                items(g.second.size, key = { g.second[it].id }) { i ->
                    val w = g.second[i]
                    HistoryRow(w, onClick = { Nav.push(Screen.WorkoutDetail(w.id)) })
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun buildGroups(desc: List<Workout>): List<Pair<String, List<Workout>>> {
    val ws = Calc.weekStart(System.currentTimeMillis())
    val groups = mutableListOf<Pair<String, MutableList<Workout>>>()
    for (w in desc) {
        val label = when {
            w.startedAt >= ws -> "Cette semaine"
            w.startedAt >= ws - 7L * 86400000L -> "Semaine dernière"
            else -> java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.US)
                .format(Calc.localDate(w.startedAt))
        }
        val g = groups.firstOrNull { it.first == label }
        if (g != null) g.second.add(w) else groups.add(label to mutableListOf(w))
    }
    return groups
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    byDay: Map<String, List<Workout>>,
    today: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDay: (String) -> Unit,
) {
    AppCard {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Rounded.ChevronLeft, null, tint = MaterialTheme.colorScheme.onBackground) }
                Text(
                    java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRANCE).format(month.atDay(1))
                        .replaceFirstChar { it.uppercase(java.util.Locale.FRANCE) },
                    fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onBackground) }
            }
            val dows = listOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di")
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                dows.forEach { d ->
                    Text(d, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val lead = (month.atDay(1).dayOfWeek.value + 6) % 7 // Monday-first
            val days = month.lengthOfMonth()
            val rows = (0 until ((lead + days + 6) / 7))
            Column(Modifier.padding(horizontal = 10.dp)) {
                rows.forEach { r ->
                    Row(Modifier.fillMaxWidth().height(38.dp)) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c
                            val dayNum = idx - lead + 1
                            Box(Modifier.weight(1f).fillMaxSize().padding(1.dp), contentAlignment = Alignment.Center) {
                                if (dayNum in 1..days) {
                                    val key = "%04d-%02d-%02d".format(month.year, month.monthValue, dayNum)
                                    val has = byDay[key]?.isNotEmpty() == true
                                    val isToday = today.year == month.year && today.monthValue == month.monthValue && today.dayOfMonth == dayNum
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (has) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
                                            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
                                            .clickable(enabled = has) { onDay(key) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("$dayNum", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                        if (has) {
                                            Box(
                                                Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(w: Workout, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val d = Calc.localDate(w.startedAt)
        Column(
            Modifier.width(46.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.FRANCE).uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
            Text("${d.dayOfMonth}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(w.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Calc.fmtTime(w.startedAt)} — ${Calc.fmtTime(w.endedAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            )
        }
        Text(
            "${Calc.fmtVol(Calc.vol(w), Repo.settings.unit)} kg",
            color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------------- workout detail ----------------

@Composable
fun WorkoutDetailScreen(id: Long) {
    val rev = Repo.rev
    val w = Repo.workoutById(id)
    // null after a delete: the delete flow already popped this screen — popping again
    // during the crossfade-out would eject the underlying screen too.
    if (w == null) return
    var confirmDelete by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(w.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            var detailMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { detailMenu = true }) { Icon(Icons.Rounded.MoreHoriz, null) }
                androidx.compose.material3.DropdownMenu(expanded = detailMenu, onDismissRequest = { detailMenu = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.s("Create routine from workout", "Créer une routine depuis cette séance")) },
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            detailMenu = false
                            Repo.routineFromWorkout(w.id, w.name)
                            toast(ctx, L10n.s("Routine created", "Routine créée"))
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.s("Repeat this workout", "Refaire cette séance")) },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            detailMenu = false
                            if (Repo.draft != null) { toast(ctx, L10n.s("Finish the current workout first", "Termine d'abord la séance en cours")) }
                            else {
                                Repo.startRepeat(w.id)
                                Nav.toTab(Screen.TrainingTab)
                                Nav.push(Screen.Logger)
                            }
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.s("Share workout", "Partager la séance")) },
                        leadingIcon = { Icon(Icons.Rounded.IosShare, null, modifier = Modifier.size(16.dp)) },
                        onClick = { detailMenu = false; shareWorkout(ctx, w) },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.s("Delete workout", "Supprimer la séance"), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                        onClick = { detailMenu = false; confirmDelete = true },
                    )
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "${Calc.fmtDateFull(w.startedAt)}, ${Calc.fmtTime(w.startedAt)} — ${Calc.fmtTime(w.endedAt)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(L10n.s("Time", "Temps"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(Calc.fmtDur(w.endedAt - w.startedAt), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1.4f)) {
                        Text(L10n.s("Volume", "Volume"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text("${Calc.fmtVol(Calc.vol(w), Repo.settings.unit)} kg", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(L10n.s("Records", "Records"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏅", fontSize = 13.sp)
                            Spacer(Modifier.width(3.dp))
                            Text("${w.prs.size}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(L10n.s("Sets", "Séries"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text("${Calc.setsDone(w)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (w.prs.isNotEmpty()) item {
                AppCard {
                    Row(Modifier.padding(14.dp)) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            w.prs.forEach { p ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(exName(p.ex), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${Calc.fmtKg(p.value, Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)} · ${p.kind}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (w.notes.isNotBlank()) item(key = "notes") {
                AppCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(w.notes, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
            items(w.exercises.size, key = { w.exercises[it].name + it }) { ei ->
                val ex = w.exercises[ei]
                val prevSets = remember(ex.name, w.id) { Repo.prevSetsBefore(w.startedAt, ex.name) }
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { Nav.push(Screen.ExerciseDetail(ex.name)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exName(ex.name), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${ex.sets.size} " + if (ex.sets.size > 1) L10n.s("series", "séries") else L10n.s("series", "série"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text("SÉRIE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                            Text(L10n.s("PREVIOUS", "PRÉCÉDENTE"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                            Text("KG", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("RÉPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                        ex.sets.forEachIndexed { i, s ->
                            Row(
                                Modifier.fillMaxWidth().background(if (i % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent)
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${i + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(34.dp))
                                val prevTxt = prevSets?.getOrNull(i)
                                Text(
                                    prevTxt ?: "—",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                                    modifier = Modifier.weight(1.1f),
                                )
                                Text(
                                    if (s.kg != null) Calc.fmtKg(s.kg, Repo.settings.unit) else "—",
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${s.reps ?: "—"}",
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                if (s.prW || s.prE) {
                                    Text(
                                        if (s.prW) "WEIGHT PR" else "1RM PR",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                        if (ex.notes.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(ex.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }

        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer la séance ?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Cette séance et ses records seront définitivement supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Repo.deleteWorkout(w.id)
                    DeletedUndo.set(w)
                    Nav.pop()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
