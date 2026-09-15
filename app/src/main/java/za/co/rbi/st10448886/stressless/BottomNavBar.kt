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

@Composable
fun StresslessBottomBar(current: String, onNavigate: (String) -> Unit) {
    val language = TaskRepository.language.value
    NavigationBar {
        NavigationBarItem(
            selected = current == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(Strings.tr("home", language)) }
        )
        NavigationBarItem(
            selected = current == "calendar",
            onClick = { onNavigate("calendar") },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            label = { Text(Strings.tr("calendar", language)) }
        )
        NavigationBarItem(
            selected = current == "stats",
            onClick = { onNavigate("stats") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text(Strings.tr("stats", language)) }
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text(Strings.tr("profile", language)) }
        )
    }
}
