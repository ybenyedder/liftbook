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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.PrRec
import com.hevyclone.app.data.Repo

/**
 * Hevy-style post-workout recap shown after tapping TERMINER:
 * editable name, stats, records, per-exercise sets, notes, then the final save button.
 * The draft is kept alive until the bottom TERMINER is pressed (back arrow returns to the workout).
 */
@Composable
fun WorkoutSummaryScreen() {
    val ctx = LocalContext.current
    val draft = Repo.draft
    if (draft == null || draft.mode != "workout" || draft.startedAt == null) { Nav.pop(); return }
    val unit = Repo.settings.unit
    var name by remember(draft) { mutableStateOf(draft.name) }

    var vol = 0.0; var reps = 0; var sets = 0
    for (ex in draft.exercises) for (s in ex.sets) {
        if (!s.done || s.kg == null || s.reps == null) continue
        vol += s.kg!! * s.reps!!; reps += s.reps!!; sets++
    }
    // records are evaluated against history strictly before this session
    val prs = remember(draft) {
        val out = mutableListOf<PrRec>()
        for (ex in draft.exercises) {
            val done = ex.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 && it.done }
            if (done.isEmpty()) continue
            val prev = Repo.prFor(ex.name)
            val pw = prev?.weight ?: 0.0
            val pe = prev?.e1rm ?: 0.0
            val bw = done.maxOf { it.kg!! }
            val be = done.maxOf { Calc.e1rm(it.kg!!, it.reps!!) }
            if (bw > pw) out.add(PrRec(ex.name, "Weight", bw))
            if (be > pe) out.add(PrRec(ex.name, "Est. 1RM", be))
        }
        out
    }
    val muscles = remember(draft) { draft.exercises.map { it.muscle }.toSet() }
    val duration = Calc.fmtDur(System.currentTimeMillis() - draft.startedAt!!)

    var showSaveRoutine by remember { mutableStateOf(false) }
    fun doFinish() {
        val w = Repo.finishWorkout(name)
        RestTimer.clear()
        Nav.toTab(Screen.HomeTab)
        if (w != null && w.prs.isNotEmpty()) toast(ctx, L10n.s("Workout saved", "Séance enregistrée") + " · ${w.prs.size} ${L10n.s("PRs", "records")}!")
        else toast(ctx, L10n.s("Workout saved", "Séance enregistrée"))
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(
                L10n.s("Workout Summary", "Résumé de la séance"),
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }
        LazyColumn(Modifier.weight(1f)) {
            item {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryStat(duration, L10n.s("Duration", "Durée"), Modifier.weight(1f), accent = true)
                    SummaryStat("${Calc.fmtVol(vol, unit)}${Calc.unitLabel(unit)}", L10n.s("Volume", "Volume"), Modifier.weight(1f))
                    SummaryStat("$sets", L10n.s("Sets", "Séries"), Modifier.weight(1f))
                    BodyMap(front = true, muscles = muscles)
                    BodyMap(front = false, muscles = muscles)
                }
            }
            if (prs.isNotEmpty()) {
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = C.Gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            L10n.s("RECORDS", "RECORDS"),
                            color = C.Gold, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                items(prs) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F3B2C))
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = C.Gold, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(exName(p.ex), fontSize = 13.5.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${if (p.kind == "Weight") L10n.s("Heaviest Weight", "Plus Gros Poids") else L10n.s("Best Est. 1RM", "Meilleure Est. 1RM")} · ${Calc.fmtKg(p.value, unit)}${Calc.unitLabel(unit)}",
                            color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1,
                        )
                    }
                }
            }
            item {
                Text(
                    L10n.s("Exercises", "Exercices"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(draft.exercises.filter { ex -> ex.sets.any { it.done } }) { ex ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            exName(ex.name),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.5.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val doneSets = ex.sets.filter { it.done && (it.kg != null || it.reps != null) }
                    doneSets.forEachIndexed { i, s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (s.prW || s.prE) Color(0xFF1F3B2C) else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                            Text(
                                "${s.kg?.let { Calc.fmtKg(it, unit) + Calc.unitLabel(unit) } ?: "—"} × ${s.reps ?: "—"}",
                                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (s.prW || s.prE) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = C.Gold, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                }
            }
            if (draft.notes.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.s("Notes", "Notes"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(draft.notes, fontSize = 13.5.sp, lineHeight = 19.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        // final save — if the routine was structurally changed, offer to save it back (Hevy behaviour)
        PrimaryButton(
            L10n.s("TERMINER", "TERMINER"),
            onClick = { if (Repo.draftDiffersFromRoutine()) showSaveRoutine = true else doFinish() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        )
    }
    if (showSaveRoutine) {
        AlertDialog(
            onDismissRequest = { showSaveRoutine = false },
            title = { Text(L10n.s("Routine modified", "Routine modifiée"), fontWeight = FontWeight.ExtraBold) },
            text = { Text(L10n.s("Save these changes for your next sessions?", "Enregistrer les modifications pour les prochaines séances ?", "¿Guardar los cambios para las próximas sesiones?", "Änderungen für kommende Einheiten speichern?")) },
            confirmButton = {
                TextButton(onClick = { showSaveRoutine = false; Repo.updateRoutineFromDraft(); doFinish() }) {
                    Text(L10n.s("Save", "Enregistrer", "Guardar", "Speichern"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveRoutine = false; doFinish() }) {
                    Text(L10n.s("Skip", "Ignorer", "Descartar", "Verwerfen"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
        )
    }
}


