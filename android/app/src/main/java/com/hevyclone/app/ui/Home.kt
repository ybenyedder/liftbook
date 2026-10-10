package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Precomputed, immutable view model of one feed post — zero work during scroll. */
@Immutable
private data class PostUi(
    val id: Long,
    val author: String,
    val timeLabel: String,
    val dayTitle: String,
    val duration: String,
    val volume: String,
    val prCount: Int,
    val exerciseLines: List<String>,
    val names: List<String>,
    val setCounts: List<Int>,
    val muscles: List<String>,
)

@Composable
fun HomeScreen() {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val unit = Repo.settings.unit
    val posts = remember(rev, unit) { buildPosts(unit) }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    L10n.s("Home", "Accueil", "Inicio", "Startseite"),
                    fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { Nav.push(Screen.Exercises) }) { Icon(Icons.Rounded.Search, null) }
                IconButton(onClick = { toast(ctx, L10n.s("No new notifications", "Pas de nouvelles notifications")) }) { Icon(Icons.Rounded.Notifications, null) }
            }
        }
        if (posts.isEmpty()) {
            item(key = "empty") {
                EmptyState(L10n.s("No workouts yet.\nHead to Training to get started!", "Aucune séance pour le moment.\nVa dans Entraînement pour démarrer !"))
            }
        } else {
            items(posts, key = { it.id }, contentType = { "post" }) { p ->
                FeedPost(p)
                HorizontalDivider(color = MaterialTheme.colorScheme.background, thickness = 8.dp)
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(20.dp)) }
    }
}

private fun buildPosts(unit: String): List<PostUi> =
    Repo.workoutsDesc().take(30).map { w ->
        PostUi(
            id = w.id,
            author = Repo.settings.profileName,
            timeLabel = relativeTime(w.startedAt),
            dayTitle = dayName(w.startedAt),
            duration = Calc.fmtDur(w.endedAt - w.startedAt),
            volume = "${Calc.fmtVol(Calc.vol(w), unit)} ${Calc.unitLabel(unit)}",
            prCount = w.prs.size,
            exerciseLines = w.exercises.map { seriesLabel(it.sets.size, exName(it.name)) },
            names = w.exercises.map { exName(it.name) },
            setCounts = w.exercises.map { it.sets.size },
            muscles = w.exercises.map { it.muscle },
        )
    }

@Composable
private fun FeedPost(p: PostUi) {
    var expanded by remember(p.id) { mutableStateOf(false) }
    val ctx = LocalContext.current
    val shown = if (expanded) p.exerciseLines.size else minOf(3, p.exerciseLines.size)
    val hidden = p.exerciseLines.size - shown

    Column(
        Modifier
            .fillMaxWidth()
            .clickable { Nav.push(Screen.WorkoutDetail(p.id)) }
            .padding(vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarImg(Repo.settings.profileName.trim().take(1).uppercase(), 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.author, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(p.timeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            IconButton(onClick = { Nav.push(Screen.WorkoutDetail(p.id)) }) {
                Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            p.dayTitle,
            fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 8.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            FeedStat(L10n.s("Time", "Temps", "Tiempo", "Zeit"), p.duration, Modifier.weight(1.2f))
            FeedStat(L10n.s("Volume", "Volume", "Volumen", "Volumen"), p.volume, Modifier.weight(1.2f))
            FeedStat(L10n.s("Records", "Records", "Récords", "Rekorde"), "", Modifier.weight(1f), medal = p.prCount)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.outline)
        for (i in 0 until shown) {
            val setCount = p.setCounts[i]
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        "$setCount",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    p.names[i],
                    fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (hidden > 0) {
            Text(
                seeMoreExercises(hidden),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 8.dp),
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.outline)
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { toast(ctx, L10n.s("Added to favorites", "Ajouté aux favoris")) }) { Icon(Icons.Rounded.ThumbUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { toast(ctx, L10n.s("Comments coming soon", "Les commentaires arrivent bientôt")) }) { Icon(Icons.Rounded.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { toast(ctx, L10n.s("Shared!", "Partagé !")) }) { Icon(Icons.Rounded.IosShare, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun FeedStat(label: String, value: String, modifier: Modifier = Modifier, medal: Int = -1) {
    Column(modifier) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(2.dp))
        if (medal >= 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏅", fontSize = 15.sp)
                Spacer(Modifier.width(5.dp))
                Text("$medal", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun dayName(ms: Long): String {
    val loc = if (L10n.lang == "fr") Locale.FRANCE else Locale.US
    val s = Calc.localDate(ms).format(DateTimeFormatter.ofPattern("EEEE", loc))
    return s.replaceFirstChar { it.uppercase(loc) }
}

private fun relativeTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    if (diff < 0) return dayName(ms) // future-dated import: show the date, not "1 min ago"
    val hours = diff / 3600000
    if (L10n.lang != "fr") {
        return when {
            hours < 1 -> "${maxOf(1, (diff / 60000).toInt())} min ago"
            hours < 24 -> "$hours h ago"
            else -> {
                val days = (hours / 24).toInt()
                if (days == 1) "1 day ago" else "$days days ago"
            }
        }
    }
    return when {
        hours < 1 -> "il y a ${maxOf(1, (diff / 60000).toInt())} min"
        hours < 24 -> "il y a $hours h"
        else -> {
            val days = (hours / 24).toInt()
            if (days == 1) "il y a 1 jour" else "il y a $days jours"
        }
    }
}
