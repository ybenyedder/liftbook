package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.rounded.Link
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.ExEntry
import com.hevyclone.app.data.MUSCLES
import com.hevyclone.app.data.EXERCISES
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.SetEntry
import kotlinx.coroutines.delay

object RestTimer {
    var endAt by mutableStateOf(0L)
    var appContext: android.content.Context? = null
    private const val CHANNEL = "rest_timer"
    private const val NOTIF_ID = 4242
    private const val PREFS = "rest_timer"

    /** Restore a running timer after process death. Call once at app start. */
    fun restore(ctx: android.content.Context) {
        appContext = ctx
        val saved = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getLong("endAt", 0L)
        if (saved > System.currentTimeMillis()) {
            endAt = saved
            postNotification()
        } else if (saved > 0) {
            ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().remove("endAt").apply()
        }
    }

    private fun persist() {
        appContext?.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)?.edit()
            ?.putLong("endAt", if (endAt > 0) endAt else 0L)?.apply()
    }

    fun start(sec: Int) {
        endAt = System.currentTimeMillis() + sec * 1000L
        persist()
        postNotification()
    }

    fun clear() {
        endAt = 0
        persist()
        appContext?.let {
            runCatching {
                (it.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                    .cancel(NOTIF_ID)
            }
        }
    }

    private fun postNotification() {
        val ctx = appContext ?: run { android.util.Log.w("RestTimer", "no context"); return }
        runCatching {
            val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(
                android.app.NotificationChannel(CHANNEL, "Minuteur de repos", android.app.NotificationManager.IMPORTANCE_LOW)
            )
            val end = endAt
            val notif = android.app.Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Repos")
                .setContentText("Repos en cours")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(end)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            nm.notify(NOTIF_ID, notif)
        }.onFailure { android.util.Log.e("RestTimer", "notif failed", it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggerScreen() {
    val rev = Repo.rev
    val draft = Repo.draft
    val ctx = LocalContext.current
    if (draft == null) { Nav.toTab(Screen.HomeTab); return }
    val isWorkout = draft.mode == "workout"
    val unit = Repo.settings.unit

    var showPicker by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showNoSets by remember { mutableStateOf(false) }
    var showDeleteRoutine by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var nameText by remember(draft) { mutableStateOf(draft.name) }
    var dragIndex by remember { mutableStateOf(-1) }   // exercise being dragged

    fun moveExercise(from: Int, to: Int) {
        val list = draft.exercises
        if (from == to || from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        dragIndex = to
        Repo.touchPublic()
    }

    LaunchedEffect(RestTimer.endAt) {
        if (RestTimer.endAt > 0) {
            delay(kotlin.math.max(0L, RestTimer.endAt - System.currentTimeMillis()) + 4000L)
            if (RestTimer.endAt > 0 && System.currentTimeMillis() >= RestTimer.endAt + 3950) RestTimer.clear()
        }
    }

    fun hasData(): Boolean = draft.exercises.any { ex -> ex.sets.any { it.kg != null || it.reps != null } }
    fun anyDone(): Boolean = draft.exercises.any { ex -> ex.sets.any { it.done && (it.kg != null || it.reps != null) } }

    Column(Modifier.fillMaxSize().imePadding()) {
        // ---- top bar ----
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (hasData()) showDiscard = true
                else { Repo.discardDraft(); Nav.pop(); toast(ctx, L10n.s("Discarded", "Supprimée")) }
            }) { Icon(Icons.Rounded.ArrowBack, null) }
            if (isWorkout) {
                Column(Modifier.weight(1f)) {
                    Text(draft.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    DurationText(draft.startedAt)
                }
            } else {
                BasicTextField(
                    value = nameText,
                    onValueChange = { nameText = it; draft.name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                )
            }
            if (isWorkout) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { if (anyDone()) showFinish = true else showNoSets = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        L10n.s("FINISH", "TERMINER"),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreHoriz, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!isWorkout && draft.routineId != null) {
                        DropdownMenuItem(text = { Text("Delete routine", color = MaterialTheme.colorScheme.error) }, onClick = {
                            showMenu = false; showDeleteRoutine = true
                        })
                    }
                    if (isWorkout) {
                        DropdownMenuItem(
                            text = { Text(L10n.s("Workout notes", "Notes de la séance")) },
                            leadingIcon = { Icon(Icons.Rounded.Notes, null, modifier = Modifier.size(16.dp)) },
                            onClick = { showMenu = false; showNotesDialog = true },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (isWorkout) L10n.s("Discard workout", "Supprimer la séance") else L10n.s("Discard changes", "Ignorer les modifications"), color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; if (hasData()) showDiscard = true else { Repo.discardDraft(); Nav.pop(); toast(ctx, "Supprimée") } },
                    )
                }
            }
        }
        // ---- exercise cards ----
        LazyColumn(Modifier.weight(1f)) {
            if (draft.exercises.isEmpty()) {
                item {
                    EmptyState(
                        if (isWorkout) "Aucun exercice.\nTouche « Ajouter un Exercice » pour commencer."
                        else "Cette routine est vide.\nTouche « Ajouter un Exercice » pour la construire."
                    )
                }
            }
            items(draft.exercises.size, key = { "${draft.exercises[it].name}-$it" }) { ei ->
                ExCard(draft.exercises[ei], ei, isWorkout, unit, dragIndex == ei, ::moveExercise)
            }
            item(key = "addEx") {
                // Hevy-style full-width add button under the cards
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                        .clickable { showPicker = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(L10n.s("Add Exercise", "Ajouter un Exercice", "Añadir Ejercicio", "Übung hinzufügen"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item(key = "dockspace") { Spacer(Modifier.height(140.dp)) }
        }
        // ---- bottom dock ----
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (RestTimer.endAt > 0) {
                RestBar()
                Spacer(Modifier.height(10.dp))
            }
            if (!isWorkout) {
                PrimaryButton(
                    L10n.s("Save Routine", "Enregistrer la routine"),
                    onClick = {
                        val n = nameText.trim()
                        if (n.isEmpty()) { toast(ctx, L10n.s("Name your routine first", "Donne un nom à ta routine")); return@PrimaryButton }
                        if (draft.exercises.isEmpty()) { toast(ctx, L10n.s("Add at least one exercise", "Ajoute au moins un exercice")); return@PrimaryButton }
                        Repo.saveRoutine(n)
                        Nav.pop()
                        toast(ctx, L10n.s("Routine saved", "Routine enregistrée"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leading = { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(17.dp)) },
                )
            }
        }
    }

    // ---- pickers / dialogs ----
    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PickerContent(
                onClose = { showPicker = false },
                onPick = { name ->
                    Repo.addExToDraft(name)
                    showPicker = false
                    toast(ctx, "$name ajouté")
                },
            )
        }
    }
    if (showFinish) {
        ModalBottomSheet(
            onDismissRequest = { showFinish = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FinishSheet(
                draft = draft,
                onCancel = { showFinish = false },
                onDiscard = {
                    showFinish = false
                    RestTimer.clear()
                    Repo.discardDraft()
                    Nav.pop()
                    toast(ctx, "Supprimée")
                },
                onSave = {
                    val w = Repo.finishWorkout(nameText)
                    showFinish = false
                    RestTimer.clear()
                    Nav.toTab(Screen.HomeTab)
                    if (w != null && w.prs.isNotEmpty()) toast(ctx, "Saved · ${w.prs.size} PR${if (w.prs.size > 1) "s" else ""}!")
                    else toast(ctx, L10n.s("Workout saved", "Séance enregistrée"))
                },
            )
        }
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(if (isWorkout) L10n.s("Discard workout?", "Supprimer la séance ?") else L10n.s("Discard changes?", "Ignorer les modifications ?"), fontWeight = FontWeight.ExtraBold) },
            text = { Text(if (isWorkout) "Your logged sets from this session will be lost." else "Changes to this routine will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    RestTimer.clear()
                    Repo.discardDraft()
                    Nav.pop()
                    toast(ctx, "Supprimée")
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
    if (showNoSets) {
        AlertDialog(
            onDismissRequest = { showNoSets = false },
            title = { Text(L10n.s("No sets completed", "Aucune série terminée"), fontWeight = FontWeight.ExtraBold) },
            text = { Text("There is nothing to save yet. Discard this workout?") },
            confirmButton = {
                TextButton(onClick = {
                    showNoSets = false
                    RestTimer.clear()
                    Repo.discardDraft()
                    Nav.pop()
                    toast(ctx, "Supprimée")
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showNoSets = false }) { Text(L10n.s("Keep editing", "Continuer")) } },
        )
    }
    if (showNotesDialog) {
        var notesText by remember { mutableStateOf(draft.notes) }
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text(L10n.s("Workout notes", "Notes de la séance"), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(L10n.s("How did it feel?", "Comment ça s'est passé ?")) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    draft.notes = notesText
                    showNotesDialog = false
                }) { Text(L10n.s("Save", "Enregistrer")) }
            },
            dismissButton = { TextButton(onClick = { showNotesDialog = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
    if (showDeleteRoutine) {
        AlertDialog(
            onDismissRequest = { showDeleteRoutine = false },
            title = { Text("Delete routine?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This routine will be removed from your list.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteRoutine = false
                    Repo.draft?.routineId?.let { Repo.deleteRoutine(it) }
                    RestTimer.clear()
                    Repo.discardDraft()
                    Nav.pop()
                    toast(ctx, "Routine deleted")
                }) { Text(L10n.s("Discard", "Supprimer"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteRoutine = false }) { Text("Annuler") } },
        )
    }
}

// ---------------- exercise card ----------------

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ExCard(
    ex: ExEntry, ei: Int, isWorkout: Boolean, unit: String,
    isDragging: Boolean, moveExercise: (Int, Int) -> Unit,
) {
    var showNotes by remember { mutableStateOf(ex.notes.isNotEmpty()) }
    var menuOpen by remember { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current
    val isLast = Repo.draft?.exercises?.indexOfLast { it === ex }?.let { it >= (Repo.draft?.exercises?.size ?: 0) - 1 } ?: true
    var cardHeight by remember { mutableStateOf(1f) }
    val dragScale = animateFloatAsState(if (isDragging) 1.03f else 1f, label = "dragScale")
    AppCard(modifier = Modifier
        .graphicsLayer {
            scaleX = dragScale.value
            scaleY = dragScale.value
            alpha = if (isDragging) 0.92f else 1f
        }
        .onGloballyPositioned { cardHeight = it.size.height.toFloat() }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val steps = (dragAmount.y / cardHeight).toInt()
                    if (steps != 0) moveExercise(ei, ei + steps)
                },
                onDragEnd = { },
            )
        }
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { Nav.push(Screen.ExerciseDetail(ex.name)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ex.superset) {
                    Box(
                        Modifier
                            .size(width = 3.dp, height = 34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(exName(ex.name), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (ex.superset) L10n.s("SUPERSET", "SUPERSET")
                        else "${ex.sets.size} " + if (ex.sets.size > 1) L10n.s("series", "séries") else L10n.s("series", "série"),
                        color = if (ex.superset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp, fontWeight = if (ex.superset) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(L10n.s("Duplicate exercise", "Dupliquer l'exercice")) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuOpen = false
                                Repo.draft?.exercises?.let { list ->
                                    if (ei < list.size) {
                                        val copy = ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.sets.map { SetEntry(it.kg, it.reps, it.done) }.toMutableList())
                                        list.add(ei + 1, copy)
                                    }
                                }
                                Repo.touchPublic()
                            },
                        )
                        if (!isLast) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (ex.superset) L10n.s("Remove superset", "Retirer le superset")
                                        else L10n.s("Superset with next", "Superset avec le suivant")
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.Link, null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    menuOpen = false
                                    ex.superset = !ex.superset
                                    Repo.touchPublic()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(L10n.s("Delete exercise", "Supprimer l'exercice"), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuOpen = false
                                Repo.draft?.exercises?.let { list -> if (ei < list.size) list.removeAt(ei) }
                                Repo.touchPublic()
                            },
                        )
                    }
                }
            }
            if (isWorkout) {
                Row(Modifier.padding(horizontal = 12.dp)) {
                    Text(L10n.s("SET", "SÉRIE"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    Text(L10n.s("PREVIOUS", "PRÉCÉDENTE"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    Text(unit.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(L10n.s("REPS", "RÉPS"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(40.dp))
                }
            } else {
                Row(Modifier.padding(horizontal = 12.dp)) {
                    Text("SÉRIE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    Text(unit.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("RÉPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(40.dp))
                }
            }
            val prev = if (isWorkout) Repo.prevFor(ex.name) else null
            ex.sets.forEachIndexed { si, s ->
                SetRow(s, si, ei, isWorkout, unit, if (isWorkout) prev?.getOrNull(si) else null)
            }
            // add set
            Text(
                L10n.s("+ ADD SET", "+ AJOUTER UNE SÉRIE"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val last = ex.sets.lastOrNull()
                        ex.sets.add(SetEntry(last?.kg, last?.reps, done = false))
                        Repo.touchPublic()
                    }
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
            // notes
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showNotes = !showNotes }) {
                    Icon(Icons.Rounded.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Notes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    if (ex.notes.isNotEmpty()) {
                        Spacer(Modifier.width(5.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
            if (showNotes) {
                BasicTextField(
                    value = ex.notes,
                    onValueChange = { ex.notes = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp),
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SetRow(s: SetEntry, si: Int, ei: Int, isWorkout: Boolean, unit: String, prevText: String?) {
    var menuOpen by remember { mutableStateOf(false) }
    val rowView = androidx.compose.ui.platform.LocalView.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // set number chip — tap opens options (copy / delete)
        Box {
            Text(
                "${si + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { menuOpen = true }
                    .wrapContentHeight(Alignment.CenterVertically),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(L10n.s("Copy set", "Copier la série")) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(15.dp)) }, onClick = {
                    menuOpen = false
                    Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                        if (si < ex.sets.size) ex.sets.add(si + 1, SetEntry(s.kg, s.reps, done = false))
                    }
                    Repo.touchPublic()
                })
                DropdownMenuItem(text = { Text(L10n.s("Delete set", "Supprimer la série"), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(15.dp)) }, onClick = {
                    menuOpen = false
                    Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                        if (ex.sets.size == 1) Repo.draft?.exercises?.removeAt(ei) else ex.sets.removeAt(si)
                    }
                    Repo.touchPublic()
                })
            }
        }
        Spacer(Modifier.width(8.dp))
        if (isWorkout) {
            Text(
                prevText ?: "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f).padding(horizontal = 2.dp),
                textAlign = TextAlign.Center,
            )
        }
        SetField(
            init = Calc.fmtKg(s.kg, unit),
            hint = unit,
            onChange = { s.kg = Calc.toKg(it, unit) },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        SetField(
            init = s.reps?.toString() ?: "",
            hint = L10n.s("Reps", "Réps"),
            onChange = { v -> s.reps = v.filter { it.isDigit() }.take(4).toIntOrNull() },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Spacer(Modifier.width(5.dp))
        if (isWorkout) {
            // dedicated completion check — toggles done + starts rest timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (s.done) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (s.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
                    .clickable {
                        s.done = !s.done
                        rowView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        if (s.done) RestTimer.start(Repo.settings.restSec)
                    },
            ) {
                if (s.done) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            }
        } else {
            // routine editor: direct delete button
            IconButton(
                onClick = {
                    Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                        if (ex.sets.size == 1) Repo.draft?.exercises?.removeAt(ei) else ex.sets.removeAt(si)
                    }
                    Repo.touchPublic()
                },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SetField(init: String, hint: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(init) { mutableStateOf(init) }
    BasicTextField(
        value = text,
        onValueChange = { v -> text = v; onChange(v) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .wrapContentHeight(Alignment.CenterVertically),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                inner()
            }
        },
    )
}

// ---------------- rest bar ----------------

/** Elapsed-workout label; ticks once per second without recomposing the whole logger. */
@Composable
private fun DurationText(startedAt: Long?) {
    val now = produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }.value
    Text(
        "${Calc.fmtDur(maxOf(0, now - (startedAt ?: now)))}",
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RestBar() {
    val now = produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(250) }
    }.value
    val remainingMs = RestTimer.endAt - now
    val remSec = remainingMs / 1000.0
    val over = remSec <= 0
    val shown = if (over) 0 else remSec.toLong()
    val text = "${shown / 60}:${String.format("%02d", shown % 60)}"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(L10n.s("REST", "REPOS"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.weight(1f))
        listOf(-10000L to "−10s", 15000L to "+15s").forEach { (delta, label) ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { RestTimer.endAt += delta }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
            ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.width(7.dp))
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { RestTimer.clear() }
                .padding(8.dp),
        ) { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp)) }
    }
}

// ---------------- picker ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerContent(onClose: () -> Unit, onPick: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var mus by remember { mutableStateOf("All") }
    val filtered = EXERCISES
        .filter { (mus == "All" || it.muscle == mus) && com.hevyclone.app.data.L10nData.matches(q, it.name) }
        .sortedBy { exName(it.name) }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.86f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.s("Select exercises", "Choisir des exercices"), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(
                L10n.s("DONE", "OK"),
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        TextField(
            value = q, onValueChange = { q = it },
            placeholder = { Text(L10n.s("Search exercises", "Rechercher des exercices"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp)) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(autoCorrect = false),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf("All") + MUSCLES) { m -> Chip(m, mus == m, onClick = { mus = m }) }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            if (filtered.isEmpty()) item { EmptyState(L10n.s("No exercises found.", "Aucun exercice trouvé.")) }
            else items(filtered) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(e.name) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IllIcon(e.muscle, 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(exName(e.name), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
                        Text(muscleName(e.muscle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

// ---------------- finish summary ----------------

@Composable
private fun FinishSheet(draft: com.hevyclone.app.data.Draft, onCancel: () -> Unit, onDiscard: () -> Unit, onSave: () -> Unit) {
    val unit = Repo.settings.unit
    var name by remember { mutableStateOf(draft.name) }
    var vol = 0.0; var reps = 0; var sets = 0
    for (ex in draft.exercises) for (s in ex.sets) {
        if (!s.done || s.kg == null || s.reps == null) continue
        vol += s.kg!! * s.reps!!; reps += s.reps!!; sets++
    }
    val prs = mutableListOf<com.hevyclone.app.data.PrRec>()
    for (ex in draft.exercises) {
        val done = ex.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 && it.done }
        if (done.isEmpty()) continue
        val prev = Repo.prFor(ex.name)
        val pw = prev?.weight ?: 0.0
        val pe = prev?.e1rm ?: 0.0
        val bw = done.maxOf { it.kg!! }
        val be = done.maxOf { Calc.e1rm(it.kg!!, it.reps!!) }
        if (bw > pw) prs.add(com.hevyclone.app.data.PrRec(ex.name, "Weight", bw))
        if (be > pe) prs.add(com.hevyclone.app.data.PrRec(ex.name, "Est. 1RM", be))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp)) {
        Text(L10n.s("Workout Summary", "Résumé de la séance"), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        TextField(
            value = name, onValueChange = { name = it },
            singleLine = true,
            shape = RoundedCornerShape(11.dp),
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Metric(Calc.fmtDur(System.currentTimeMillis() - (draft.startedAt ?: System.currentTimeMillis())), L10n.s("Duration", "Durée"), tight = true) }
            Box(Modifier.weight(1f)) { Metric(Calc.fmtVol(vol, unit), "Volume", unit = unit, tight = true) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Metric("$sets", L10n.s("Sets", "Séries"), tight = true) }
            Box(Modifier.weight(1f)) { Metric("$reps", "Réps", tight = true) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (prs.isEmpty()) L10n.s("No new records this session", "Aucun nouveau record cette séance") else "${prs.size} new record${if (prs.size > 1) "s" else ""}",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
        )
        prs.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(exName(p.ex), fontSize = 13.sp)
                Text("${Calc.fmtKg(p.value, unit)} ${Calc.unitLabel(unit)} · ${p.kind}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            GhostButton(L10n.s("Discard", "Supprimer"), onClick = onDiscard, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            PrimaryButton(L10n.s("Save", "Enregistrer"), onClick = onSave, modifier = Modifier.weight(1f).height(44.dp))
        }
    }
}
