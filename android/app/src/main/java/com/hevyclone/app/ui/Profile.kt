@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hevyclone.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Cloud
import com.hevyclone.app.data.Repo
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ProfileScreen() {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val st = Repo.settings
    val unit = st.unit

    // cached aggregates
    val totalVol = remember(rev) { Calc.totalVol(Repo.workouts) }
    val totalSets = remember(rev) { Repo.workouts.sumOf { Calc.setsDone(it) } }
    val totalReps = remember(rev) { Repo.workouts.sumOf { Calc.reps(it) } }
    val dayVolumes = remember(rev) {
        Repo.workouts.groupBy { Calc.dayKey(it.startedAt) }
            .mapValues { (_, ws) -> ws.sumOf { Calc.vol(it) } }
    }
    val dist = remember(rev) { Calc.muscleDist(Repo.workouts, 15) }
    val maxVol = dist.firstOrNull()?.second ?: 1.0

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarImg(st.profileName.trim().take(1).uppercase(), 52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(st.profileName, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("@" + st.handle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        ProfileStat("${Repo.workouts.size}", L10n.s("Workouts", "Entraînements", "Entrenamientos", "Trainings"))
                        ProfileStat("0", L10n.s("Followers", "Abonnés", "Seguidores", "Follower"))
                        ProfileStat("0", L10n.s("Following", "Abonnements", "Seguidos", "Folgt"))
                    }
                }
                IconButton(onClick = { Nav.push(Screen.Settings) }) { Icon(Icons.Rounded.Settings, null) }
            }
        }
        item(key = "volume-heat") {
            TrainingDaysGrid(dayVolumes)
        }
        item(key = "streak") {
            val streak = remember(rev) { Calc.streak(Repo.workouts, System.currentTimeMillis()) }
            if (streak >= 2) {
                Row(
                    Modifier
                        .padding(start = 16.dp, top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.Card2)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        L10n.s("%1\$d-day streak", "Série de %1\$d jours").format(streak),
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    )
                }
            }
        }
        item(key = "totals") {
            Text(
                buildString {
                    append("${Calc.fmtVol(totalVol, unit)} ${Calc.unitLabel(unit)}")
                    append("  ·  ")
                    append(
                        (if (totalSets > 1) L10n.s("%1\$d series", "%1\$d séries") else L10n.s("%1\$d series", "%1\$d série")).format(totalSets)
                    )
                    append("  ·  ")
                    append(L10n.s("%1\$d reps", "%1\$d réps").format(totalReps))
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
        item(key = "month-stats") {
            val monthStart = remember(rev) {
                val d = LocalDate.now().withDayOfMonth(1)
                d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val month = remember(rev, monthStart) { Repo.workouts.filter { it.startedAt >= monthStart } }
            if (month.isNotEmpty()) {
                val mVol = month.sumOf { Calc.vol(it) }
                val mPrs = month.sumOf { it.prs.size }
                Text(
                    L10n.s("This month", "Ce mois-ci") + " : ${month.size} " +
                        L10n.s("workouts", "séances") + " · ${Calc.fmtVol(mVol, unit)} ${Calc.unitLabel(unit)} · ${mPrs} " +
                        L10n.s("PRs", "records"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                )
            }
        }
        item(key = "rolling-week") {
            val now = remember(rev) { System.currentTimeMillis() }
            val last7 = remember(rev, now) { Repo.workouts.filter { it.startedAt >= now - 7L * 86400000L } }
            val prev7 = remember(rev, now) { Repo.workouts.filter { it.startedAt in (now - 14L * 86400000L) until (now - 7L * 86400000L) } }
            if (last7.isNotEmpty() || prev7.isNotEmpty()) {
                val v1 = last7.sumOf { Calc.vol(it) }
                val v2 = prev7.sumOf { Calc.vol(it) }
                val delta = if (v2 > 0) ((v1 - v2) / v2 * 100).toInt() else null
                Text(
                    L10n.s("Last 7 days", "7 derniers jours") + " : ${Calc.fmtVol(v1, unit)} ${Calc.unitLabel(unit)} · ${last7.size} " +
                        L10n.s("workouts", "séances") +
                        (delta?.let { "  (" + (if (it >= 0) "+" else "") + "$it% " + L10n.s("vs previous week", "vs semaine précédente") + ")" } ?: "") +
                        "\n" + L10n.s("Previous 7 days", "7 jours précédents") + " : ${Calc.fmtVol(v2, unit)} ${Calc.unitLabel(unit)} · ${prev7.size} " + L10n.s("workouts", "séances"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                )
            }
        }
        item(key = "monthly-chart") {
            val monthly = remember(rev) {
                val cal = java.time.LocalDate.now().withDayOfMonth(1)
                val mLoc = if (L10n.lang == "fr") java.util.Locale.FRANCE else java.util.Locale.getDefault()
                (5 downTo 0).map { back ->
                    val m = cal.minusMonths(back.toLong())
                    val start = m.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val end = m.plusMonths(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val label = m.format(java.time.format.DateTimeFormatter.ofPattern("MMM", mLoc))
                    label to Repo.workouts.filter { it.startedAt in start until end }.sumOf { Calc.vol(it) }
                }
            }
            val maxV = (monthly.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    monthly.forEach { (label, vol) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                                val frac = ((vol / maxV).toFloat()).coerceIn(0f, 1f)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((frac * 76f).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label.take(3), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item(key = "history-link") {
            Spacer(Modifier.height(14.dp))
            AppCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Nav.push(Screen.History) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(L10n.s("Workout history", "Historique des séances"), fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
        item(key = "progress-photos") {
            Spacer(Modifier.height(8.dp))
            AppCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { Nav.push(Screen.Progress) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(L10n.s("Progress photos", "Photos de progression"), fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (Repo.photos.isEmpty()) "" else "${Repo.photos.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    val recent = remember(rev) { Repo.photosDesc().take(5) }
                    if (recent.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            recent.forEach { p ->
                                PhotoImg(
                                    p,
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    targetPx = 240,
                                )
                            }
                            repeat(5 - recent.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item(key = "muscle-title") {
            Text(
                L10n.s("Volume by muscle group", "Volume par groupe musculaire"),
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp),
            )
        }
        if (dist.isEmpty()) {
            item(key = "empty") {
                EmptyState(L10n.s("No data yet.\nYour stats will appear after your first workout.", "Aucune donnée.\nTes stats apparaîtront après ta première séance."))
            }
        } else {
            items(dist.size, key = { dist[it].first }) { i ->
                val (muscle, vol) = dist[i]
                Row(
                    Modifier.fillMaxWidth().animateItemPlacement().padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IllIcon(muscle, 34.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(muscleName(muscle), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("${Calc.fmtVol(vol, unit)} ${Calc.unitLabel(unit)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((vol / maxVol).toFloat().coerceIn(0.02f, 1f))
                                    .animateContentSize()
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
        item(key = "foot") {
            Text(
                if (Cloud.session != null)
                    L10n.s("Synced to your account", "Synchronisé sur ton compte") + " · " + Cloud.session!!.email
                else L10n.s("Local app · your data stays on this device.", "Application locale · tes données restent sur cet appareil."),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
    }
}

/**
 * Multi-year training-day grid (GitHub contribution style).
 * One square per day, lit on the days you trained — in the app accent.
 * Stacked by year with month labels, fun rest-day caption, and a next-year closer.
 */
@Composable
private fun TrainingDaysGrid(dayVolumes: Map<String, Double>) {
    val accent = MaterialTheme.colorScheme.primary
    val empty = C.Card2
    val trainedDays = remember(dayVolumes) { dayVolumes.keys }
    val today = LocalDate.now()
    val years = remember(trainedDays) {
        val first = trainedDays.minOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
        ((first.year)..(today.year)).toList() // chronological top → bottom
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            L10n.s(
                "One square per day, lit on the days you trained.",
                "Un carré par jour, allumé les jours où tu t'es entraîné.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        years.forEach { year ->
            YearGrid(year, dayVolumes, accent, empty, today)
        }

        // Fun rest-day caption (the day of week you almost never train)
        val restDow = remember(trainedDays) {
            if (trainedDays.size < 8) null
            else {
                val counts = IntArray(7) // Mon=0 … Sun=6
                for (k in trainedDays) {
                    val d = runCatching { LocalDate.parse(k) }.getOrNull() ?: continue
                    counts[(d.dayOfWeek.value + 6) % 7]++
                }
                counts.indices.minBy { counts[it] }.takeIf { counts[it] < trainedDays.size / 20.0 }
            }
        }
        if (restDow != null) {
            val loc = if (L10n.lang == "fr") java.util.Locale.FRENCH else java.util.Locale.US
            val name = java.time.DayOfWeek.of(restDow + 1)
                .getDisplayName(java.time.format.TextStyle.FULL, loc)
                .replaceFirstChar { it.uppercase(loc) }
            Text(
                if (L10n.lang == "fr") "$name c'est repos." else "$name is rest day.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            )
        }

        Text(
            L10n.s(
                "See you there in ${today.year + 1}",
                "Rendez-vous en ${today.year + 1}",
            ),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun YearGrid(
    year: Int,
    dayVolumes: Map<String, Double>,
    accent: Color,
    empty: Color,
    today: LocalDate,
) {
    val days: List<LocalDate> = remember(year) {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        generateSequence(start) { if (it < end) it.plusDays(1) else null }.toList()
    }
    val maxDay = remember(dayVolumes) { dayVolumes.values.maxOrNull() ?: 1.0 }
    val loc = if (L10n.lang == "fr") java.util.Locale.FRENCH else java.util.Locale.US
    val trainedCount = remember(year, dayVolumes) {
        days.count { (dayVolumes[it.toString()] ?: 0.0) > 0.0 }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$year",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (trainedCount > 0) {
                Text(
                    L10n.s("$trainedCount days", "$trainedCount jours", "$trainedCount días", "$trainedCount Tage"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // month labels (12 evenly spaced)
        Row(Modifier.fillMaxWidth()) {
            (1..12).forEach { m ->
                Text(
                    LocalDate.of(year, m, 1)
                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM", loc))
                        .take(3),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(88.dp)) {
            drawYearGrid(days, dayVolumes, maxDay, accent, empty, today)
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun DrawScope.drawYearGrid(
    days: List<LocalDate>,
    dayVolumes: Map<String, Double>,
    maxDay: Double,
    accent: Color,
    empty: Color,
    today: LocalDate,
) {
    if (days.isEmpty()) return
    val first = days[0]
    val lead = (first.dayOfWeek.value + 6) % 7   // Monday-first
    val weeks = ((lead + days.size) + 6) / 7
    val gap = 2.2f
    val cell = minOf(
        (size.width - gap * (weeks - 1)) / weeks,
        (size.height - gap * 6) / 7,
    )
    days.forEach { d ->
        val idx = d.dayOfYear - 1 + lead
        val w = idx / 7
        val dow = idx % 7
        val v = dayVolumes[d.toString()] ?: 0.0
        val future = d > today
        val isToday = d == today
        val color = when {
            future -> empty.copy(alpha = 0.40f)
            v <= 0.0 -> empty
            else -> accent.copy(alpha = (0.45f + 0.55f * ((v / maxDay).coerceIn(0.0, 1.0)).toFloat()))
        }
        val pad = if (isToday) cell * 0.08f else 0f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * (cell + gap) + pad, dow * (cell + gap) + pad),
            size = Size(cell - pad * 2, cell - pad * 2),
            cornerRadius = CornerRadius(cell * 0.22f),
        )
        if (isToday) {
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * (cell + gap), dow * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell * 0.22f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f),
            )
        }
    }
}
