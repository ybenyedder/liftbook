package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.rounded.Link
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
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
import com.hevyclone.app.MainActivity
import com.hevyclone.app.R
import com.hevyclone.app.RestNotifService
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.ExEntry
import com.hevyclone.app.data.MUSCLES
import com.hevyclone.app.data.EXERCISES
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.SetEntry
import kotlinx.coroutines.delay

object WorkoutNotif {
    private const val CHANNEL = "workout_chrono"
    private const val NOTIF_ID = 4243

    fun post(startedAt: Long) {
        val ctx = RestTimer.appContext ?: return
        runCatching {
            val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(
                android.app.NotificationChannel(CHANNEL, "Chronomètre de séance", android.app.NotificationManager.IMPORTANCE_LOW)
            )
            // Hevy-style media card: app icon, "Entraînement" + running chronometer.
            // NB: Chronometer bases run on elapsedRealtime, not wall clock — offset by the
            // session's age or it displays a huge negative value.
            val rv = android.widget.RemoteViews(ctx.packageName, R.layout.notif_workout)
            rv.setImageViewResource(R.id.iv_app, R.drawable.notif_logo)
            rv.setChronometer(
                R.id.workout_chrono,
                android.os.SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startedAt),
                null, true,
            )
            val content = android.app.PendingIntent.getActivity(
                ctx, 0,
                android.content.Intent(ctx, MainActivity::class.java),
                android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            val notif = android.app.Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setColor(android.graphics.Color.BLACK)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(content)
                .setCustomContentView(rv)
                .setCustomBigContentView(rv)
                .build()
            nm.notify(NOTIF_ID, notif)
        }
    }

    fun cancel() {
        val ctx = RestTimer.appContext ?: return
        runCatching {
            (ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .cancel(NOTIF_ID)
        }
    }
}

object RestTimer {
    var endAt by mutableStateOf(0L)
    /** Total rest length (progress bar); maintained alongside endAt. */
    var totalMs by mutableStateOf(0L)
    /** Exercise currently resting — shown in the notification illustration. */
    var exName: String? = null
    var exMuscle: String? = null
    var appContext: android.content.Context? = null
    private const val PREFS = "rest_timer"

    private fun prefs(ctx: android.content.Context) =
        ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    /** Restore a running timer after process death. Call once at app start. */
    fun restore(ctx: android.content.Context) {
        appContext = ctx
        val p = prefs(ctx)
        val saved = p.getLong("endAt", 0L)
        if (saved > System.currentTimeMillis()) {
            endAt = saved
            totalMs = p.getLong("totalMs", 0L).takeIf { it > 0 }
                ?: ((saved - System.currentTimeMillis()) / 1000L) * 1000L
            startService(ctx, (totalMs / 1000L).toInt())
        } else if (saved > 0) {
            p.edit().remove("endAt").remove("total").remove("totalMs").apply()
        }
    }

    fun start(sec: Int) {
        endAt = System.currentTimeMillis() + sec * 1000L
        totalMs = sec * 1000L
        appContext?.let {
            prefs(it).edit().putLong("endAt", endAt).putInt("total", sec).putLong("totalMs", totalMs).apply()
            startService(it, sec)
        }
    }

    private fun startService(ctx: android.content.Context, totalSec: Int) {
        androidx.core.content.ContextCompat.startForegroundService(
            ctx,
            android.content.Intent(ctx, RestNotifService::class.java)
                .setAction(RestNotifService.ACTION_START)
                .putExtra(RestNotifService.EXTRA_TOTAL, totalSec),
        )
    }

    /** +15s from the notification button. */
    fun plus15() {
        if (endAt > 0) { endAt += 15_000L; totalMs += 15_000L; persistEnd() }
    }

    /** −15s from the notification button (never below 0.5 s left). */
    fun minus15() {
        if (endAt > 0) {
            endAt = maxOf(System.currentTimeMillis() + 500L, endAt - 15_000L)
            persistEnd()
        }
    }

