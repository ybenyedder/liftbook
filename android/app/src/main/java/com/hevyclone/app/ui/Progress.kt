@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hevyclone.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.ProgressPhoto
import com.hevyclone.app.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================ bitmap loading ============================

/** Small LRU of decoded photos ("path@target" -> bitmap), keeps the grid smooth. */
private val photoMemCache = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean = size > 12
}

/** Decodes a photo file off the main thread, roughly sized for its destination. */
@Composable
fun rememberPhotoBitmap(id: Long, targetPx: Int = 720): ImageBitmap? {
    // Key on rev too: a photo downloaded by the sync turns path null→file without id changing.
    val path = remember(id, Repo.rev) { Repo.photoFile(id)?.takeIf { it.exists() }?.absolutePath }
    return produceState<ImageBitmap?>(null, path, targetPx) {
        if (path == null) return@produceState
        val key = "$path@$targetPx"
        photoMemCache[key]?.let { value = it; return@produceState }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                if (bounds.outWidth <= 0) return@runCatching null
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > targetPx) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        } ?: return@produceState
        val img = bmp.asImageBitmap()
        photoMemCache[key] = img
        value = img
    }.value
}

/** Photo cell; falls back to a dark tile with the camera icon while loading/missing. */
@Composable
fun PhotoImg(p: ProgressPhoto, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, targetPx: Int = 720) {
    val bmp = rememberPhotoBitmap(p.id, targetPx)
    Box(modifier.background(C.Card2)) {
        if (bmp != null) {
            Image(
                painter = BitmapPainter(bmp),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Rounded.AddAPhoto, null,
                tint = C.Mut2, modifier = Modifier.size(26.dp).align(Alignment.Center),
            )
        }
    }
}

private fun fmtDay(ts: Long): String =
    SimpleDateFormat("d MMM yy", Locale.FRANCE).format(Date(ts))

private fun fmtDayLong(ts: Long): String =
    SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(Date(ts)).replaceFirstChar { it.uppercase() }

// ============================ gallery ============================

/**
 * Progress photos gallery: grid most-recent-first, body-weight chart,
 * long-press to pick any two photos and compare them before/after.
 */
