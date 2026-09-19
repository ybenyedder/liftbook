package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    fun start(sec: Int) { endAt = System.currentTimeMillis() + sec * 1000L }
    fun clear() { endAt = 0 }
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
    var nameText by remember(draft) { mutableStateOf(draft.name) }

    val now = produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(250) }
    }.value
    val restRemaining = RestTimer.endAt - now
    LaunchedEffect(RestTimer.endAt) {
        if (RestTimer.endAt > 0) {
            delay(kotlin.math.max(0L, restRemaining) + 4000L)
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
                else { Repo.discardDraft(); Nav.pop(); toast(ctx, "Discarded") }
            }) { Icon(Icons.Rounded.ArrowBack, null) }
            if (isWorkout) {
                Column(Modifier.weight(1f)) {
                    Text(draft.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${Calc.fmtDur(maxOf(0, now - (draft.startedAt ?: now)))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    )
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
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreHoriz, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!isWorkout && draft.routineId != null) {
                        DropdownMenuItem(text = { Text("Delete routine", color = MaterialTheme.colorScheme.error) }, onClick = {
                            showMenu = false; showDeleteRoutine = true
                        })
                    }
                    DropdownMenuItem(
                        text = { Text(if (isWorkout) "Discard workout" else "Discard changes", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; if (hasData()) showDiscard = true else { Repo.discardDraft(); Nav.pop(); toast(ctx, "Discarded") } },
                    )
                }
            }
        }
        // ---- exercise cards ----
        LazyColumn(Modifier.weight(1f)) {
            if (draft.exercises.isEmpty()) {
                item {
                    EmptyState(
                        if (isWorkout) "No exercises yet.\nTap “Add Exercise” to start logging."
                        else "This routine is empty.\nTap “Add Exercise” to build it."
                    )
                }
            }
            items(draft.exercises.size) { ei ->
                ExCard(draft.exercises[ei], ei, isWorkout, unit)
            }
            item { Spacer(Modifier.height(170.dp)) }
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
                RestBar(remainingMs = restRemaining)
                Spacer(Modifier.height(10.dp))
            }
            Row {
                GhostButton(
                    "Add Exercise",
                    onClick = { showPicker = true },
                    modifier = Modifier.weight(1f),
                    leading = { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp)) },
                )
                Spacer(Modifier.width(10.dp))
                PrimaryButton(
                    if (isWorkout) "Finish" else "Save",
                    onClick = {
                        if (!isWorkout) {
                            val n = nameText.trim()
                            if (n.isEmpty()) { toast(ctx, "Give your routine a name"); return@PrimaryButton }
                            if (draft.exercises.isEmpty()) { toast(ctx, "Add at least one exercise"); return@PrimaryButton }
                            Repo.saveRoutine(n)
                            Nav.pop()
                            toast(ctx, "Routine saved")
                        } else {
                            if (anyDone()) showFinish = true else showNoSets = true
                        }
                    },
                    modifier = Modifier.weight(1f),
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
                    toast(ctx, "$name added")
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
                    toast(ctx, "Discarded")
                },
                onSave = {
                    val w = Repo.finishWorkout(nameText)
                    showFinish = false
                    RestTimer.clear()
                    Nav.toTab(Screen.HomeTab)
                    if (w != null && w.prs.isNotEmpty()) toast(ctx, "Saved · ${w.prs.size} PR${if (w.prs.size > 1) "s" else ""}!")
                    else toast(ctx, "Workout saved")
                },
            )
        }
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(if (isWorkout) "Discard workout?" else "Discard changes?", fontWeight = FontWeight.ExtraBold) },
            text = { Text(if (isWorkout) "Your logged sets from this session will be lost." else "Changes to this routine will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    RestTimer.clear()
                    Repo.discardDraft()
                    Nav.pop()
                    toast(ctx, "Discarded")
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Cancel") } },
        )
    }
    if (showNoSets) {
        AlertDialog(
            onDismissRequest = { showNoSets = false },
            title = { Text("No sets completed", fontWeight = FontWeight.ExtraBold) },
            text = { Text("There is nothing to save yet. Discard this workout?") },
            confirmButton = {
                TextButton(onClick = {
                    showNoSets = false
                    RestTimer.clear()
                    Repo.discardDraft()
                    Nav.pop()
                    toast(ctx, "Discarded")
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showNoSets = false }) { Text("Keep editing") } },
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
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteRoutine = false }) { Text("Cancel") } },
        )
    }
}

// ---------------- exercise card ----------------

