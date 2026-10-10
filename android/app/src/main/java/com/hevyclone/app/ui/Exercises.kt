@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hevyclone.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val filtered = remember(q, mus) {
        EXERCISES
            .filter { (mus == "All" || it.muscle == mus) && com.hevyclone.app.data.L10nData.matches(q, it.name) }
            .sortedBy { exName(it.name) }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.muscle } }

    Column(Modifier.fillMaxSize()) {
        var showCreate by remember { mutableStateOf(false) }
        if (showCreate) {
            CreateExerciseDialog(initialName = q, onCreated = { showCreate = false }, onClose = { showCreate = false })
        }
        Text(L10n.s("Exercises", "Exercices"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Card2)
                .clickable { showCreate = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(L10n.s("Create exercise", "Créer un exercice"), color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        TextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text(L10n.s("Search exercises", "Rechercher des exercices"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                Chip(if (m == "All") L10n.s("All", "Tous") else muscleName(m), mus == m, onClick = { mus = m })
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (filtered.isEmpty()) item { EmptyState(L10n.s("No exercises found.", "Aucun exercice trouvé.")) }
            else if (q.isBlank() && mus == "All") {
                grouped.forEach { (muscle, exs) ->
                    item(key = "hdr-$muscle") {
                        Text(
                            muscleName(muscle).uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                        )
                    }
                    items(exs.size, key = { exs[it].name }) { i ->
                        Box(Modifier.animateItemPlacement()) { ExerciseRow(exs[i]) }
                    }
                }
            } else {
                items(filtered.size, key = { filtered[it].name }) { i ->
                    Box(Modifier.animateItemPlacement()) { ExerciseRow(filtered[i]) }
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
        if (def?.equip == "Barbell") {
            var showPlateCalc by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Card2)
                    .clickable { showPlateCalc = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    L10n.s("Plate calculator", "Calculateur de disques"),
                    color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            if (showPlateCalc) PlateCalcSheet(name) { showPlateCalc = false }
        }
        // tab row with animated underline
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val tabWidth = maxWidth / 3
            Column {
                Row(Modifier.fillMaxWidth()) {
                    listOf(
                        L10n.s("Summary", "Résumé"),
                        L10n.s("History", "Historique"),
                        L10n.s("Instructions", "Instructions"),
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
                        }
                    }
                }
                val underline by animateDpAsState(
                    targetValue = tabWidth * tab,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "tabUnderline",
                )
                Box(
                    Modifier
                        .offset(x = underline)
                        .width(tabWidth)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
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
                                    if (sessions.size >= 2) {
                                        Spacer(Modifier.height(8.dp))
                                        val lastTop = sessions.last().second.sets.maxOfOrNull { it.kg ?: 0.0 } ?: 0.0
                                        val bestTop = bestSet ?: 0.0
                                        val delta = lastTop - bestTop
                                        val pct = if (bestTop > 0) (delta / bestTop * 100).toInt() else 0
                                        Text(
                                            L10n.s("Last session vs best", "Dernière séance vs record") + " : " +
                                                (if (delta >= 0) "+" else "") + "${Calc.fmtKg(delta, unit)} kg ($pct%)",
                                            color = if (delta < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
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
                        val steps = com.hevyclone.app.data.L10nData.steps(name)
                        if (steps.isNotEmpty()) {
                            AppCard {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        L10n.s("How to perform", "Comment réaliser l'exercice"),
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    steps.forEachIndexed { si, step ->
                                        Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                                            Box(
                                                Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    "${si + 1}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Text(step, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            Column(Modifier.padding(14.dp)) {
                                val cues = com.hevyclone.app.data.L10nData.cues(def?.muscle ?: "")
                                if (cues.isNotEmpty()) {
                                    Text(
                                        L10n.s("TIPS", "CONSEILS"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    cues.forEachIndexed { ci, cue ->
                                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                                            Text(
                                                "•",
                                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                                modifier = Modifier.width(14.dp),
                                            )
                                            Text(cue, fontSize = 14.sp, lineHeight = 20.sp)
                                        }
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

@Composable
private fun ExerciseRow(e: com.hevyclone.app.data.ExerciseDef) {
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

// ============================ plate calculator ============================

/** Gym plate colors by size — the familiar red 25 / blue 20 / yellow 15 / green 10 scheme. */
private fun plateColor(kg: Double): Color = when (kg) {
    45.0, 25.0 -> Color(0xFFD64545)
    35.0, 15.0 -> Color(0xFFE8B931)
    20.0 -> Color(0xFF3F7BD8)
    10.0 -> Color(0xFF3FA35C)
    else -> Color(0xFF9AA3AB)
}

/**
 * Hevy-style plate calculator bottom sheet: target weight + bar choice →
 * greedy breakdown per side, drawn on a bar with real plate proportions.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlateCalcSheet(exName: String, onClose: () -> Unit) {
    val unit = Repo.settings.unit
    val isLb = unit == "lb"
    val pr = Repo.prFor(exName)
    var target by remember {
        mutableStateOf(pr?.weight?.let { Calc.fmtKg(it, unit) } ?: "")
    }
    var barIdx by remember { mutableStateOf(0) }
    val bars = if (isLb) listOf(45.0, 35.0, 25.0, 0.0) else listOf(20.0, 15.0, 10.0, 0.0)
    val barLabel = { b: Double -> if (b == 0.0) L10n.s("No bar", "Sans barre") else Calc.trimNum(b) + Calc.unitLabel(unit) }
    val targetVal = target.replace(',', '.').toDoubleOrNull()
    val plates = targetVal?.let { Calc.platesForSide(it, bars[barIdx], unit) } ?: emptyList()
    val perSide = plates.sumOf { it.first * it.second }
    val reached = if (targetVal != null) bars[barIdx] + 2 * perSide else 0.0

    ModalBottomSheet(onDismissRequest = onClose, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                L10n.s("Plate calculator", "Calculateur de disques"),
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(exName(exName), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) },
                label = { Text(L10n.s("Target weight", "Poids ciblé") + " (" + Calc.unitLabel(unit) + ")") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(L10n.s("Bar", "Barre"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bars.forEachIndexed { i, b ->
                    Text(
                        barLabel(b),
                        color = if (barIdx == i) Color.Black else MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (barIdx == i) MaterialTheme.colorScheme.primary else C.Card2)
                            .clickable { barIdx = i }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            if (targetVal == null || targetVal <= 0.0) {
                Text(
                    L10n.s("Enter a target weight.", "Saisis un poids ciblé."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp,
                )
            } else {
                PlateBarCanvas(plates, Modifier.fillMaxWidth().height(110.dp))
                Spacer(Modifier.height(12.dp))
                if (plates.isEmpty()) {
                    Text(
                        L10n.s("Empty bar.", "Barre à vide."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp,
                    )
                } else {
                    Text(
                        L10n.s("Per side", "Par côté") + " : " +
                            plates.joinToString(" · ") { (pl, n) -> "$n×${Calc.trimNum(pl)}" } +
                            "  —  " + L10n.s("Total", "Total") + " : ${Calc.trimNum(reached)}${Calc.unitLabel(unit)}",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    if (kotlin.math.abs(reached - targetVal) > 0.001) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            L10n.s("Closest load achievable with these plates.", "Charge la plus proche atteignable avec ces disques."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Side view of a loaded bar: gray bar with proportional plates on both ends. */
@Composable
private fun PlateBarCanvas(plates: List<Pair<Double, Int>>, modifier: Modifier = Modifier) {
    val maxPl = plates.maxOfOrNull { it.first } ?: 1.0
    val ordered = plates.sortedByDescending { it.first }
    Canvas(modifier) {
        val cy = size.height / 2
        val barH = 7.dp.toPx()
        val sleeve = size.width * 0.30f
        val cx = size.width / 2
        // bar
        drawRoundRect(
            color = Color(0xFF6E767E),
            topLeft = androidx.compose.ui.geometry.Offset(cx - sleeve - 14.dp.toPx(), cy - barH / 2),
            size = androidx.compose.ui.geometry.Size(2 * sleeve + 28.dp.toPx(), barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2),
        )
        var side = -1
        while (side <= 1) {
            var x = cx + side * (sleeve + 10.dp.toPx())
            val dir = side
            ordered.forEach { (pl, n) ->
                val w = (6.dp.toPx() + 3.dp.toPx() * (pl / maxPl).toFloat()).coerceAtMost(16.dp.toPx())
                val h = (26.dp.toPx() + 62.dp.toPx() * (pl / maxPl).toFloat()).coerceAtMost(size.height - 6.dp.toPx())
                repeat(n) {
                    drawRoundRect(
                        color = plateColor(pl),
                        topLeft = androidx.compose.ui.geometry.Offset(x - w / 2, cy - h / 2),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2),
                    )
                    x += dir * (w + 3.dp.toPx())
                }
            }
            side += 2
        }
    }
}


// ============================ create custom exercise ============================

/** Creates a user-defined exercise: name + muscle group + equipment, registered
 *  into the global catalog so search, icons and stats all work. */
@Composable
fun CreateExerciseDialog(initialName: String, onCreated: (String) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var muscle by remember { mutableStateOf("Chest") }
    var equip by remember { mutableStateOf("Barbell") }
    var pickMuscle by remember { mutableStateOf(false) }
    var pickEquip by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val equips = listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Other")

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(L10n.s("New exercise", "Nouvel exercice"), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(48) },
                    label = { Text(L10n.s("Name", "Nom")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Card2)
                        .clickable { pickMuscle = true }.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(L10n.s("Muscle group", "Groupe musculaire"), fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                    Text(muscleName(muscle), color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Card2)
                        .clickable { pickEquip = true }.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(L10n.s("Equipment", "Équipement"), fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                    Text(equipName(equip), color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cx = Repo.addCustom(name, muscle, equip)
                if (cx == null) toast(ctx, L10n.s("This exercise already exists", "Cet exercice existe déjà"))
                else { toast(ctx, L10n.s("Exercise created", "Exercice créé")); onCreated(cx.name) }
            }) { Text(L10n.s("Create", "Créer"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(L10n.s("Cancel", "Annuler"), color = C.Mut) } },
    )
    if (pickMuscle) {
        ListPickDialog(L10n.s("Muscle group", "Groupe musculaire"), com.hevyclone.app.data.MUSCLES.map { it to muscleName(it) }) {
            muscle = it; pickMuscle = false
        }
    }
    if (pickEquip) {
        ListPickDialog(L10n.s("Equipment", "Équipement"), equips.map { it to equipName(it) }) {
            equip = it; pickEquip = false
        }
    }
}

@Composable
private fun ListPickDialog(title: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                items(options) { (value, label) ->
                    Text(
                        label,
                        fontSize = 14.5.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(value) }
                            .padding(horizontal = 8.dp, vertical = 11.dp),
                    )
                }
            }
        },
        confirmButton = {},
    )
}
