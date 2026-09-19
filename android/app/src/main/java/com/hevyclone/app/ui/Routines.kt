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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo

@Composable
fun RoutinesScreen() {
    val ctx = LocalContext.current
    val rev = Repo.rev
    val routines = Repo.routines
    val byDay = Repo.workouts.groupBy { Calc.dayKey(it.startedAt) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Training", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Today, ${java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.US).format(java.time.LocalDate.now())}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                )
            }
            IconButton(onClick = {
                if (Repo.draft != null) { Nav.push(Screen.Logger); return@IconButton }
                Repo.startRoutine(null)
                Nav.push(Screen.Logger)
            }) { Icon(Icons.Rounded.Add, null) }
        }
        WeekStrip(byDay)
        PrimaryButton(
            "Start an Empty Workout",
            onClick = {
                if (Repo.draft != null) { Nav.push(Screen.Logger); return@PrimaryButton }
                Repo.startWorkout(null)
                Nav.push(Screen.Logger)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            leading = { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(19.dp)) },
        )
        Text(
            "Routines", color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (routines.isEmpty()) item { EmptyState("No routines yet.\nCreate one to reuse your favorite workouts.") }
            else items(routines.size) { i ->
                val r = routines[i]
                val last = Repo.routineLastPerformed(r)
                AppCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    Row(Modifier.clickable {
                        if (Repo.draft != null && Repo.draft?.mode != "routine") toast(ctx, "Finish or discard the current workout first")
                        else if (Repo.draft != null) Nav.push(Screen.Logger)
                        else { Repo.startRoutine(r.id); Nav.push(Screen.Logger) }
                    }.padding(14.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${r.exercises.size} exercise${if (r.exercises.size > 1) "s" else ""} · ${if (last != null) "Last performed ${Calc.fmtDateShort(last)}" else "Never performed"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.exercises.take(2).forEach { e -> MuscleTag(e.name) }
                                if (r.exercises.size > 2) MuscleTag("+${r.exercises.size - 2}")
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickableNoRipple {
                                        if (Repo.draft != null) { toast(ctx, "Finish or discard the current workout first"); return@clickableNoRipple }
                                        Repo.startWorkout(r.id)
                                        Nav.push(Screen.Logger)
                                    }
                                    .padding(horizontal = 13.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Start", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** MON–SUN strip: letters, day numbers, dot on workout days, today filled with accent. */
@Composable
fun WeekStrip(byDay: Map<String, List<com.hevyclone.app.data.Workout>>) {
    val today = java.time.LocalDate.now()
    val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    val labels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        labels.forEachIndexed { i, label ->
            val date = monday.plusDays(i.toLong())
            val key = date.toString()
            val has = byDay[key]?.isNotEmpty() == true
            val isToday = date == today
            Column(
                Modifier.weight(1f).clickable(enabled = has) {
                    byDay[key]?.firstOrNull()?.let { Nav.push(Screen.WorkoutDetail(it.id)) }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${date.dayOfMonth}",
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (has) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }
        }
    }
}
