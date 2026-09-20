package com.hevyclone.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import com.hevyclone.app.data.Repo
import com.hevyclone.app.data.Workout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

sealed interface Screen {
    data object HomeTab : Screen
    data object TrainingTab : Screen
    data object ProfileTab : Screen
    data object Settings : Screen
    data class WorkoutDetail(val id: Long) : Screen
    data class ExerciseDetail(val name: String) : Screen
    data class RoutineDetail(val id: Long) : Screen
    data object History : Screen
    data object Exercises : Screen
    data object Logger : Screen
}

object Nav {
    var pendingStartEmpty = false

    val stack = mutableStateListOf<Screen>(Screen.HomeTab)
    val current: Screen get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
    fun toTab(s: Screen) { stack.clear(); stack.add(s) }
    val atTab: Boolean get() = stack.size == 1
}

/** Global 5-second undo for a deleted workout — survives navigation. */
object DeletedUndo {
    var workout by mutableStateOf<Workout?>(null)
    fun set(w: Workout?) { workout = w }
}

private data class TabDef(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
fun App(refreshKey: Int = 0) {
    androidx.compose.runtime.LaunchedEffect(refreshKey) {
        if (Nav.pendingStartEmpty) {
            Nav.pendingStartEmpty = false
            if (Repo.draft == null) Repo.startWorkout(null)
            Nav.push(Screen.Logger)
        }
    }
    val tabs = listOf(
        TabDef(Screen.HomeTab, L10n.s("Home", "Accueil", "Inicio", "Startseite"), Icons.Rounded.Home),
        TabDef(Screen.TrainingTab, L10n.s("Training", "Entraînement", "Entrenamiento", "Training"), Icons.Rounded.FitnessCenter),
        TabDef(Screen.ProfileTab, L10n.s("Profile", "Profil", "Perfil", "Profil"), Icons.Rounded.Person),
    )

    BackHandler(enabled = !Nav.atTab) { Nav.pop() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (Nav.atTab) {
                NavigationBar(containerColor = C.NavBar, tonalElevation = 0.dp) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = Nav.current == t.screen,
                            onClick = { Nav.toTab(t.screen) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).statusBarsPadding()) {
            androidx.compose.runtime.LaunchedEffect(Nav.current) {
                val d = com.hevyclone.app.data.Repo.draft
                if (Nav.current != Screen.Logger && (d == null || d.startedAt == null)) WorkoutNotif.cancel()
            }
            androidx.compose.animation.Crossfade(
                targetState = Nav.current,
                animationSpec = androidx.compose.animation.core.tween(180),
                label = "nav",
            ) { s ->
                when (s) {
                    Screen.HomeTab -> HomeScreen()
                    Screen.TrainingTab -> TrainingScreen()
                    Screen.ProfileTab -> ProfileScreen()
                    Screen.Settings -> SettingsScreen()
                    is Screen.WorkoutDetail -> WorkoutDetailScreen(s.id)
                    is Screen.ExerciseDetail -> ExerciseDetailScreen(s.name)
                    is Screen.RoutineDetail -> RoutineDetailScreen(s.id)
                    Screen.History -> HistoryScreen()
                    Screen.Exercises -> ExercisesScreen()
                    Screen.Logger -> LoggerScreen()
                }
            }
            // Global undo bar for a deleted workout — hosted in App so it survives navigation.
            AnimatedVisibility(
                visible = DeletedUndo.workout != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                DeletedUndo.workout?.let { victim ->
                    LaunchedEffect(victim.id) {
                        delay(5000)
                        DeletedUndo.set(null)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            L10n.s("Workout deleted", "Séance supprimée"),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            L10n.s("UNDO", "ANNULER"),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                Repo.restoreWorkout(victim)
                                DeletedUndo.set(null)
                            },
                        )
                    }
                }
            }
        }
    }
}
