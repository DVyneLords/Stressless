package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Maps a task's priority label to the small colour dot shown on its card. */
fun priorityColor(priority: String): Color = when (priority) {
    "High" -> Color(0xFFE57373)
    "Low" -> Color(0xFF81C784)
    else -> Color(0xFFFFD54F) // Medium (default)
}

/**
 * DashboardScreen — "My Tasks" home screen. Shows filter chips (All /
 * Pending / In Progress / Completed) with live counts, and a scrollable
 * list of tasks matching the selected filter. Tapping + opens the add-task
 * form; tapping a task card opens its details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onAddTask: () -> Unit, onOpenTask: (String) -> Unit, onNavigate: (String) -> Unit) {
    val language = TaskRepository.language.value
    val tasks = TaskRepository.tasks
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    // Which status filter chip is currently active
    var statusFilter by remember { mutableStateOf("All") }

    // Counts shown on each filter chip — recomputed on every recomposition
    // since `tasks` is a Compose state list (cheap for typical task-list sizes)
    val pending = tasks.count { it.status == "Pending" }
    val inProgress = tasks.count { it.status == "In Progress" }
    val completed = tasks.count { it.status == "Completed" }

    val filtered = when (statusFilter) {
        "Pending" -> tasks.filter { it.status == "Pending" }
        "In Progress" -> tasks.filter { it.status == "In Progress" }
        "Completed" -> tasks.filter { it.status == "Completed" }
        else -> tasks
    }
    val unreadCount = TaskRepository.notifications.count { !it.read }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${Strings.tr("good_morning", language)}, ${TaskRepository.displayName}!", style = MaterialTheme.typography.titleMedium)
                        Text(Strings.tr("stay_focused", language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    // Notification bell with an unread-count badge
                    BadgedBox(badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } }) {
                        IconButton(onClick = { onNavigate("notifications") }) {
                            Icon(Icons.Default.Notifications, contentDescription = Strings.tr("notifications", language))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) { Icon(Icons.Default.Add, contentDescription = Strings.tr("add_task", language)) }
        },
        bottomBar = { StresslessBottomBar(current = "dashboard", onNavigate = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Status filter chips, each labelled with its live count
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "All" to tasks.size,
                    "Pending" to pending,
                    "In Progress" to inProgress,
                    "Completed" to completed
                ).forEach { (label, count) ->
                    FilterChip(
                        selected = statusFilter == label,
                        onClick = { statusFilter = label },
                        // Translate the filter label via its string-map key
                        // (spaces -> underscores, lowercased, e.g. "In Progress" -> "in_progress")
                        label = { Text("${Strings.tr(label.lowercase().replace(" ", "_"), language)} $count") }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                // Empty state — shown for a fresh account or an empty filter result
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks yet — tap + to add one", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { task ->
                        TaskCard(task, dateFormat, onOpenTask)
                    }
                }
            }
        }
    }
}

/**
 * TaskCard — reusable row used on Dashboard and Calendar screens.
 * Shows a completion checkbox, priority dot, title/description/due date,
 * and a delete action. Tapping the card body opens task details.
 */
@Composable
fun TaskCard(task: Task, dateFormat: SimpleDateFormat, onOpenTask: (String) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = { onOpenTask(task.id) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick-complete checkbox — toggling writes straight through
            // TaskRepository.updateTask (which also syncs to Firestore)
            Checkbox(
                checked = task.status == "Completed",
                onCheckedChange = { checked ->
                    TaskRepository.updateTask(task.copy(status = if (checked) "Completed" else "Pending"))
                }
            )
            Box(modifier = Modifier.size(10.dp).background(priorityColor(task.priority), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                if (task.dueDate > 0) {
                    Text(dateFormat.format(Date(task.dueDate)), style = MaterialTheme.typography.labelSmall)
                }
            }
            // Delete button — removes locally and from Firestore (or queues offline delete)
            IconButton(onClick = { TaskRepository.deleteTask(task.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}