@Composable
private fun ExCard(ex: ExEntry, ei: Int, isWorkout: Boolean, unit: String) {
    var showNotes by remember { mutableStateOf(ex.notes.isNotEmpty()) }
    AppCard {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Repo.draft?.exercises?.let { if (ei < it.size) it.removeAt(ei) } }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Text(
                    ex.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f).clickable { Nav.push(Screen.ExerciseDetail(ex.name)) },
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                MuscleTag(ex.muscle)
            }
            if (isWorkout) {
                Row(Modifier.padding(horizontal = 12.dp)) {
                    Text("SET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    Text("PREVIOUS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    Text(unit.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("REPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            } else {
                Row(Modifier.padding(horizontal = 12.dp)) {
                    Text("SET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    Text(unit.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("REPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            val prev = if (isWorkout) Repo.prevFor(ex.name) else null
            ex.sets.forEachIndexed { si, s ->
                SetRow(s, si, ei, isWorkout, unit, if (isWorkout) prev?.getOrNull(si) else null)
            }
            // add set
            Text(
                "+  Add Set",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
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
                    onValueChange = { ex.notes = it; Repo.touchPublic() },
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // set number circle — tap to toggle done (workout mode)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (s.done && isWorkout) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(
                    1.5.dp,
                    if (s.done && isWorkout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape,
                )
                .combinedClickable(
                    onClick = {
                        if (isWorkout) {
                            s.done = !s.done
                            if (s.done) RestTimer.start(Repo.settings.restSec)
                        }
                    },
                    onLongClick = { menuOpen = true },
                ),
        ) {
            if (isWorkout && s.done) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            } else {
                Text(
                    "${si + 1}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            if (isWorkout) {
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Copy set") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(15.dp)) }, onClick = {
                        menuOpen = false
                        Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                            if (si < ex.sets.size) ex.sets.add(si + 1, SetEntry(s.kg, s.reps, done = false))
                        }
                        Repo.touchPublic()
                    })
                    DropdownMenuItem(text = { Text("Delete set", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(15.dp)) }, onClick = {
                        menuOpen = false
                        Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                            if (ex.sets.size == 1) Repo.draft?.exercises?.removeAt(ei) else ex.sets.removeAt(si)
                        }
                        Repo.touchPublic()
                    })
                }
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
        } else {
            Spacer(Modifier.width(0.dp))
        }
        SetField(
            init = Calc.fmtKg(s.kg, unit),
            hint = unit,
            onChange = { s.kg = Calc.toKg(it, unit); Repo.touchPublic() },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        SetField(
            init = s.reps?.toString() ?: "",
            hint = "Reps",
            onChange = { v -> s.reps = v.filter { it.isDigit() }.take(4).toIntOrNull(); Repo.touchPublic() },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        if (!isWorkout) {
            DropdownMenuHost(s, si, ei)
        }
    }
}

@Composable
private fun DropdownMenuHost(s: SetEntry, si: Int, ei: Int) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Text(
            "⋯",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 6.dp)
                .clickable { menuOpen = true },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Copy set") }, onClick = {
                menuOpen = false
                Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                    if (si < ex.sets.size) ex.sets.add(si + 1, SetEntry(s.kg, s.reps, done = true))
                }
                Repo.touchPublic()
            })
            DropdownMenuItem(text = { Text("Delete set", color = MaterialTheme.colorScheme.error) }, onClick = {
                menuOpen = false
                Repo.draft?.exercises?.getOrNull(ei)?.let { ex ->
                    if (ex.sets.size == 1) Repo.draft?.exercises?.removeAt(ei) else ex.sets.removeAt(si)
                }
                Repo.touchPublic()
            })
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

@Composable
private fun RestBar(remainingMs: Long) {
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
        Text("REST", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
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
        .filter { (mus == "All" || it.muscle == mus) && (q.isBlank() || it.name.lowercase().contains(q.trim().lowercase())) }
        .sortedBy { it.name }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.86f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Select Exercise", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null) }
        }
        TextField(
            value = q, onValueChange = { q = it },
            placeholder = { Text("Search exercises", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp)) },
            singleLine = true,
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
            if (filtered.isEmpty()) item { EmptyState("No exercises found.") }
            else items(filtered) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(e.name) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("${e.muscle} · ${e.equip}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
        Text("Workout Summary", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
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
            Box(Modifier.weight(1f)) { Metric(Calc.fmtDur(System.currentTimeMillis() - (draft.startedAt ?: System.currentTimeMillis())), "Duration", tight = true) }
            Box(Modifier.weight(1f)) { Metric(Calc.fmtVol(vol, unit), "Volume", unit = unit, tight = true) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Metric("$sets", "Sets", tight = true) }
            Box(Modifier.weight(1f)) { Metric("$reps", "Reps", tight = true) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (prs.isEmpty()) "No new records this session" else "${prs.size} new record${if (prs.size > 1) "s" else ""}",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold,
        )
        prs.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.ex, fontSize = 13.sp)
                Text("${Calc.fmtKg(p.value, unit)} ${Calc.unitLabel(unit)} · ${p.kind}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            GhostButton("Discard", onClick = onDiscard, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            PrimaryButton("Save", onClick = onSave, modifier = Modifier.weight(1f).height(44.dp))
        }
    }
}
