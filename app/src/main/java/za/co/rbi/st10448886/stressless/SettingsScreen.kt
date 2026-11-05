package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings screen — allows the user to change language, dark mode,
 * notification preferences, and log out.
 *
 * All toggles here write directly into TaskRepository's mutableState
 * properties, so changes apply instantly across the whole app (e.g.
 * switching language immediately re-renders every Strings.tr() call).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit, onNavigate: (String) -> Unit) {
    val language = TaskRepository.language.value

    Scaffold(
        topBar = { TopAppBar(title = { Text(Strings.tr("settings", language)) }) },
        bottomBar = { StresslessBottomBar(current = "settings", onNavigate = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(Strings.tr("preferences", language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Language picker — three supported languages as filter chips
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(Strings.tr("language", language), modifier = Modifier.weight(1f))
                Row {
                    listOf("English", "Afrikaans", "isiZulu").forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { TaskRepository.language.value = lang },
                            label = { Text(lang) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            // Dark mode toggle — read by StresslessTheme in MainActivity
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(Strings.tr("dark_mode", language), modifier = Modifier.weight(1f))
                Switch(checked = TaskRepository.darkMode.value, onCheckedChange = { TaskRepository.darkMode.value = it })
            }

            Spacer(Modifier.height(16.dp))
            Text(Strings.tr("notifications", language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Master switch — checked by TaskFormScreen before scheduling any reminder
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(Strings.tr("push_notifications", language), modifier = Modifier.weight(1f))
                Switch(checked = TaskRepository.notificationsEnabled.value, onCheckedChange = { TaskRepository.notificationsEnabled.value = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(Strings.tr("due_date_reminders", language), modifier = Modifier.weight(1f))
                Switch(checked = TaskRepository.dueDateRemindersEnabled.value, onCheckedChange = { TaskRepository.dueDateRemindersEnabled.value = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(Strings.tr("daily_summary", language), modifier = Modifier.weight(1f))
                Switch(checked = TaskRepository.dailySummaryEnabled.value, onCheckedChange = { TaskRepository.dailySummaryEnabled.value = it })
            }

            Spacer(Modifier.height(16.dp))
            Text(Strings.tr("other", language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            // "About" navigates to the About screen
            TextButton(onClick = { onNavigate("about") }) { Text(Strings.tr("about", language)) }

            Spacer(Modifier.height(24.dp))
            // Signs out of Firebase, clears local state, and returns to onboarding
            // with the entire back stack wiped (handled in MainActivity's nav host)
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text(Strings.tr("logout", language))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}