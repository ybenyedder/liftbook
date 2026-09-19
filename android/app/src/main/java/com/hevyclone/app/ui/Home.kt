package com.hevyclone.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.Workout
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen() {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val feed = remember(rev) { Repo.workoutsDesc().take(15) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(L10n.s("Home", "Accueil", "Inicio", "Startseite"), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { Nav.push(Screen.Exercises) }) { Icon(Icons.Rounded.Search, null) }
                IconButton(onClick = { toast(ctx, L10n.s("No new notifications", "Pas de nouvelles notifications", "Sin notificaciones nuevas", "Keine neuen Benachrichtigungen")) }) { Icon(Icons.Rounded.Notifications, null) }
            }
        }
        if (feed.isEmpty()) item { EmptyState(L10n.s("No workouts yet.\nHead to Training to get started!", "Aucune séance pour le moment.\nVa dans Entraînement pour démarrer !", "Aún no hay entrenamientos.\n¡Ve a Entrenamiento para empezar!", "Noch keine Workouts.\nStarte unter Training!")) }
        else items(feed.size) { i ->
            FeedPost(feed[i])
            if (i < feed.size - 1) Box(Modifier.fillMaxWidth().height(8.dp).background(Color.Black))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.s("Suggested Athletes", "Athlètes Recommandés", "Atletas Recomendados", "Empfohlene Athleten"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { toast(ctx, L10n.s("Invite copied!", "Invitation copiée !", "¡Invitación copiada!", "Einladung kopiert!")) }) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(L10n.s("Invite a friend", "Inviter un ami", "Invitar a un amigo", "Freund einladen"), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            val suggestions = listOf(
                listOf("alex_m", Color(0xFF7C4DFF)),
                listOf("sam.frt", Color(0xFFFF7043)),
                listOf("lea.fit", Color(0xFF26A69A)),
            )
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(suggestions.size) { i ->
                    val name = suggestions[i][0] as String
                    val color = suggestions[i][1] as Color
                    Column(
                        Modifier
                            .width(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(84.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(name.take(1).uppercase(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Featured", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { toast(ctx, L10n.s("Request sent to $name", "Demande envoyée à $name")) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(L10n.s("Follow", "Suivre", "Seguir", "Folgen"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun FeedPost(w: Workout) {
    var expanded by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val shown = if (expanded) w.exercises else w.exercises.take(3)
    val hidden = w.exercises.size - shown.size

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(Repo.settings.profileName.trim().take(1).uppercase(), 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Repo.settings.profileName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(relativeTime(w.startedAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            IconButton(onClick = { toast(ctx, L10n.s("Workout options", "Options de la séance")) }) {
                Text("•••", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold)
            }
        }
        Text(
            dayNameFr(w.startedAt),
            fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 8.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            FeedStat(L10n.s("Time", "Temps", "Tiempo", "Zeit"), Calc.fmtDur(w.endedAt - w.startedAt), Modifier.weight(1.2f))
            FeedStat(L10n.s("Volume", "Volume", "Volumen", "Volumen"), "${Calc.fmtVol(Calc.vol(w), Repo.settings.unit)} kg", Modifier.weight(1.2f))
            FeedStat(L10n.s("Records", "Records", "Récords", "Rekorde"), "", Modifier.weight(1f), medal = w.prs.size)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.outline)
        shown.forEach { ex ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    IllIcon(ex.muscle, 30.dp)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    seriesLabel(ex.sets.size, exName(ex.name)),
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
            IconButton(onClick = { toast(ctx, L10n.s("Shared!", "Partagé !", "¡Compartido!", "Geteilt!")) }) { Icon(Icons.Rounded.IosShare, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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

private fun dayNameFr(ms: Long): String {
    val loc = if (L10n.lang == "fr") Locale.FRANCE else Locale.US
    val s = Calc.localDate(ms).format(DateTimeFormatter.ofPattern("EEEE", loc))
    return s.replaceFirstChar { it.uppercase(loc) }
}

private fun relativeTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
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
