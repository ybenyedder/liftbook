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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
        .filter { (mus == "All" || it.muscle == mus) && com.hevyclone.app.data.L10nData.matches(q, it.name) }
        .sortedBy { exName(it.name) }

    Column(Modifier.fillMaxSize()) {
        Text("Exercises", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp))
        TextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text("Search exercises", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp)) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(autoCorrect = false),
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
                        Text(exName(e.name), fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${muscleName(e.muscle)} · ${equipName(e.equip)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
    val def = com.hevyclone.app.data.EX[name]
    var tab by remember { mutableStateOf(0) }   // 0=Charts 1=History 2=About
    var period by remember { mutableStateOf(3) } // 0=3m 1=6m 2=1y 3=all
    val historySessions = remember(rev, name) {
        Repo.workoutsDesc().mapNotNull { w -> w.exercises.firstOrNull { it.name == name }?.let { w to it } }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(
                exName(name),
                fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // tab row with underline
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf(
                L10n.s("Charts", "Graphiques"),
                L10n.s("History", "Historique"),
                L10n.s("About", "À propos"),
            ).forEachIndexed { i, label ->
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { tab = i }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        color = if (tab == i) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> {
                    item {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                L10n.s("3m", "3m"), L10n.s("6m", "6m"), L10n.s("1y", "1a"), L10n.s("All", "Tout"),
                            ).forEachIndexed { i, label ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (period == i) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (period == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(999.dp),
                                        )
                                        .clickable { period = i }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        label,
                                        color = if (period == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        val cutoff = when (period) {
                            0 -> System.currentTimeMillis() - 91L * 86400000L
                            1 -> System.currentTimeMillis() - 182L * 86400000L
                            2 -> System.currentTimeMillis() - 365L * 86400000L
                            else -> 0L
                        }
                        val sessions = remember(rev, period, name) {
                            Repo.workouts.filter { it.startedAt >= cutoff }
                                .mapNotNull { w -> w.exercises.firstOrNull { it.name == name }?.let { w to it } }
                        }
                        val heaviest = sessions.map { (w, e) ->
                            Calc.fmtDateShort(w.startedAt) to (e.sets.maxOfOrNull { it.kg ?: 0.0 } ?: 0.0)
                        }
                        val volumes = sessions.map { (w, e) ->
                            Calc.fmtDateShort(w.startedAt) to e.sets.filter { it.done }.sumOf { (it.kg ?: 0.0) * (it.reps ?: 0) }
                        }
                        if (heaviest.isEmpty()) {
                            EmptyState(L10n.s("No data yet.\nLog this exercise to see charts.", "Aucune donnée.\nEnregistre cet exercice pour voir les graphiques."))
                        } else {
                            // records summary card
                            val pr = Repo.prFor(name)
                            val best1rm = sessions.flatMap { (_, e) -> e.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 }.map { Calc.e1rm(it.kg!!, it.reps!!) } }.maxOrNull()
                            val bestSet = sessions.flatMap { (_, e) -> e.sets.mapNotNull { it.kg } }.maxOrNull()
                            AppCard {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text(L10n.s("Records", "Records"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(L10n.s("Best est. 1RM", "1RM max est."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                            Text("${Calc.fmtKg(best1rm, unit)} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(L10n.s("Heaviest set", "Série lourde"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                            Text("${Calc.fmtKg(bestSet, unit)} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(L10n.s("Sessions", "Séances"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                            Text("${sessions.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                                    if (pr != null) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            L10n.s("PR set on ", "Record établi le ") + Calc.fmtDateShort(pr.weightDate),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp,
                                        )
                                    }
                                }
                            }
                            AppCard {
                                Column(Modifier.padding(14.dp)) {
                                    Text(L10n.s("Heaviest weight", "Poids le plus lourd"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${Calc.fmtKg(heaviest.lastOrNull()?.second, unit)} kg",
                                        fontSize = 19.sp, fontWeight = FontWeight.Bold,
                                    )
                                    LineChart(heaviest, fmtLabel = { Calc.fmtKg(it, unit) })
                                }
                            }
                            AppCard {
                                Column(Modifier.padding(14.dp)) {
                                    Text(L10n.s("Total volume", "Volume total"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${Calc.fmtVol(volumes.sumOf { it.second }, unit)} kg",
                                        fontSize = 19.sp, fontWeight = FontWeight.Bold,
                                    )
                                    LineChart(volumes, fmtLabel = { Calc.fmtVol(it, unit) })
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val sessions = historySessions
                    if (sessions.isEmpty()) item { EmptyState(L10n.s("No sessions logged yet.", "Aucune séance enregistrée.")) }
                    else items(sessions.size, key = { sessions[it].first.id }) { i ->
                        val (w, ex) = sessions[i]
                        Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(Calc.fmtDateFull(w.startedAt), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(
                                    "${Calc.fmtVol(ex.sets.filter { it.done }.sumOf { (it.kg ?: 0.0) * (it.reps ?: 0) }, unit)} kg",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                                )
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Text("SÉRIE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("KG", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("RÉPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                            ex.sets.forEachIndexed { si, st ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(if (si % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent)
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                ) {
                                    Text("${si + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text(if (st.kg != null) Calc.fmtKg(st.kg, unit) else "—", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("${st.reps ?: "—"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                else -> {
                    item { AnimatedDemo(def?.muscle ?: "Chest") }
                    item {
                        AppCard {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    equipName(def?.equip ?: ""),
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                com.hevyclone.app.data.L10nData.equipHint(def?.equip ?: "").takeIf { it.isNotEmpty() }?.let {
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    L10n.s("PRIMARY MUSCLE", "MUSCLE PRINCIPAL"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(muscleName(def?.muscle ?: ""), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                    item {
                        AppCard {
                            Column(Modifier.padding(14.dp)) {
                                val cues = com.hevyclone.app.data.L10nData.cues(def?.muscle ?: "")
                                cues.forEachIndexed { ci, cue ->
                                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                                        Text(
                                            "${ci + 1}.",
                                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                            modifier = Modifier.width(22.dp),
                                        )
                                        Text(cue, fontSize = 14.sp, lineHeight = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
        if (Repo.draft?.mode == "workout") {
            PrimaryButton(
                L10n.s("Add to Current Workout", "Ajouter à la séance en cours"),
                onClick = {
                    Repo.addExToDraft(name)
                    Nav.pop()
                    toast(ctx, "${exName(name)} ajouté")
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leading = { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(17.dp)) },
            )
        }
    }
}


/** Animated instruction demo: pulsing target muscle on the pictogram (play/pause). */
@Composable
private fun AnimatedDemo(muscle: String) {
    var playing by remember { mutableStateOf(true) }
    val ctx = LocalContext.current
    val avd = remember(muscle) {
        ctx.getDrawable(illAnimRes(muscle)) as? android.graphics.drawable.AnimatedVectorDrawable
    }
    LaunchedEffect(avd, playing) {
        if (playing) avd?.start() else avd?.stop()
    }
    androidx.compose.runtime.DisposableEffect(avd) {
        onDispose { avd?.stop() }
    }
    AppCard {
        Box(
            Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { viewCtx ->
                    android.widget.ImageView(viewCtx).apply { setImageDrawable(avd) }
                },
                update = { iv -> if (iv.drawable !== avd) iv.setImageDrawable(avd) },
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
            IconButton(
                onClick = { playing = !playing },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}
