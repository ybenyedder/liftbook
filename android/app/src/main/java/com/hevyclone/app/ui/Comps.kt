package com.hevyclone.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo

val Accent get() = C.Accent
val AccText get() = C.AccText

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) { content() }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(999.dp)
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun Metric(v: String, l: String, accent: Boolean = false, unit: String? = null, tight: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    v,
                    color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (unit != null) {
                    Spacer(Modifier.width(3.dp))
                    Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(l, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(0.dp),
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Avatar(letter: String, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.42f).sp)
    }
}

/** Profile photo when set (filesDir/avatar.jpg), letter avatar otherwise. */
@Composable
fun AvatarImg(letter: String, size: Dp) {
    val bmp = remember(Repo.rev) { com.hevyclone.app.data.AvatarCache.bmp }
    if (bmp != null) {
        Box(Modifier.size(size).clip(CircleShape)) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Avatar(letter, size.value.toInt())
    }
}

@Composable
fun MuscleTag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyState(text: String, slim: Boolean = false) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = visibleState, enter = fadeIn(tween(300))) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.5.sp, textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = if (slim) 8.dp else 30.dp),
            )
        }
    }
}

/** Smooth line chart with gradient fill — no charting library. Y-axis labels on the right. */
@Composable
fun LineChart(points: List<Pair<String, Double>>, fmtLabel: (Double) -> String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp).padding(top = 4.dp)) {
        if (points.size < 2) return@Canvas
        val padL = 6f; val padR = 54f; val padT = 16f; val padB = 20f
        val iw = size.width - padL - padR
        val ih = size.height - padT - padB
        val vals = points.map { it.second }
        var min = vals.min(); var max = vals.max()
        if (max - min < max * 0.06 + 1.0) { val mid = (max + min) / 2; min = mid * 0.97; max = mid * 1.03 }
        val pad = (max - min) * 0.1; min -= pad; max += pad
        if (max - min < 1e-9) { min -= 1.0; max += 1.0 } // all-equal values (0/0) → flat line, no NaN
        fun x(i: Int): Float = padL + (i.toFloat() / (points.size - 1)) * iw.toFloat()
        fun y(v: Double): Float = (padT + ih - ((v - min) / (max - min)) * ih).toFloat()

        // grid
        for (g in 0..2) {
            val gy = padT + (ih * g / 2.0).toFloat()
            drawLine(labelColor.copy(alpha = 0.15f), Offset(padL, gy), Offset(padL + iw, gy), 1f)
        }
        // smooth path
        val path = Path()
        path.moveTo(x(0), y(vals[0]))
        for (i in 1 until points.size) {
            val x0 = x(i - 1); val y0 = y(vals[i - 1]); val x1 = x(i); val y1 = y(vals[i])
            path.quadraticBezierTo(x0, y0, (x0 + x1) / 2f, (y0 + y1) / 2f)
        }
        path.lineTo(x(points.size - 1), y(vals.last()))
        // fill
        val fill = Path().apply {
            addPath(path)
            lineTo(x(points.size - 1), padT + ih)
            lineTo(x(0), padT + ih)
            close()
        }
        clipRect(right = size.width * progress.value) {
            drawPath(fill, Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                startY = padT, endY = padT + ih,
            ))
            drawPath(path, accent, style = Stroke(4f, cap = StrokeCap.Round))
            // last point
            val lx = x(points.size - 1); val ly = y(vals.last())
            drawCircle(accent, 6f, Offset(lx, ly))
            drawCircle(C.Bg, 3.5f, Offset(lx, ly))
        }
        // Y-axis labels (max / mid / min) — fmtLabel finally used, Hevy-style right rail
        val paint = android.graphics.Paint().apply {
            color = labelColor.copy(alpha = 0.85f).toArgb()
            textSize = 9.sp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.run {
            drawText(fmtLabel(max - pad / 2), size.width - 4f, padT + 8f, paint)
            drawText(fmtLabel((min + max) / 2), size.width - 4f, padT + ih / 2 + 3f, paint)
            drawText(fmtLabel(min + pad / 2), size.width - 4f, padT + ih, paint)
        }
    }
}

