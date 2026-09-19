package com.hevyclone.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CalendarMonth
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
    data object HistoryTab : Screen
    data object RoutinesTab : Screen
    data object ExercisesTab : Screen
    data object ProfileTab : Screen
    data class WorkoutDetail(val id: Long) : Screen
    data class ExerciseDetail(val name: String) : Screen
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
        TabDef(Screen.HomeTab, "Home", Icons.Rounded.Home),
        TabDef(Screen.HistoryTab, "History", Icons.Rounded.CalendarMonth),
        TabDef(Screen.RoutinesTab, "Training", Icons.Rounded.Assignment),
        TabDef(Screen.ExercisesTab, "Exercises", Icons.Rounded.FitnessCenter),
        TabDef(Screen.ProfileTab, "Profile", Icons.Rounded.Person),
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
                            label = { Text(t.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
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
            when (val s = Nav.current) {
                Screen.HomeTab -> HomeScreen()
                Screen.HistoryTab -> HistoryScreen()
                Screen.RoutinesTab -> RoutinesScreen()
                Screen.ExercisesTab -> ExercisesScreen()
                Screen.ProfileTab -> ProfileScreen()
                is Screen.WorkoutDetail -> WorkoutDetailScreen(s.id)
                is Screen.ExerciseDetail -> ExerciseDetailScreen(s.name)
                Screen.Logger -> LoggerScreen()
            }
        }
    }
}