@Composable
fun ProgressScreen() {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val galleryScope = androidx.compose.runtime.rememberCoroutineScope()
    val photos = remember(rev) { Repo.photosDesc() }
    val weights = remember(rev) { photos.filter { it.kg != null }.sortedBy { it.ts } }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            galleryScope.launch {
                val p = withContext(Dispatchers.IO) { Repo.addPhotoFromUri(ctx, uri) }
                if (p == null) toast(ctx, L10n.s("Could not read this image", "Impossible de lire cette image"))
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Text(
                L10n.s("Progress", "Progression"),
                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { picker.launch("image/*") }) {
                Icon(Icons.Rounded.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (photos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                EmptyState(
                    L10n.s(
                        "No progress photos yet.\nAdd one after a workout to track your physique.",
                        "Aucune photo de progression.\nAjoute-en une après une séance pour suivre ton physique.",
                    )
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (weights.size >= 2) {
                    item(key = "weight-chart") {
                        Text(
                            L10n.s("Body weight", "Poids corporel"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        LineChart(
                            weights.map { fmtDay(it.ts) to it.kg!! },
                            { Calc.fmtKg(it, Repo.settings.unit) + Calc.unitLabel(Repo.settings.unit) },
                            Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                item(key = "grid-head") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            L10n.s("%1\$d photos", "%1\$d photos").format(photos.size),
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (selecting) {
                            Text(
                                if (selected.size == 2) L10n.s("Compare →", "Comparer →") else L10n.s("Select 2 photos", "Choisis 2 photos"),
                                color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickableNoRipple {
                                    if (selected.size == 2) {
                                        val byAge = selected.sortedBy { id -> Repo.photoById(id)?.ts ?: 0 }
                                        Nav.push(Screen.ComparePhotos(byAge[0], byAge[1]))
                                    }
                                },
                            )
                        } else if (photos.size >= 2) {
                            Text(
                                L10n.s("Long-press to compare", "Appui long pour comparer"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                            )
                        }
                    }
                }
                item(key = "grid") {
                    val cols = 3
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        photos.chunked(cols).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { p ->
                                    val sel = selected.contains(p.id)
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .then(
                                                if (sel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                                else Modifier
                                            )
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (selecting) {
                                                        if (sel) selected.remove(p.id)
                                                        else if (selected.size < 2) selected.add(p.id)
                                                    } else {
                                                        Nav.push(Screen.PhotoViewer(p.id))
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!selecting) selecting = true
                                                    if (!sel && selected.size < 2) selected.add(p.id)
                                                },
                                            ),
                                    ) {
                                        PhotoImg(p, Modifier.fillMaxSize(), targetPx = 420)
                                        // date badge
                                        Text(
                                            fmtDay(p.ts),
                                            color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(5.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0xB3000000))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                        )
                                        if (sel) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (selecting && !sel) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .border(1.5.dp, Color.White, RoundedCornerShape(99.dp)),
                                            )
                                        }
                                    }
                                }
                                // pad incomplete last row so cells stay square and even
                                repeat(cols - row.size) {
                    Spacer(Modifier.weight(1f))
                }
                            }
                        }
                    }
                }
                item(key = "foot") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ============================ viewer ============================

/** Full-screen viewer with swipe, editable note/body weight, and delete. */
@Composable
fun PhotoViewerScreen(id: Long) {
    val rev = Repo.rev
    val photos = remember(rev) { Repo.photosDesc() }
    val start = photos.indexOfFirst { it.id == id }
    if (start < 0) { Nav.pop(); return }
    val pager = rememberPagerState(initialPage = start) { photos.size }
    // Deleting the last page must pull the pager back inside the new bounds.
    LaunchedEffect(photos.size) {
        if (pager.currentPage >= photos.size && photos.isNotEmpty()) {
            pager.scrollToPage(photos.size - 1)
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    val current = photos.getOrNull(pager.currentPage)

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White) }
            Text(
                if (photos.size > 1) "${(pager.currentPage + 1).coerceAtMost(photos.size)}/${photos.size}" else "",
                color = C.Mut, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
            IconButton(onClick = { edit = true }) { Icon(Icons.Rounded.Edit, null, tint = Color.White) }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, null, tint = Color.White) }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            val p = photos.getOrNull(page) ?: return@HorizontalPager
            PhotoImg(p, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, targetPx = 1440)
        }
        current?.let { p ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(fmtDayLong(p.ts), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    p.kg?.let {
                        Text(
                            L10n.s("Body weight", "Poids corporel") + " : " + Calc.fmtKg(it, Repo.settings.unit) + Calc.unitLabel(Repo.settings.unit),
                            color = C.Mut, fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    if (p.wId != null && Repo.workoutById(p.wId!!) != null) {
                        Text("· " + L10n.s("linked to a workout", "liée à une séance"), color = C.Mut2, fontSize = 12.sp)
                    }
                }
                if (p.note.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(p.note, color = C.Mut, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }

    if (confirmDelete && current != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(L10n.s("Delete this photo?", "Supprimer cette photo ?"), fontWeight = FontWeight.ExtraBold) },
            text = { Text(L10n.s("It will be removed from all your devices.", "Elle sera supprimée de tous tes appareils.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Repo.deletePhoto(current.id)
                    if (Repo.photos.isEmpty()) Nav.pop()
                }) { Text(L10n.s("Delete", "Supprimer"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(L10n.s("Cancel", "Annuler"), color = C.Mut) }
            },
        )
    }
    if (edit && current != null) {
        EditPhotoDialog(current, onDone = { edit = false })
    }
}

@Composable
private fun EditPhotoDialog(p: ProgressPhoto, onDone: () -> Unit) {
    var note by remember { mutableStateOf(p.note) }
    var kg by remember { mutableStateOf(p.kg?.let { Calc.fmtKg(it, Repo.settings.unit) } ?: "") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(L10n.s("Edit photo", "Modifier la photo"), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(L10n.s("Note", "Note")) },
                    singleLine = false, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = kg,
                    onValueChange = { kg = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') },
                    label = { Text(L10n.s("Body weight (optional)", "Poids corporel (optionnel)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Convert the typed display-unit value back to kg (the field is prefilled
                // in lb in lb mode — saving it raw used to multiply the weight by 2.2 per edit).
                val parsed = kg.toDoubleOrNull()?.let { com.hevyclone.app.data.Calc.toKg(it.toString(), Repo.settings.unit) }
                Repo.updatePhoto(p.id, note = note, kg = parsed, kgSet = kg.isNotBlank())
                onDone()
            }) { Text(L10n.s("Save", "Enregistrer"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text(L10n.s("Cancel", "Annuler"), color = C.Mut) }
        },
    )
}

// ============================ before/after compare ============================

/**
 * Before/after comparison: draggable slider (default) or side-by-side, with
 * dates, days apart and body-weight delta between the two photos.
 */
@Composable
fun ComparePhotosScreen(aId: Long, bId: Long) {
    val rev = Repo.rev // a photo deleted by a background sync must recompose this screen
    val a = Repo.photoById(aId)
    val b = Repo.photoById(bId)
    if (a == null || b == null) { Nav.pop(); return }
    val older = if (a.ts <= b.ts) a else b
    val newer = if (a.ts <= b.ts) b else a
    var before by remember { mutableStateOf(older) }
    var after by remember { mutableStateOf(newer) }
    var mode by remember { mutableStateOf(0) } // 0 = slider, 1 = side by side
    var fraction by remember { mutableStateOf(0.5f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    // Calendar-day gap (a 22h→9h overnight pair is 2 days apart, not "same day").
    val days = java.time.temporal.ChronoUnit.DAYS.between(Calc.localDate(before.ts), Calc.localDate(after.ts)).toInt()
    val kgDelta = if (before.kg != null && after.kg != null) after.kg!! - before.kg!! else null

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White) }
            Text(
                L10n.s("Comparison", "Comparaison"),
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
            IconButton(onClick = { val t = before; before = after; after = t; fraction = 1f - fraction }) {
                Icon(Icons.Rounded.SwapHoriz, null, tint = Color.White)
            }
        }
        // mode chips
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CompareChip(L10n.s("Slider", "Curseur"), mode == 0) { mode = 0 }
            Spacer(Modifier.width(8.dp))
            CompareChip(L10n.s("Side by side", "Côte à côte"), mode == 1) { mode = 1 }
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            if (mode == 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(16.dp))
                        .onSizeChanged { boxSize = it }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, drag ->
                                change.consume()
                                if (boxSize.width > 0) {
                                    fraction = (fraction + drag / boxSize.width).coerceIn(0.03f, 0.97f)
                                }
                            }
                        },
                ) {
                    // APRÈS plein cadre
                    PhotoImg(after, Modifier.fillMaxSize(), targetPx = 1080)
                    // AVANT clippé à gauche du curseur (layout pleine taille, dessin tronqué)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(right = size.width * fraction) {
                                    this@drawWithContent.drawContent()
                                }
                            },
                    ) {
                        PhotoImg(before, Modifier.fillMaxSize(), targetPx = 1080)
                    }
                    // ligne + poignée du curseur
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val x = size.width * fraction
                                drawLine(Color.White, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 3f)
                                drawLine(Color(0x66000000), androidx.compose.ui.geometry.Offset(x - 3f, 0f), androidx.compose.ui.geometry.Offset(x - 3f, size.height), strokeWidth = 1f)
                            },
                    )
                    Box(
                        Modifier
                            .offset {
                                val handle = 44.dp.roundToPx()
                                IntOffset(
                                    (boxSize.width * fraction - handle / 2f).toInt(),
                                    (boxSize.height / 2f - handle / 2f).toInt(),
                                )
                            }
                            .size(44.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0xCC000000)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⇄", color = Color.White, fontSize = 20.sp)
                    }
                    // étiquettes
                    Text(
                        L10n.s("BEFORE", "AVANT") + " · " + fmtDay(before.ts),
                        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                    Text(
                        L10n.s("AFTER", "APRÈS") + " · " + fmtDay(after.ts),
                        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        PhotoImg(before, Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(14.dp)), targetPx = 720)
                        Spacer(Modifier.height(6.dp))
                        Text(L10n.s("BEFORE", "AVANT"), color = C.Mut, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(fmtDay(before.ts), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        PhotoImg(after, Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(14.dp)), targetPx = 720)
                        Spacer(Modifier.height(6.dp))
                        Text(L10n.s("AFTER", "APRÈS"), color = C.Mut, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(fmtDay(after.ts), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // bandeau infos : écart + delta poids
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (days > 0) L10n.s("%1\$d days apart", "%1\$d jours d'écart").format(days) else L10n.s("Same day", "Le même jour"),
                color = C.Mut, fontSize = 13.sp,
            )
            if (kgDelta != null) {
                Spacer(Modifier.width(14.dp))
                val sign = if (kgDelta >= 0) "+" else "−"
                Text(
                    "$sign${Calc.fmtKg(kotlin.math.abs(kgDelta), Repo.settings.unit)}${Calc.unitLabel(Repo.settings.unit)}",
                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CompareChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (active) Color.Black else Color.White,
        fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else C.Card2)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
