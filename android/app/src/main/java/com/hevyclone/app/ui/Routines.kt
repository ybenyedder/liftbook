@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.Routine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TrainingScreen() {
    val ctx = LocalContext.current
    val rev = Repo.rev
    val routines = Repo.routines
    var expanded by remember { mutableStateOf(true) }
    var sortMode by remember { mutableStateOf(0) } // 0=Personnalisé 1=A→Z 2=Dernière utilisée 3=Création
    var view by remember { mutableStateOf(0) }      // 0=Routines 1=Calendrier (comme Hevy ▾)
    var viewMenu by remember { mutableStateOf(false) }
    val sortedRoutines = remember(rev, sortMode) {
        when (sortMode) {
            0 -> routines.toList() // Repo order == manual pos order
            1 -> routines.sortedBy { exName(it.name).lowercase() }
            2 -> routines.sortedByDescending { Repo.routineLastPerformed(it) ?: 0L }
            else -> routines.sortedByDescending { it.id }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dd = rememberDragDropState(listState, scope) { from, to ->
        if (from < Repo.routines.size && to < Repo.routines.size) Repo.moveRoutine(from, to)
    }
    dd.dragEnabled = sortMode == 0

    // A stale draft (app killed mid-workout) used to block every new start with a
    // toast only — offer resume / discard-and-restart instead.
    var busyDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun guard(block: () -> Unit) {
        if (Repo.draft != null) { pendingAction = block; busyDialog = true } else block()
    }
    fun startFresh(block: () -> Unit) = guard {
        RestTimer.clear()
        Repo.discardDraft()
        block()
    }

    Column(Modifier.fillMaxSize()) {
        // Header with view selector (Routines / Calendrier), like Hevy's "Training ▾"
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(L10n.s("Training", "Entraînement", "Entrenamiento", "Training"), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(22.dp)
                            .graphicsLayer { rotationZ = if (viewMenu) 180f else 0f },
                    )
                }
                // m3 1.3 menus read their container from the color scheme — force true-dark (no blue-gray)
                androidx.compose.material3.MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surfaceContainer = C.Card,
                        surfaceContainerHigh = C.Card,
                        surfaceContainerHighest = C.Card,
                    ),
                ) {
                DropdownMenu(expanded = viewMenu, onDismissRequest = { viewMenu = false }) {
                    listOf(
                        L10n.s("Training", "Entraînement", "Entrenamiento", "Training") to 0,
                        L10n.s("Calendar", "Calendrier", "Calendario", "Kalender") to 1,
                    ).forEach { (label, idx) ->
                        DropdownMenuItem(
                            modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 200.dp),
                            text = {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        label,
                                        fontSize = 17.sp,
                                        color = if (view == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = if (view == idx) FontWeight.Bold else FontWeight.Medium,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (view == idx) {
                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            onClick = { viewMenu = false; view = idx },
                        )
                    }
                }
                }
            }
        }
        if (view == 1) {
            TrainingCalendar()
            return@Column
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
                    startFresh {
                        Repo.startWorkout(null)
                        Nav.push(Screen.Logger)
                    }
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
        // Routines header
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.s("Routines", "Routines", "Rutinas", "Routinen"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            var sortMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Rounded.Sort, null) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(
                        L10n.s("Custom order", "Ordre personnalisé", "Orden personalizado", "Eigene Reihenfolge"),
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
                startFresh {
                    Repo.startRoutine(null)
                    Nav.push(Screen.Logger)
                }
            }) { Icon(Icons.Rounded.CreateNewFolder, null) }
        }
        // Nouv. Routine / Explorer buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(L10n.s("New routine", "Nouvelle routine", "Nueva rutina", "Neue Routine"), Icons.Rounded.CreateNewFolder, Modifier.weight(1f)) {
                startFresh {
                    Repo.startRoutine(null)
                    Nav.push(Screen.Logger)
                }
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
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            if (expanded) {
                if (routines.isEmpty()) item { EmptyState(L10n.s("No routines yet.\nTap “New routine” to create one.", "Aucune routine.\nTouche « Nouvelle routine » pour en créer une.")) }
                else items(sortedRoutines.size, key = { sortedRoutines[it].id }) { i ->
                    DraggableItem(dd, i) { dragging ->
                        Box(if (dragging) Modifier else Modifier.animateItemPlacement()) {
                            RoutineCard(sortedRoutines[i], dragging) {
                                startFresh {
                                    Repo.startWorkout(sortedRoutines[i].id)
                                    Nav.push(Screen.Logger)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (busyDialog) {
        WorkoutInProgressDialog(
            onDismiss = { busyDialog = false; pendingAction = null },
            onResume = { busyDialog = false; pendingAction = null; Nav.push(Screen.Logger) },
            onRestart = { busyDialog = false; pendingAction?.invoke() },
        )
    }
}

// ---------------- drag & drop (long press to reorder routines) ----------------

/** Long-press drag reordering for a LazyColumn — swaps items live and auto-scrolls at the edges. */
class DragDropState(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var dragEnabled: Boolean = true
    private var draggedOffsetY by mutableFloatStateOf(0f)
    val draggingItemOffsetY: Float get() = draggedOffsetY

    fun onDragStart(index: Int) {
        if (!dragEnabled) return
        draggingItemIndex = index
        draggedOffsetY = 0f
    }

    fun onDragInterrupted() {
        draggingItemIndex = null
        draggedOffsetY = 0f
    }

    fun onDrag(amount: Offset) {
        val current = draggingItemIndex ?: return
        draggedOffsetY += amount.y
        val info = listState.layoutInfo
        val currentInfo = info.visibleItemsInfo.firstOrNull { it.index == current } ?: return
        val startOffset = currentInfo.offset + draggedOffsetY
        val endOffset = startOffset + currentInfo.size
        val middle = startOffset + (endOffset - startOffset) / 2f
        // swap only when the dragged middle crosses a neighbour's midpoint (precise, Hevy-like)
        val target = info.visibleItemsInfo
            .filter { it.index != current }
            .firstOrNull { item ->
                val itemMid = item.offset + item.size / 2f
                if (item.index < current) middle < itemMid - 2f else middle > itemMid + 2f
            }
        if (target != null) {
            if (current == listState.firstVisibleItemIndex || target.index == listState.firstVisibleItemIndex) {
                scope.launch { listState.scrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            }
            val movingDown = target.index > current
            onMove(current, target.index)
            // keep the card glued to the finger across the slot change
            draggedOffsetY += if (movingDown) -target.size.toFloat() else currentInfo.size.toFloat()
            draggingItemIndex = target.index
        } else {
            // gentle capped auto-scroll when dragging past the visible bounds
            val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return
            val bottom = lastVisible.offset + lastVisible.size
            val overshootDown = endOffset - bottom
            if (overshootDown > 10 && current < info.totalItemsCount - 2) {
                listState.dispatchRawDelta(overshootDown.coerceAtMost(14f))
            } else {
                val first = info.visibleItemsInfo.first()
                val overshootUp = first.offset - startOffset
                if (overshootUp > 10 && current > 0) {
                    listState.dispatchRawDelta((-overshootUp).coerceAtMost(-14f))
                }
            }
        }
    }
}

@Composable
fun rememberDragDropState(listState: LazyListState, scope: CoroutineScope, onMove: (Int, Int) -> Unit): DragDropState =
    remember(listState) { DragDropState(listState, scope, onMove) }

/** Item wrapper: long-press picks the card up (lift + follow finger), release drops it. */
@Composable
fun LazyItemScope.DraggableItem(dd: DragDropState, index: Int, content: @Composable (Boolean) -> Unit) {
    val dragging = dd.draggingItemIndex == index
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                if (dragging) {
                    translationY = dd.draggingItemOffsetY
                    scaleX = 1.02f
                    scaleY = 1.02f
                    shadowElevation = 28f
                }
            }
            .pointerInput(dd.dragEnabled) {
                if (!dd.dragEnabled) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDrag = { change, amount ->
                        change.consume()
                        dd.onDrag(amount)
                    },
                    onDragStart = {
                        dd.onDragStart(index)
                        if (dd.draggingItemIndex != null) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    onDragEnd = { dd.onDragInterrupted() },
                    onDragCancel = { dd.onDragInterrupted() },
                )
            }
    ) { content(dragging) }
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
private fun RoutineCard(r: Routine, dragging: Boolean = false, onStart: () -> Unit = {}) {
    val ctx = LocalContext.current
    var cardMenu by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    AppCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (dragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(14.dp))
                .clickable { onStart() }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exName(r.name), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box {
                    IconButton(onClick = { cardMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = cardMenu, onDismissRequest = { cardMenu = false }) {
                        DropdownMenuItem(text = { Text(L10n.s("View routine", "Voir la routine")) }, leadingIcon = { Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp)) }, onClick = {
                            cardMenu = false
                            Nav.push(Screen.RoutineDetail(r.id))
                        })
                        DropdownMenuItem(text = { Text(L10n.s("Rename routine", "Renommer la routine")) }, leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp)) }, onClick = {
                            cardMenu = false; renameTarget = r.id
                        })
                        DropdownMenuItem(text = { Text(L10n.s("Duplicate routine", "Dupliquer la routine")) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)) }, onClick = {
                            cardMenu = false
                            Repo.duplicateRoutine(r.id)
                        })
                        DropdownMenuItem(text = { Text(L10n.s("Delete routine", "Supprimer la routine"), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }, onClick = {
                            cardMenu = false
                            confirmDel = true
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (r.exercises.isEmpty()) {
                Text(
                    L10n.s("No exercises yet", "Aucun exercice pour l'instant"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            } else {
                // Hevy-style exercise list: name + set count, "see N more" beyond 4
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    r.exercises.take(4).forEach { ex ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                exName(ex.name), fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${ex.sets.size} " + if (ex.sets.size > 1) L10n.s("series", "séries") else L10n.s("series", "série"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                            )
                        }
                    }
                }
                if (r.exercises.size > 4) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        seeMoreExercises(r.exercises.size - 4),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    )
                }
            }
        }
    }
    renameTarget?.let { rid ->
        var newName by remember(rid) { mutableStateOf(r.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(L10n.s("Rename routine", "Renommer la routine"), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newName, onValueChange = { newName = it.take(40) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Repo.renameRoutine(rid, newName)
                    renameTarget = null
                }) { Text(L10n.s("Save", "Enregistrer")) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
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
    var rMenu by remember { mutableStateOf(false) }
    var confirmDelR by remember { mutableStateOf(false) }
    var renameR by remember { mutableStateOf(false) }
    var busyDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun guard(block: () -> Unit) {
        if (Repo.draft != null) { pendingAction = block; busyDialog = true } else block()
    }
    fun startFresh(block: () -> Unit) = guard {
        RestTimer.clear()
        Repo.discardDraft()
        block()
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.Rounded.ArrowBack, null) }
            Text(L10n.s("Routine", "Routine", "Rutina", "Routine"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { shareRoutine(ctx, r) }) { Icon(Icons.Rounded.IosShare, null) }
            Box {
                IconButton(onClick = { rMenu = true }) { Icon(Icons.Rounded.MoreHoriz, null) }
                DropdownMenu(expanded = rMenu, onDismissRequest = { rMenu = false }) {
                    DropdownMenuItem(text = { Text(L10n.s("Edit routine", "Modifier la routine")) }, leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp)) }, onClick = {
                        rMenu = false
                        startFresh {
                            Repo.startRoutine(r.id)
                            Nav.push(Screen.Logger)
                        }
                    })
                    DropdownMenuItem(text = { Text(L10n.s("Rename routine", "Renommer la routine")) }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null, modifier = Modifier.size(16.dp)) }, onClick = {
                        rMenu = false; renameR = true
                    })
                    DropdownMenuItem(text = { Text(L10n.s("Duplicate routine", "Dupliquer la routine")) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)) }, onClick = {
                        rMenu = false
                        Repo.duplicateRoutine(r.id)
                        toast(ctx, L10n.s("Routine duplicated", "Routine dupliquée"))
                    })
                    DropdownMenuItem(text = { Text(L10n.s("Delete routine", "Supprimer la routine"), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }, onClick = {
                        rMenu = false; confirmDelR = true
                    })
                }
            }
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
                        startFresh {
                            Repo.startWorkout(r.id)
                            Nav.push(Screen.Logger)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            item {
                val sessions = remember(rev, r.id) { sessionsFor(r) }
                // no history yet → no empty chart block (avoids the blank gap)
                if (sessions.isNotEmpty()) {
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
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L10n.s("Exercises", "Exercices", "Ejercicios", "Übungen"), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        L10n.s("Edit Routine", "Modifier la Routine"),
                        color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            startFresh {
                                Repo.startRoutine(r.id)
                                Nav.push(Screen.Logger)
                            }
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
                        val effective = ex.restSec ?: Repo.settings.restSec
                        val m = effective / 60
                        val s = effective % 60
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

    if (busyDialog) {
        WorkoutInProgressDialog(
            onDismiss = { busyDialog = false; pendingAction = null },
            onResume = { busyDialog = false; pendingAction = null; Nav.push(Screen.Logger) },
            onRestart = { busyDialog = false; pendingAction?.invoke() },
        )
    }
    if (renameR) {
        var newName by remember { mutableStateOf(r.name) }
        AlertDialog(
            onDismissRequest = { renameR = false },
            title = { Text(L10n.s("Rename routine", "Renommer la routine"), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newName, onValueChange = { newName = it.take(40) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Repo.renameRoutine(r.id, newName)
                    renameR = false
                }) { Text(L10n.s("Save", "Enregistrer")) }
            },
            dismissButton = { TextButton(onClick = { renameR = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
    if (confirmDelR) {
        AlertDialog(
            onDismissRequest = { confirmDelR = false },
            title = { Text(L10n.s("Delete routine?", "Supprimer la routine ?"), fontWeight = FontWeight.ExtraBold) },
            text = { Text(L10n.s("This routine will be removed from your list.", "Cette routine sera retirée de ta liste.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelR = false
                    Repo.deleteRoutine(r.id)
                    Nav.pop()
                }) { Text(L10n.s("Delete", "Supprimer"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelR = false }) { Text(L10n.s("Cancel", "Annuler")) } },
        )
    }
}

/** Real Android share sheet with the routine summary. */
private fun shareRoutine(ctx: android.content.Context, r: Routine) {
    val sb = StringBuilder()
    sb.appendLine(r.name)
    sb.appendLine()
    for (ex in r.exercises) {
        sb.appendLine(exName(ex.name) + " (" + ex.sets.size + " séries)")
        for ((i, st) in ex.sets.withIndex()) {
            val kgLabel = st.kg?.let { com.hevyclone.app.data.Calc.fmtKg(it, Repo.settings.unit) + " kg" } ?: ""
            sb.appendLine("  " + (i + 1) + ". " + kgLabel + " × " + (st.reps ?: "—"))
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
    }
    ctx.startActivity(android.content.Intent.createChooser(intent, "Partager la routine"))
}

private fun sessionsFor(r: Routine): List<com.hevyclone.app.data.Workout> =
    Repo.workouts.sortedBy { it.startedAt }
        .filter { w -> w.exercises.any { e -> r.exercises.any { it.name == e.name } } }
        .takeLast(12)

private fun volTargets(r: Routine): Double =
    r.exercises.sumOf { e -> e.sets.sumOf { (it.kg ?: 0.0) * (it.reps ?: 0) } }
