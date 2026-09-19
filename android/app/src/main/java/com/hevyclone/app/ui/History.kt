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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notes
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
    val byDay = Repo.workouts.groupBy { Calc.dayKey(it.startedAt) }
    val month = YearMonth.of(viewYear, viewMonth)
    val today = LocalDate.now()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "History",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
            )
        }
        item { CalendarCard(month, byDay, today, onPrev = {
            if (viewMonth == 1) { viewMonth = 12; viewYear-- } else viewMonth--
        }, onNext = {
            if (viewMonth == 12) { viewMonth = 1; viewYear++ } else viewMonth++
        }, onDay = { key ->
            byDay[key]?.firstOrNull()?.let { Nav.push(Screen.WorkoutDetail(it.id)) }
        }) }
        item { SectionLabel("") }
        val groups = buildGroups(Repo.workoutsDesc())
        if (groups.isEmpty()) item { EmptyState("Aucune séance enregistrée.") }
        else {
            groups.forEach { g ->
                item { Text(g.first.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)) }
                items(g.second.size) { i ->
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
            Text(d.dayOfWeek.toString().take(3).uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
            Text("${d.dayOfMonth}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(w.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Calc.fmtVol(Calc.vol(w), Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)} · ${Calc.setsDone(w)} sets · ${Calc.fmtDur(w.endedAt - w.startedAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

// ---------------- workout detail ----------------

@Composable
fun WorkoutDetailScreen(id: Long) {
    val rev = Repo.rev
    val w = Repo.workoutById(id)
    if (w == null) { Nav.pop(); return }
    var confirmDelete by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(w.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.MoreHoriz, null) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "${Calc.fmtDateFull(w.startedAt)} · ${Calc.fmtTime(w.startedAt)} — ${Calc.fmtTime(w.endedAt)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(62.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Metric(Calc.fmtDur(w.endedAt - w.startedAt), "Durée", tight = true) }
                    Box(Modifier.weight(1f)) { Metric(Calc.fmtVol(Calc.vol(w), Repo.settings.unit), "Volume", unit = Calc.unitLabel(Repo.settings.unit), tight = true) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(62.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Metric("${Calc.setsDone(w)}", "Sets", tight = true) }
                    Box(Modifier.weight(1f)) { Metric("${w.prs.size}", "PRs", accent = true, tight = true) }
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
            items(w.exercises.size) { ei ->
                val ex = w.exercises[ei]
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exName(ex.name), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            MuscleTag(ex.muscle)
                        }
                        Spacer(Modifier.height(8.dp))
                        ex.sets.forEachIndexed { i, s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${i + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                                Text(
                                    if (s.kg != null) "${Calc.fmtKg(s.kg, Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)} × ${s.reps ?: "—"}"
                                    else if (s.reps != null) "× ${s.reps} (bodyweight)" else "—",
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp,
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
            item {
                GhostButton(
                    "Supprimer la séance",
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    leading = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(17.dp)) },
                )
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
                    Nav.toTab(Screen.History)
                    toast(ctx, "Séance supprimée")
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
