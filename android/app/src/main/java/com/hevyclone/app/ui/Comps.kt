package com.hevyclone.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc

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
    // Hevy-style CTA: black pill in light theme, accent pill in dark theme
    val dark = MaterialTheme.colorScheme.background == C.DBg
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (dark) MaterialTheme.colorScheme.primary else C.Text,
            contentColor = if (dark) C.AccText else Color.White,
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
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.42f).sp)
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp, textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = if (slim) 8.dp else 30.dp),
        )
    }
}

/** Smooth line chart with gradient fill — no charting library. */
@Composable
fun LineChart(points: List<Pair<String, Double>>, fmtLabel: (Double) -> String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp).padding(top = 4.dp)) {
        if (points.size < 2) return@Canvas
        val padL = 6f; val padR = 54f; val padT = 16f; val padB = 20f
        val iw = size.width - padL - padR
        val ih = size.height - padT - padB
        val vals = points.map { it.second }
        var min = vals.min(); var max = vals.max()
        if (max - min < max * 0.06 + 1.0) { val mid = (max + min) / 2; min = mid * 0.97; max = mid * 1.03 }
        val pad = (max - min) * 0.1; min -= pad; max += pad
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