    /** Jump straight to the finished state ("Passer"). */
    fun skip() {
        if (endAt > 0) { endAt = System.currentTimeMillis(); persistEnd() }
    }

    private fun persistEnd() {
        appContext?.let { prefs(it).edit().putLong("endAt", if (endAt > 0) endAt else 0L).apply() }
    }

    fun clear() {
        endAt = 0
        totalMs = 0
        exName = null
        exMuscle = null
        appContext?.let { ctx ->
            prefs(ctx).edit().remove("endAt").remove("total").remove("totalMs").apply()
            runCatching {
                ctx.stopService(android.content.Intent(ctx, RestNotifService::class.java))
                (ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                    .cancel(RestNotifService.NOTIF_ID)
            }
        }
    }

    /**
     * Called by RestNotifService once the finished state has been shown long enough:
     * resets the global state + prefs, but never starts nor stops the service itself
     * (the caller manages its own shutdown).
     */
    fun onFinishedByService() {
        endAt = 0
        totalMs = 0
        exName = null
        exMuscle = null
        appContext?.let { prefs(it).edit().remove("endAt").remove("total").remove("totalMs").apply() }
    }
}

/** One-shot "record battu" pill shown at the top of the workout screen (Hevy PR badge). */
object PrBadge {
    class Badge(val ex: String, val muscle: String, val kind: String, val value: String)
    var current by mutableStateOf<Badge?>(null)
    fun show(ex: String, muscle: String, kind: String, value: String) {
        current = Badge(ex, muscle, kind, value)
    }
}

@Composable
fun PrBadgeHost() {
    val b = PrBadge.current ?: return
    LaunchedEffect(b) {
        delay(4200)
        if (PrBadge.current === b) PrBadge.current = null
    }
    AnimatedVisibility(
        visible = PrBadge.current != null,
        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it * 2 } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(C.Card2)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExCircle(b.muscle, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(exName(b.ex), color = C.Text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${b.kind} - ${b.value}", color = C.Orange, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Rounded.Close, null,
                tint = C.Mut,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { PrBadge.current = null }
                    .padding(4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LoggerScreen() {
    val rev = Repo.rev
    val draft = Repo.draft
    val ctx = LocalContext.current
    if (draft == null) { Nav.toTab(Screen.HomeTab); return }
    val isWorkout = draft.mode == "workout"
    val unit = Repo.settings.unit

    var showPicker by remember { mutableStateOf(false) }
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

    LaunchedEffect(isWorkout, draft.startedAt) {
        if (isWorkout && draft.startedAt != null) WorkoutNotif.post(draft.startedAt!!)
        else WorkoutNotif.cancel()
    }

    fun hasData(): Boolean = draft.exercises.any { ex -> ex.sets.any { it.kg != null || it.reps != null } }
    fun anyDone(): Boolean = draft.exercises.any { ex -> ex.sets.any { it.done && (it.kg != null || it.reps != null) } }

    fun trySaveRoutine() {
        val n = nameText.trim()
        if (n.isEmpty()) { toast(ctx, L10n.s("Name your routine first", "Donne un nom à ta routine")); return }
        if (draft.exercises.isEmpty()) { toast(ctx, L10n.s("Add at least one exercise", "Ajoute au moins un exercice")); return }
        Repo.saveRoutine(n)
        Nav.pop()
        toast(ctx, L10n.s("Routine saved", "Routine enregistrée"))
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            // ---- top bar (Hevy: ∨ Entraînement | ⏱ | Terminer) ----
            if (isWorkout) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMenu = true }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(L10n.s("Training", "Entraînement", "Entrenamiento", "Training"), fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        // notes / discard live in the chevron menu, like Hevy's minimize menu
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(L10n.s("Workout notes", "Notes de la séance")) },
                                leadingIcon = { Icon(Icons.Rounded.Notes, null, modifier = Modifier.size(16.dp)) },
                                onClick = { showMenu = false; showNotesDialog = true },
                            )
                            DropdownMenuItem(
                                text = { Text(L10n.s("Discard workout", "Supprimer la séance"), color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; if (hasData()) showDiscard = true else { Repo.discardDraft(); Nav.pop(); toast(ctx, "Supprimée") } },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { if (anyDone()) Nav.push(Screen.WorkoutSummary) else showNoSets = true }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(
                            L10n.s("Finish", "Terminer"),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // ---- live stats (Hevy: label above value, no boxes) ----
                val tick = produceState(System.currentTimeMillis()) {
                    while (true) { value = System.currentTimeMillis(); delay(1000) }
                }.value
                var liveVol = 0.0; var liveSets = 0
                for (ex in draft.exercises) for (s in ex.sets) {
                    if (s.done && s.kg != null && s.reps != null) { liveVol += s.kg!! * s.reps!!; liveSets++ }
                }
                val muscles = remember(rev) { draft.exercises.map { it.muscle }.toSet() }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiveStat(Calc.fmtClock(maxOf(0, tick - (draft.startedAt ?: tick))), L10n.s("Duration", "Durée"), Modifier.weight(1f), accent = true)
                    LiveStat("${Calc.fmtVol(liveVol, unit)} ${Calc.unitLabel(unit)}", L10n.s("Volume", "Volume"), Modifier.weight(1f))
                    LiveStat("$liveSets", L10n.s("Sets", "Séries"), Modifier.weight(1f))
                    BodyMap(front = true, muscles = muscles)
                    Spacer(Modifier.width(2.dp))
                    BodyMap(front = false, muscles = muscles)
                }
                Spacer(Modifier.height(6.dp))
            } else {
                // ---- routine editor top bar (Hevy: Annuler | Créer une Routine | Enregistrer bleu) ----
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        L10n.s("Cancel", "Annuler"),
                        color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (hasData()) showDiscard = true
                                else { Repo.discardDraft(); Nav.pop() }
                            }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                    )
                    Text(
                        if (draft.routineId != null) L10n.s("Edit Routine", "Modifier la Routine")
                        else L10n.s("Create Routine", "Créer une Routine"),
                        fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { trySaveRoutine() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            L10n.s("Save", "Enregistrer"),
                            color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.MoreVert, null, modifier = Modifier.size(17.dp)) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (draft.routineId != null) {
                                DropdownMenuItem(text = { Text(L10n.s("Delete routine", "Supprimer la routine"), color = MaterialTheme.colorScheme.error) }, onClick = {
                                    showMenu = false; showDeleteRoutine = true
                                })
                            }
                            DropdownMenuItem(
                                text = { Text(L10n.s("Discard changes", "Ignorer les modifications"), color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; if (hasData()) showDiscard = true else { Repo.discardDraft(); Nav.pop(); toast(ctx, "Supprimée") } },
                            )
                        }
                    }
                }
                // routine title: plain text over a hairline, like Hevy
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    BasicTextField(
                        value = nameText,
                        onValueChange = { nameText = it; draft.name = it },
                        singleLine = true,
                        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Column {
                                Box(Modifier.height(34.dp), contentAlignment = Alignment.CenterStart) {
                                    if (nameText.isEmpty()) {
                                        Text(L10n.s("Routine title", "Titre de la routine"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    }
                                    inner()
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            // ---- exercise sections (Hevy: directly on black, no card boxes) ----
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
                    Box(Modifier.animateItemPlacement()) {
                        ExCard(draft.exercises[ei], ei, isWorkout, unit, dragIndex == ei, ::moveExercise)
                    }
                }
                item(key = "addEx") {
                    // Hevy-style blue add button under the sections
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { showPicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(L10n.s("Add Exercise", "Ajouter un Exercice", "Añadir Ejercicio", "Übung hinzufügen"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                AnimatedVisibility(
                    visible = RestTimer.endAt > 0,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        RestBar()
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        // PR pill overlays the top of the workout screen
        if (isWorkout) {
            Box(Modifier.align(Alignment.TopCenter)) { PrBadgeHost() }
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

// ---------------- exercise section (Hevy: no card, directly on black) ----------------

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ExCard(
    ex: ExEntry, ei: Int, isWorkout: Boolean, unit: String,
    isDragging: Boolean, moveExercise: (Int, Int) -> Unit,
) {
    var showNotes by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRestDialog by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current
    val isLast = Repo.draft?.exercises?.indexOfLast { it === ex }?.let { it >= (Repo.draft?.exercises?.size ?: 0) - 1 } ?: true
    var cardHeight by remember { mutableStateOf(1f) }
    val dragScale = animateFloatAsState(if (isDragging) 1.03f else 1f, label = "dragScale")
    Column(
        Modifier
            .fillMaxWidth()
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
            .padding(vertical = 8.dp)
            .animateContentSize()
    ) {
        // title row: white illustration circle + blue title + ⋮
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
            ExCircle(ex.muscle, 46.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                exName(ex.name),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(L10n.s("Replace exercise", "Remplacer l'exercice")) },
                        leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(16.dp)) },
                        onClick = { menuOpen = false; showReplace = true },
                    )
                    DropdownMenuItem(
                        text = { Text(L10n.s("Duplicate exercise", "Dupliquer l'exercice")) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            menuOpen = false
                            Repo.draft?.exercises?.let { list ->
                                if (ei < list.size) {
                                    val copy = ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.done) }.toMutableList())
                                    list.add(ei + 1, copy)
                                }
                            }
                            Repo.touchPublic()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                L10n.s("Rest timer", "Minuteur de repos") +
                                    (ex.restSec?.let { " : ${it}s" } ?: "")
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(16.dp)) },
                        onClick = { menuOpen = false; showRestDialog = true },
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
        // notes placeholder / preview (Hevy position: under the title)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clickable { showNotes = true }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            if (ex.notes.isEmpty()) {
                Text(
                    L10n.s("Add notes here…", "Ajouter des notes ici…"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        ex.notes,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // blue rest line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showRestDialog = true }
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                L10n.s("Rest: %1\$s", "Repos: %1\$s").format(fmtRestLabel(ex.restSec ?: Repo.settings.restSec)),
                color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(8.dp))
        // column headers
        if (isWorkout) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.s("SET", "SÉRIE"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                Text(L10n.s("PREVIOUS", "PRÉCÉDENT"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1.15f), textAlign = TextAlign.Center)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(unit.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text(L10n.s("REPS", "RÉPS"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }
            }
        } else {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("SÉRIE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(unit.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text("RÉPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Box(Modifier.width(44.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        val prev = if (isWorkout) Repo.prevFor(ex.name) else null
        ex.sets.forEachIndexed { si, s ->
            SetRow(s, si, ei, isWorkout, unit, ex, if (isWorkout) prev?.getOrNull(si) else null, ex.restSec)
        }
        // add set — Hevy dark full-width button
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Card2)
                .clickable {
                    val last = ex.sets.lastOrNull()
                    ex.sets.add(SetEntry(last?.kg, last?.reps, done = false))
                    Repo.touchPublic()
                }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                L10n.s("Add Set", "Ajouter une Série"),
                fontWeight = FontWeight.Medium, fontSize = 15.sp,
            )
        }
        if (showReplace) {
            ModalBottomSheet(
                onDismissRequest = { showReplace = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                PickerContent(
                    onClose = { showReplace = false },
                    onPick = { newName ->
                        ex.name = newName
                        ex.muscle = com.hevyclone.app.data.EX[newName]?.muscle ?: ""
                        Repo.touchPublic()
                        showReplace = false
                    },
                )
            }
        }
        if (showRestDialog) {
            RestSheet(
                initialSec = ex.restSec ?: Repo.settings.restSec,
                onDismiss = { showRestDialog = false },
                onDone = { sec ->
                    ex.restSec = sec
                    Repo.touchPublic()
                    showRestDialog = false
                },
                onReset = {
                    ex.restSec = null
                    Repo.touchPublic()
                    showRestDialog = false
                },
            )
        }
        if (showNotes) {
            var exNotes by remember { mutableStateOf(ex.notes) }
            AlertDialog(
                onDismissRequest = { showNotes = false },
                title = { Text(exName(ex.name), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = exNotes,
                        onValueChange = { exNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(L10n.s("Exercise notes…", "Notes de l'exercice…")) },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        ex.notes = exNotes
                        Repo.touchPublic()
                        showNotes = false
                    }) { Text(L10n.s("Save", "Enregistrer")) }
                },
                dismissButton = { TextButton(onClick = { showNotes = false }) { Text(L10n.s("Cancel", "Annuler")) } },
            )
        }
    }
}

/**
 * PR evaluation at check-time, against history strictly before this workout.
 * Announces the badge once per new record; flags the set for the green row + recap.
 */
fun evaluatePr(ex: ExEntry, s: SetEntry) {
    val kg = s.kg ?: return
    val reps = s.reps ?: return
    if (kg <= 0 || reps <= 0) return
    val pr = Repo.prFor(ex.name)
    val bestW = pr?.weight ?: 0.0
    val bestE = pr?.e1rm ?: 0.0
    val e = Calc.e1rm(kg, reps)
    if (kg > bestW && !s.prW) {
        s.prW = true
        PrBadge.show(ex.name, ex.muscle, L10n.s("Heaviest Weight", "Plus Gros Poids"), "${Calc.fmtKg(kg, Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)}")
    } else if (e > bestE && !s.prE) {
        s.prE = true
        PrBadge.show(ex.name, ex.muscle, L10n.s("Best Est. 1RM", "Meilleure Est. 1RM"), "${Calc.fmtKg(e, Repo.settings.unit)} ${Calc.unitLabel(Repo.settings.unit)}")
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SetRow(
    s: SetEntry, si: Int, ei: Int, isWorkout: Boolean, unit: String,
    ex: ExEntry, prevText: String?, restOverride: Int? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val rowView = androidx.compose.ui.platform.LocalView.current
    val isPr = isWorkout && (s.prW || s.prE)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPr) C.GreenBg else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // set number chip (rounded square) — or medal on a PR row; tap opens copy/delete
        Box {
            if (isPr) {
                Icon(
                    Icons.Rounded.EmojiEvents, null, tint = C.Gold,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(9.dp)
                        .clickable { menuOpen = true },
                )
            } else {
                Text(
                    "${si + 1}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Card2)
                        .clickable { menuOpen = true }
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(L10n.s("Copy set", "Copier la série")) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(15.dp)) }, onClick = {
                    menuOpen = false
                    Repo.draft?.exercises?.getOrNull(ei)?.let { exx ->
                        if (si < exx.sets.size) exx.sets.add(si + 1, SetEntry(s.kg, s.reps, done = false))
                    }
                    Repo.touchPublic()
                })
                DropdownMenuItem(text = { Text(L10n.s("Delete set", "Supprimer la série"), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(15.dp)) }, onClick = {
                    menuOpen = false
                    Repo.draft?.exercises?.getOrNull(ei)?.let { exx ->
                        if (exx.sets.size == 1) Repo.draft?.exercises?.removeAt(ei) else exx.sets.removeAt(si)
                    }
                    Repo.touchPublic()
                })
            }
        }
        Spacer(Modifier.width(6.dp))
        if (isWorkout) {
            Text(
                prevText ?: "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.5.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.15f).padding(horizontal = 2.dp),
                textAlign = TextAlign.Center,
            )
        }
        SetField(
            init = Calc.fmtKg(s.kg, unit),
            hint = "-",
            onChange = { s.kg = Calc.toKg(it, unit) },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        SetField(
            init = s.reps?.toString() ?: "",
            hint = "-",
            onChange = { v -> s.reps = v.filter { it.isDigit() }.take(4).toIntOrNull() },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Spacer(Modifier.width(4.dp))
        if (isWorkout) {
            // plain gray check → green filled rounded square when done
            val checkScale by animateFloatAsState(
                if (s.done) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "checkScale",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        s.done = !s.done
                        rowView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        if (s.done) {
                            evaluatePr(ex, s)
                            RestTimer.exName = ex.name
                            RestTimer.exMuscle = ex.muscle
                            RestTimer.start(restOverride ?: Repo.settings.restSec)
                        }
                    },
            ) {
                if (s.done) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(C.Green),
                    )
                }
                Icon(
                    Icons.Rounded.Check, null,
                    tint = if (s.done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            val pop = if (s.done) 0.6f + 0.4f * checkScale else 1f
                            scaleX = pop; scaleY = pop; alpha = pop
                        },
                )
            }
        } else {
            // routine editor: direct delete button
            IconButton(
                onClick = {
                    Repo.draft?.exercises?.getOrNull(ei)?.let { exx ->
                        if (exx.sets.size == 1) Repo.draft?.exercises?.removeAt(ei) else exx.sets.removeAt(si)
                    }
                    Repo.touchPublic()
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(C.Card2)
            .border(1.dp, C.Line2, RoundedCornerShape(10.dp))
            .wrapContentHeight(Alignment.CenterVertically),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                inner()
            }
        },
    )
}

// ---------------- rest bar ----------------

/** "2min 0s" style rest label. */
fun fmtRestLabel(sec: Int): String =
    if (sec >= 60) "${sec / 60}min ${sec % 60}s" else "${sec}s"

/** Hevy live stat: gray label above the big value, no box. */
@Composable
private fun LiveStat(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
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

/** Hevy-style rest picker: scrollable 5-second steps + Terminé. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestSheet(
    initialSec: Int,
    onDismiss: () -> Unit,
    onDone: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val values = remember { (3..120).map { it * 5 } } // 15s → 10min
    var sel by remember { mutableStateOf(initialSec.coerceIn(15, 600)) }
    val selIndex = values.indexOf(sel).coerceAtLeast(0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = maxOf(0, selIndex - 4),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                L10n.s("Rest Timer", "Minuteur de Repos"),
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                L10n.s("Rest: %1\$s", "Repos: %1\$s").format(fmtRestLabel(sel)),
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            ) {
                items(values.size) { i ->
                    val v = values[i]
                    val selected = v == sel
                    Text(
                        fmtRestLabel(v),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = if (selected) 18.sp else 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .clickable { sel = v }
                            .padding(vertical = 9.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = onReset) {
                        Text(L10n.s("Use default", "Par défaut"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.weight(2f)) {
                    PrimaryButton(
                        L10n.s("Done", "Terminé"),
                        onClick = { onDone(sel) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

/** Hevy rest bar: blue line on top, −15 / big 01:59 / +15, blue Passer button. */
@Composable
private fun RestBar() {
    val now = produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(250) }
    }.value
    val remainingMs = (RestTimer.endAt - now).coerceAtLeast(0)
    val over = RestTimer.endAt - now <= 0
    val shown = (remainingMs / 1000L)
    val text = String.format("%02d:%02d", shown / 60, shown % 60)
    val fraction = if (RestTimer.totalMs > 0) (remainingMs.toFloat() / RestTimer.totalMs).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // progress line on top
        Box(Modifier.fillMaxWidth().height(4.dp).background(C.Line)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Card2)
                    .clickable { RestTimer.minus15() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) { Text("−15", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.weight(1f))
            Text(
                text,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Card2)
                    .clickable { RestTimer.plus15() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) { Text("+15", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { RestTimer.clear() }
                    .padding(horizontal = 18.dp, vertical = 11.dp),
            ) {
                Text(L10n.s("Skip", "Passer"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
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
            items(listOf("All") + MUSCLES) { m ->
                Chip(if (m == "All") L10n.s("All", "Tous") else muscleName(m), mus == m, onClick = { mus = m })
            }
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
                        Text("${muscleName(e.muscle)} · ${equipName(e.equip)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
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
