package com.hevyclone.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.Repo

@Composable
fun ProfileScreen() {
    val rev = Repo.rev
    val ctx = LocalContext.current
    val st = Repo.settings
    val unit = st.unit
    val showSettings = remember { mutableStateOf(false) }
    val month = java.time.Instant.ofEpochMilli(st.since).atZone(java.time.ZoneId.systemDefault())

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { showSettings.value = true }) { Icon(Icons.Rounded.Settings, null) }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(6.dp))
                    Avatar(st.profileName.trim().take(1).uppercase(), 76)
                    Spacer(Modifier.height(10.dp))
                    Text(st.profileName, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "@${st.handle} · since ${month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.US))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(62.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Metric("${Repo.workouts.size}", "Workouts", tight = true) }
                    Box(Modifier.weight(1f)) { Metric(Calc.fmtVol(Calc.totalVol(Repo.workouts), unit), "Volume", unit = unit, tight = true) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(62.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Metric("${Calc.streak(Repo.workouts, System.currentTimeMillis())}", "Day streak", tight = true) }
                    Box(Modifier.weight(1f)) { Metric("${Repo.workouts.sumOf { it.prs.size }}", "PRs", accent = true, tight = true) }
                }
            }
            item {
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Weekly Volume", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        LineChart(Calc.weekly(Repo.workouts, 12, unit, System.currentTimeMillis()), fmtLabel = { v ->
                            val k = Math.round(v / 100.0) / 10.0
                            "${k}k"
                        })
                    }
                }
            }
            item {
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Muscle Split", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        val dist = Calc.muscleDist(Repo.workouts, 8)
                        val maxVol = dist.firstOrNull()?.second ?: 1.0
                        if (dist.isEmpty()) Text("No data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        dist.forEach { (muscle, vol) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(muscle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(86.dp))
                                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((vol / maxVol).toFloat().coerceIn(0.04f, 1f))
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("${Calc.fmtVol(vol, unit)} ${Calc.unitLabel(unit)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                                    modifier = Modifier.width(70.dp))
                            }
                        }
                    }
                }
            }
            item {
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Recent Records", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        val prs = Calc.recentPrs(Repo.workouts, 8)
                        if (prs.isEmpty()) Text("No records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        prs.forEach { (p, date, _) ->
                            PrRow("${p.ex} · ${p.kind}", "${Calc.fmtKg(p.value, unit)} ${Calc.unitLabel(unit)} · ${Calc.fmtDateShort(date)}")
                        }
                    }
                }
            }
            item {
                Text("Local demo · your data stays on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), style = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center))
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    if (showSettings.value) SettingsSheet(onClose = { showSettings.value = false })
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val st = Repo.settings
    var confirm by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(bottom = 30.dp)) {
            Text("Settings", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            Text("UNITS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
                listOf("kg", "lb").forEach { u ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (st.unit == u) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickableNoRipple { Repo.setUnit(u) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(u, color = if (st.unit == u) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
            Text("REST TIMER", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { sec ->
                    Box(Modifier.weight(1f)) { Chip("${sec}s", st.restSec == sec, onClick = { Repo.setRest(sec) }) }
                }
            }
            Text("THEME", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
                listOf("dark", "light").forEach { t ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (st.theme == t) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickableNoRipple {
                                if (st.theme != t) {
                                    Repo.setTheme(t)
                                    onClose()
                                    (ctx as? android.app.Activity)?.recreate()
                                }
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(t.replaceFirstChar { it.uppercase() },
                            color = if (st.theme == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
            Text("DATA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            TextButton(onClick = { confirm = "reseed" }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Reload demo data", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { confirm = "wipe" }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Erase all data", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (action == "reseed") "Reload demo data?" else "Erase all data?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    if (action == "reseed") "Your current workouts and routines will be replaced by the demo dataset."
                    else "All workouts, routines and records will be permanently deleted from this device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (action == "reseed") {
                        Repo.reseed()
                    } else {
                        Repo.wipe()
                    }
                    onClose()
                }) { Text(if (action == "reseed") "Reload" else "Erase", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}
