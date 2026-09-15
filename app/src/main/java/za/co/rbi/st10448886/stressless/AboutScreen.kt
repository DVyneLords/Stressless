package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * AboutScreen — static informational screen showing the app name, tagline,
 * a short description, a hard-coded feature list, and version/developer info.
 * Reached from Settings > About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    // Current language drives all Strings.tr() lookups on this screen so it
    // reacts immediately if the user changes language in Settings.
    val language = TaskRepository.language.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.tr("about_title", language)) },
                navigationIcon = {
                    // Back arrow returns to whichever screen navigated here (usually Settings)
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                // Scrollable in case of long translated strings or small screens
                .verticalScroll(rememberScrollState())
        ) {
            // App icon / branding
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.padding(top = 32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            Text(
                Strings.tr("app_name", language),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            Text(
                Strings.tr("tagline", language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Text(
                Strings.tr("about_description", language),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))

            Text(
                "Features:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            // Static feature list — intentionally not translated via Strings.tr()
            // since this is a plain informational bullet list, not core UI text.
            Text("• Task Management with status tracking (Pending, In Progress, Completed)")
            Text("• Subtasks with progress tracking")
            Text("• Calendar view for due dates")
            Text("• Statistics and completion rates")
            Text("• Push notifications and reminders")
            Text("• Cloud backup and sync")
            Text("• Multi-language support (English, Afrikaans, Zulu, Xhosa, Sotho)")
            Text("• Dark mode")

            Spacer(Modifier.height(24.dp))

            Text(
                Strings.tr("about_version", language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            Text(
                Strings.tr("about_developer", language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Extra bottom padding so the last line isn't flush against the screen edge
            Spacer(Modifier.height(48.dp))
        }
    }
}