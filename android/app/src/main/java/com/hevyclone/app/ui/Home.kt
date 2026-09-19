package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val rev = Repo.rev // subscribe to data changes
    val st = Repo.settings
    val now = System.currentTimeMillis()
    val week = Calc.weekStats(Repo.workouts, now)
    val streak = Calc.streak(Repo.workouts, now)
    val routines = Repo.routines
    val recent = Repo.workoutsDesc().take(4)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(st.profileName.trim().take(1).uppercase(), 40)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(st.profileName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text("@${st.handle}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                // streak chip
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$streak", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
        item {
            PrimaryButton(
                "Start an Empty Workout",
                onClick = {
                    if (Repo.draft != null) {
                        toast(ctx, "Workout resumed")
                    } else {
                        Repo.startWorkout(null)
                    }
                    Nav.push(Screen.Logger)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                leading = { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(19.dp)) },
            )
        }
        item { SectionLabel("This week") }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(62.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.weight(1f)) { Metric("${week.count}", "Workouts", tight = true) }
                Box(Modifier.weight(1f)) { Metric(Calc.fmtVol(week.vol, st.unit), "Volume (${Calc.unitLabel(st.unit)})", tight = true) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(62.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.weight(1f)) { Metric("${week.reps}", "Reps", tight = true) }
                Box(Modifier.weight(1f)) { Metric("${week.prs}", "PRs", accent = true, tight = true) }
            }
        }
        item { SectionLabel("Routines") }
        item {
            if (routines.isEmpty()) EmptyState("No routines yet.", slim = true)
            else LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(routines.size) { i ->
                    val r = routines[i]
                    Column(
                        Modifier
                            .width(176.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickableNoRipple { Repo.startRoutine(r.id); Nav.push(Screen.Logger) }
                            .padding(13.dp),
                    ) {
                        Text(r.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.exercises.size} exercise${if (r.exercises.size > 1) "s" else ""}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickableNoRipple {
                                    if (Repo.draft != null) toast(ctx, "Finish or discard the current workout first")
                                    else { Repo.startWorkout(r.id); Nav.push(Screen.Logger) }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Start Workout", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        item { SectionLabel("Recent activity") }
        if (recent.isEmpty()) item { EmptyState("No workouts yet — start your first one!") }
        else items(recent.size) { i ->
            val w = recent[i]
            HistoryRow(w, onClick = { Nav.push(Screen.WorkoutDetail(w.id)) })
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
