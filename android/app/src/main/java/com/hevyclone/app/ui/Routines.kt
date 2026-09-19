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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.Routine

@Composable
fun TrainingScreen() {
    val ctx = LocalContext.current
    val rev = Repo.rev
    val routines = Repo.routines
    var expanded by remember { mutableStateOf(true) }
    var sortMode by remember { mutableStateOf(0) } // 0=A→Z 1=Dernière utilisée 2=Création
    val sortedRoutines = remember(rev, sortMode) {
        when (sortMode) {
            0 -> routines.sortedBy { exName(it.name).lowercase() }
            1 -> routines.sortedByDescending { Repo.routineLastPerformed(it) ?: 0L }
            else -> routines.sortedByDescending { it.id }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(L10n.s("Training", "Entraînement", "Entrenamiento", "Training"), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp).size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        // Start an empty workout — bordered dark button
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable {
                    if (Repo.draft != null) { Nav.push(Screen.Logger); return@clickable }
                    Repo.startWorkout(null)
                    Nav.push(Screen.Logger)
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(L10n.s("Start an Empty Workout", "Démarrer un Entraînement Vide", "Iniciar un Entrenamiento Vacío", "Leeres Workout starten"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (Repo.workouts.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .clickable {
                        if (Repo.draft != null) { Nav.push(Screen.Logger); return@clickable }
                        Repo.startRepeatLast()
                        Nav.push(Screen.Logger)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    L10n.s("Repeat last workout", "Reprendre la dernière séance"),
                    color = MaterialTheme.colorScheme.primary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        // Routines header
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.s("Routines", "Routines", "Rutinas", "Routinen"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            var sortMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Rounded.Sort, null) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(
                        L10n.s("Name A→Z", "Nom A→Z"),
                        L10n.s("Last performed", "Dernière utilisée"),
                        L10n.s("Most recent", "Plus récente"),
                    ).forEachIndexed { i, label ->
                        DropdownMenuItem(
                            text = { Text(label, color = if (sortMode == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground) },
                            onClick = { sortMenu = false; sortMode = i },
                        )
                    }
                }
            }
            IconButton(onClick = {
                if (Repo.draft != null) { Nav.push(Screen.Logger); return@IconButton }
                Repo.startRoutine(null)
                Nav.push(Screen.Logger)
            }) { Icon(Icons.Rounded.CreateNewFolder, null) }
        }
        // Nouv. Routine / Explorer buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(L10n.s("New routine", "Nouvelle routine", "Nueva rutina", "Neue Routine"), Icons.Rounded.CreateNewFolder, Modifier.weight(1f)) {
                if (Repo.draft != null) { Nav.push(Screen.Logger); return@SecondaryButton }
                Repo.startRoutine(null)
                Nav.push(Screen.Logger)
            }
            SecondaryButton(L10n.s("Explore", "Explorer", "Explorar", "Entdecken"), Icons.Rounded.Search, Modifier.weight(1f)) {
                Nav.push(Screen.Exercises)
            }
        }
        // Mes routines (N) collapsible
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.ExpandMore, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                L10n.s("My routines (%1\$d)", "Mes routines (%1\$d)", "Mis rutinas (%1\$d)", "Meine Routinen (%1\$d)").format(routines.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (expanded) {
                if (routines.isEmpty()) item { EmptyState(L10n.s("No routines yet.\nTap “New routine” to create one.", "Aucune routine.\nTouche « Nouvelle routine » pour en créer une.")) }
                else items(sortedRoutines.size) { i ->
                    val r = sortedRoutines[i]
                    RoutineCard(r)
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SecondaryButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoutineCard(r: Routine) {
    val ctx = LocalContext.current
    var cardMenu by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    AppCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { Nav.push(Screen.RoutineDetail(r.id)) }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exName(r.name), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box {
                    IconButton(onClick = { cardMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = cardMenu, onDismissRequest = { cardMenu = false }) {
                        DropdownMenuItem(text = { Text(L10n.s("Edit routine", "Modifier la routine")) }, leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp)) }, onClick = {
                            cardMenu = false
                            if (Repo.draft != null) { toast(ctx, L10n.s("Finish the current workout first", "Termine d'abord la séance en cours")); return@DropdownMenuItem }
                            Repo.startRoutine(r.id)
                            Nav.push(Screen.Logger)
                        })
                        DropdownMenuItem(text = { Text(L10n.s("Delete routine", "Supprimer la routine"), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }, onClick = {
                            cardMenu = false
                            confirmDel = true
                        })
                    }
                }
            }
            Text(
                r.exercises.joinToString(", ") { exName(it.name) },
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 21.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (Repo.draft != null) { toast(ctx, L10n.s("Finish the current workout first", "Termine d'abord la séance en cours")); return@clickable }
                        Repo.startWorkout(r.id)
                        Nav.push(Screen.Logger)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(L10n.s("Start routine", "Commencer la routine", "Comenzar la rutina", "Routine starten"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (confirmDel) {
        AlertDialog(
            onDismissRequest = { confirmDel = false },
            title = { Text(L10n.s("Delete routine?", "Supprimer la routine ?"), fontWeight = FontWeight.ExtraBold) },
            text = { Text(L10n.s("This routine will be removed from your list.", "Cette routine sera retirée de ta liste.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDel = false
                    Repo.deleteRoutine(r.id)
                }) { Text(L10n.s("Delete", "Supprimer"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDel = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
}

// ---------------- routine detail ----------------

@Composable
fun RoutineDetailScreen(id: Long) {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val r = Repo.routineById(id)
    if (r == null) { Nav.pop(); return }
    var metric by remember { mutableStateOf(0) } // 0=Volume 1=Réps 2=Durée
    val unit = Repo.settings.unit

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(L10n.s("Routine", "Routine", "Rutina", "Routine"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { toast(ctx, L10n.s("Shared!", "Partagé !")) }) { Icon(Icons.Rounded.IosShare, null) }
            IconButton(onClick = { toast(ctx, L10n.s("Routine options", "Options de la routine")) }) { Icon(Icons.Rounded.MoreHoriz, null) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(r.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                Text(
                    L10n.s("Created by %1\$s", "Créée par %1\$s").format(Repo.settings.handle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 12.dp),
                )
            }
            item {
                PrimaryButton(
                    "Commencer la Routine",
                    onClick = {
                        if (Repo.draft != null) { toast(ctx, "Termine d'abord la séance en cours"); return@PrimaryButton }
                        Repo.startWorkout(r.id)
                        Nav.push(Screen.Logger)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            item {
                val sessions = remember(rev, r.id) { sessionsFor(r) }
                val series = remember(rev, r.id, metric) {
                    sessions.map { w ->
                        val v = when (metric) {
                            0 -> Calc.vol(w)
                            1 -> Calc.reps(w).toDouble()
                            else -> (w.endedAt - w.startedAt) / 60000.0
                        }
                        Calc.fmtDateShort(w.startedAt) to v
                    }.takeLast(10)
                }
                val lastDate = sessions.lastOrNull()?.let { Calc.fmtDateShort(it.startedAt) } ?: ""
                val total = when (metric) {
                    0 -> "${Calc.fmtVol(volTargets(r), unit)} kg"
                    1 -> "${r.exercises.sumOf { e -> e.sets.sumOf { it.reps ?: 0 } }} réps"
                    else -> "—"
                }
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                    Text(total, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(lastDate, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(L10n.s("3 months", "3 derniers mois"), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                LineChart(series, fmtLabel = { v ->
                    if (metric == 2) "${v.toInt()}m" else Calc.fmtVol(v, unit)
                })
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(L10n.s("Volume", "Volume"), L10n.s("Reps", "Réps"), L10n.s("Duration", "Durée")).forEachIndexed { i, label ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (metric == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { metric = i }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                color = if (metric == i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L10n.s("Exercises", "Exercices", "Ejercicios", "Übungen"), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        L10n.s("Edit Routine", "Modifier la Routine"),
                        color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            if (Repo.draft != null) { toast(ctx, "Termine d'abord la séance en cours"); return@clickable }
                            Repo.startRoutine(r.id)
                            Nav.push(Screen.Logger)
                        },
                    )
                }
            }
            items(r.exercises.size) { ei ->
                val ex = r.exercises[ei]
                Column(Modifier.padding(bottom = 18.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            IllIcon(ex.muscle, 34.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            exName(ex.name),
                            color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).clickable { Nav.push(Screen.ExerciseDetail(ex.name)) },
                        )
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        val m = Repo.settings.restSec / 60
                        val s = Repo.settings.restSec % 60
                        Text(
                            L10n.s("Rest Timer: %1\$s", "Minuteur de Repos: %1\$s").format("${if (m > 0) "${m}min " else ""}${s}s"),
                            color = MaterialTheme.colorScheme.primary, fontSize = 14.5.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        Text(L10n.s("SET", "SÉRIE"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("KG", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(L10n.s("REPS", "RÉPS"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                    ex.sets.forEachIndexed { si, s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (si % 2 == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text("${si + 1}", fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(if (s.kg != null) Calc.fmtKg(s.kg, unit) else "—", fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text("${s.reps ?: "—"}", fontSize = 15.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

private fun sessionsFor(r: Routine): List<com.hevyclone.app.data.Workout> =
    Repo.workouts.sortedBy { it.startedAt }
        .filter { w -> w.exercises.any { e -> r.exercises.any { it.name == e.name } } }
        .takeLast(12)

private fun volTargets(r: Routine): Double =
    r.exercises.sumOf { e -> e.sets.sumOf { (it.kg ?: 0.0) * (it.reps ?: 0) } }