/** Highlight positions per muscle, (isFront, x, y) normalized — kept alongside the pictogram below. */
private val BODY_SPOTS: Map<String, List<Triple<Boolean, Float, Float>>> = mapOf(
    "Chest" to listOf(Triple(true, .37f, .28f), Triple(true, .63f, .28f)),
    "Shoulders" to listOf(Triple(true, .30f, .21f), Triple(true, .70f, .21f)),
    "Biceps" to listOf(Triple(true, .23f, .32f), Triple(true, .77f, .32f)),
    "Forearms" to listOf(Triple(true, .17f, .40f), Triple(true, .83f, .40f)),
    "Abs" to listOf(Triple(true, .5f, .38f)),
    "Quads" to listOf(Triple(true, .42f, .60f), Triple(true, .58f, .60f)),
    "Adductors" to listOf(Triple(true, .46f, .52f), Triple(true, .54f, .52f)),
    "Calves" to listOf(Triple(true, .41f, .82f), Triple(true, .59f, .82f), Triple(false, .41f, .82f), Triple(false, .59f, .82f)),
    "Traps" to listOf(Triple(true, .5f, .17f), Triple(false, .5f, .17f)),
    "Lats" to listOf(Triple(false, .37f, .30f), Triple(false, .63f, .30f)),
    "Triceps" to listOf(Triple(false, .23f, .32f), Triple(false, .77f, .32f)),
    "Lower back" to listOf(Triple(false, .5f, .38f)),
    "Glutes" to listOf(Triple(false, .43f, .47f), Triple(false, .57f, .47f)),
    "Hamstrings" to listOf(Triple(false, .42f, .60f), Triple(false, .58f, .60f)),
    "Abductors" to listOf(Triple(false, .36f, .52f), Triple(false, .64f, .52f)),
)

/** Hevy-style mini body pictogram (front/back) with the worked muscles highlighted in accent. */
@Composable
fun BodyMap(front: Boolean, muscles: Set<String>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val body = Color(0xFF4A4F56)
    Box(
        modifier
            .size(width = 34.dp, height = 56.dp)
            .clip(RoundedCornerShape(9.dp))
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val c = androidx.compose.ui.geometry.CornerRadius(w * 0.09f)
            fun limb(x1: Float, y1: Float, x2: Float, y2: Float, t: Float) =
                drawLine(body, Offset(w * x1, h * y1), Offset(w * x2, h * y2), strokeWidth = w * t, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            // head + neck
            drawCircle(body, radius = w * 0.115f, center = Offset(w * 0.5f, h * 0.085f))
            drawLine(body, Offset(w * 0.5f, h * 0.14f), Offset(w * 0.5f, h * 0.185f), strokeWidth = w * 0.11f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            // torso — tapered V shape
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.36f, h * 0.19f)
                lineTo(w * 0.64f, h * 0.19f)
                lineTo(w * 0.585f, h * 0.475f)
                lineTo(w * 0.415f, h * 0.475f)
                close()
            }
            drawPath(path, body)
            // pelvis
            drawRoundRect(body, topLeft = Offset(w * 0.415f, h * 0.475f), size = androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.075f), cornerRadius = c)
            // arms (slight outward angle)
            limb(0.345f, 0.205f, 0.245f, 0.31f, 0.105f)
            limb(0.245f, 0.31f, 0.275f, 0.435f, 0.09f)
            limb(0.655f, 0.205f, 0.755f, 0.31f, 0.105f)
            limb(0.755f, 0.31f, 0.725f, 0.435f, 0.09f)
            // legs
            limb(0.455f, 0.545f, 0.43f, 0.75f, 0.125f)
            limb(0.43f, 0.75f, 0.425f, 0.925f, 0.10f)
            limb(0.545f, 0.545f, 0.57f, 0.75f, 0.125f)
            limb(0.57f, 0.75f, 0.575f, 0.925f, 0.10f)
            // worked-muscle highlights
            muscles.forEach { m ->
                BODY_SPOTS[m]?.forEach { (f, x, y) ->
                    if (f == front) drawCircle(accent, radius = w * 0.085f, center = Offset(w * x, h * y))
                }
            }
        }
    }
}

/**
 * Shown when the user tries to start a workout/routine while a draft is already
 * alive (e.g. the app was killed mid-workout): offer to resume it or throw it away
 * and start fresh. Without this the draft blocks every new start with just a toast.
 */
@Composable
fun WorkoutInProgressDialog(
    onDismiss: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.s("Workout in progress", "Une séance est déjà en cours"), fontWeight = FontWeight.ExtraBold) },
        text = { Text(L10n.s("Resume it, or discard it and start a new one.", "Reprends-la, ou supprime-la pour en commencer une nouvelle.")) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onResume) {
                Text(L10n.s("Resume", "Reprendre"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onRestart) {
                Text(L10n.s("Discard & restart", "Supprimer et recommencer"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
fun PrRow(left: String, right: String, subLeft: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(left, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            if (subLeft != null) Text(subLeft, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 11.5.sp)
        }
        Text(right, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
