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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.CUES
import com.hevyclone.app.data.EQUIP_HINT
import com.hevyclone.app.data.EXERCISES
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo

@Composable
fun ExercisesScreen() {
    var q by remember { mutableStateOf("") }
    var mus by remember { mutableStateOf("All") }
    val filtered = EXERCISES
        .filter { (mus == "All" || it.muscle == mus) && (q.isBlank() || it.name.lowercase().contains(q.trim().lowercase())) }
        .sortedBy { it.name }

    Column(Modifier.fillMaxSize()) {
        Text("Exercises", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp))
        TextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text("Search exercises", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp)) },
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp).height(52.dp),
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf("All") + com.hevyclone.app.data.MUSCLES) { m ->
                Chip(m, mus == m, onClick = { mus = m })
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (filtered.isEmpty()) item { EmptyState("No exercises found.") }
            else items(filtered.size) { i ->
                val e = filtered[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Nav.push(Screen.ExerciseDetail(e.name)) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${e.muscle} · ${e.equip}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun ExerciseDetailScreen(name: String) {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val unit = Repo.settings.unit
    val pr = Repo.prFor(name)
    val series = Repo.e1rmSeries(name)
    val cues = CUES[com.hevyclone.app.data.EX[name]?.muscle] ?: emptyList()
    val equip = com.hevyclone.app.data.EX[name]?.equip ?: ""
    val muscle = com.hevyclone.app.data.EX[name]?.muscle ?: ""

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MuscleTag(muscle)
                    MuscleTag(equip)
                }
            }
            if (Repo.draft?.mode == "workout") item {
                PrimaryButton(
                    "Add to Current Workout",
                    onClick = {
                        Repo.addExToDraft(name)
                        Nav.pop()
                        toast(ctx, "$name added to workout")
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    leading = { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(17.dp)) },
                )
            }
            item {
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Text("How to", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        EQUIP_HINT[equip]?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp) }
                        Spacer(Modifier.height(6.dp))
                        cues.forEach { c ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(c, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
            if (pr != null) {
                item {
                    AppCard {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Personal Records", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            PrRow("Heaviest set", "${Calc.fmtKg(pr.weight, unit)} ${Calc.unitLabel(unit)} · ${Calc.fmtDateShort(pr.weightDate)}")
                            PrRow("Best est. 1RM", "${Calc.fmtKg(pr.e1rm, unit)} ${Calc.unitLabel(unit)} · ${Calc.fmtDateShort(pr.e1rmDate)}")
                        }
                    }
                }
                if (series.size >= 2) item {
                    AppCard {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Est. 1RM History", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            LineChart(
                                series.map { (l, v) -> l to Calc.toDisplay(v, unit) },
                                fmtLabel = { Calc.fmtKg(it, unit) },
                            )
                        }
                    }
                }
            }
            item {
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("History", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        val sessions = Repo.workoutsDesc().mapNotNull { w ->
                            w.exercises.firstOrNull { it.name == name }?.let { w to it }
                        }.take(8)
                        if (sessions.isEmpty()) {
                            Text("No sessions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        } else sessions.forEach { (w, ex) ->
                            val best = ex.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 && it.done }
                                .maxByOrNull { Calc.e1rm(it.kg!!, it.reps!!) }
                            PrRow(
                                "${w.name} · ${Calc.fmtDateShort(w.startedAt)}",
                                if (best != null) "${Calc.fmtKg(best.kg, unit)} ${Calc.unitLabel(unit)} × ${best.reps}" else "—",
                                subLeft = "",
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Form cues are general guidelines — adjust to your mobility and equipment.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
