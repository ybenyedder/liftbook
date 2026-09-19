package com.hevyclone.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

sealed interface Screen {
    data object HomeTab : Screen
    data object TrainingTab : Screen
    data object ProfileTab : Screen
    data class WorkoutDetail(val id: Long) : Screen
    data class ExerciseDetail(val name: String) : Screen
    data class RoutineDetail(val id: Long) : Screen
    data object History : Screen
    data object Exercises : Screen
    data object Logger : Screen
}

object Nav {
    val stack = mutableStateListOf<Screen>(Screen.HomeTab)
    val current: Screen get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
    fun toTab(s: Screen) { stack.clear(); stack.add(s) }
    val atTab: Boolean get() = stack.size == 1
}

private data class TabDef(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
fun App() {
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
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
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
            androidx.compose.animation.Crossfade(
                targetState = Nav.current,
                animationSpec = androidx.compose.animation.core.tween(180),
                label = "nav",
            ) { s ->
                when (s) {
                    Screen.HomeTab -> HomeScreen()
                    Screen.TrainingTab -> TrainingScreen()
                    Screen.ProfileTab -> ProfileScreen()
                    is Screen.WorkoutDetail -> WorkoutDetailScreen(s.id)
                    is Screen.ExerciseDetail -> ExerciseDetailScreen(s.name)
                    is Screen.RoutineDetail -> RoutineDetailScreen(s.id)
                    Screen.History -> HistoryScreen()
                    Screen.Exercises -> ExercisesScreen()
                    Screen.Logger -> LoggerScreen()
                }
            }
        }
    }
}
