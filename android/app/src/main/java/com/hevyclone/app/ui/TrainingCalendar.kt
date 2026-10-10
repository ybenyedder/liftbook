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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Entraînement ▾ → Calendrier" — month view with workout days highlighted in blue (Hevy-style). */
@Composable
fun TrainingCalendar() {
    val rev = Repo.rev
    val unit = Repo.settings.unit
    // Survives rotation/process death (was a plain remember).
    var monthStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthStr) { YearMonth.parse(monthStr) }
    val loc = if (L10n.lang == "fr") Locale.FRENCH else Locale.US

    val byDay = remember(rev) {
        Repo.workouts.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() }
    }

    val days: List<LocalDate?> = remember(month) {
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value + 6) % 7 // Monday-first
        val len = month.lengthOfMonth()
        List(lead) { null } + (1..len).map { month.atDay(it) }
    }
    val monthWorkouts = remember(rev, month) {
        val from = month.atDay(1).toEpochDay()
        val to = month.atEndOfMonth().toEpochDay()
        byDay.filterKeys { it in from..to }.values.flatten()
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "cal") {
            // month navigation
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { monthStr = month.minusMonths(1).toString() }) { Icon(Icons.Rounded.ChevronLeft, null) }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", loc)).replaceFirstChar { it.uppercase(loc) },
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { if (month.isBefore(YearMonth.now())) monthStr = month.plusMonths(1).toString() }) {
                    Icon(Icons.Rounded.ChevronRight, null, tint = if (month.isBefore(YearMonth.now())) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // weekday header (Monday-first, localized narrow day names)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                java.time.DayOfWeek.entries.forEach { dow ->
                    Text(
                        dow.getDisplayName(java.time.format.TextStyle.NARROW, loc).uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // day grid
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                days.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            Box(Modifier.weight(1f).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                if (d != null) DayCell(d, byDay[d.toEpochDay()]?.maxByOrNull { it.startedAt }, loc)
                                else Spacer(Modifier.size(40.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // month summary
            if (monthWorkouts.isNotEmpty()) {
                val vol = monthWorkouts.sumOf { Calc.vol(it) }
                Text(
                    monthWorkouts.size.toString() + " " + L10n.s("workouts", "séances", "entrenamientos", "Workouts") +
                        "  ·  " + Calc.fmtVol(vol, unit) + " " + Calc.unitLabel(unit),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            } else {
                Text(
                    L10n.s("No workouts this month.", "Aucune séance ce mois-ci.", "Sin entrenamientos este mes.", "Keine Workouts in diesem Monat."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, latestWorkout: com.hevyclone.app.data.Workout?, loc: Locale) {
    val today = remember { LocalDate.now() }
    val hasWorkout = latestWorkout != null
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (hasWorkout) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                width = if (d == today && !hasWorkout) 1.5.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            )
            .clickable(enabled = hasWorkout) { Nav.push(Screen.WorkoutDetail(latestWorkout!!.id)) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${d.dayOfMonth}",
            fontSize = 14.5.sp,
            fontWeight = if (hasWorkout || d == today) FontWeight.Bold else FontWeight.Normal,
            color = if (hasWorkout) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        )
    }
}
