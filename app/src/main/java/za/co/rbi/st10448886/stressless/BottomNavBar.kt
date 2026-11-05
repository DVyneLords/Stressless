package za.co.rbi.st10448886.stressless

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * StresslessBottomBar — shared bottom navigation bar used by Dashboard,
 * Calendar, Statistics, and Settings.
 *
 * @param current the route key of the screen currently shown (e.g. "dashboard"),
 *                used to highlight the matching tab as selected.
 * @param onNavigate callback fired with the target route key when a tab is tapped.
 */
@Composable
fun StresslessBottomBar(current: String, onNavigate: (String) -> Unit) {
    val language = TaskRepository.language.value
    NavigationBar {
        // Home tab -> My Tasks / Dashboard
        NavigationBarItem(
            selected = current == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(Strings.tr("home", language)) }
        )
        // Calendar tab -> tasks grouped by due date
        NavigationBarItem(
            selected = current == "calendar",
            onClick = { onNavigate("calendar") },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            label = { Text(Strings.tr("calendar", language)) }
        )
        // Stats tab -> completion rate + priority breakdown
        NavigationBarItem(
            selected = current == "stats",
            onClick = { onNavigate("stats") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text(Strings.tr("stats", language)) }
        )
        // Profile tab -> Settings screen (labelled "Profile" in the UI)
        NavigationBarItem(
            selected = current == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text(Strings.tr("profile", language)) }
        )
    }
}