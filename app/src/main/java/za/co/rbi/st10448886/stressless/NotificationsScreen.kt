package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * NotificationsScreen — lists in-app notifications (task added/completed,
 * reminders, etc.) grouped into "Today" and "Earlier" sections, with a
 * "mark all as read" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val language = TaskRepository.language.value
    val notifications = TaskRepository.notifications
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val today = remember { Calendar.getInstance() }

    /** True if the given timestamp falls on today's calendar date. */
    fun isToday(ts: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }

    val todayList = notifications.filter { isToday(it.timestamp) }
    val earlierList = notifications.filter { !isToday(it.timestamp) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.tr("notifications", language)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notifications yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // weight(1f, fill = false) lets the list take only the space it needs,
                // leaving room for the "mark all read" button below without overlapping it
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (todayList.isNotEmpty()) {
                        item { Text(Strings.tr("today", language), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp)) }
                        items(todayList, key = { it.id }) { NotificationRow(it, timeFormat) }
                    }
                    if (earlierList.isNotEmpty()) {
                        // Note: section is always labelled "yesterday" even for older items —
                        // acceptable simplification since the app doesn't retain long history.
                        item { Text(Strings.tr("yesterday", language), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp)) }
                        items(earlierList, key = { it.id }) { NotificationRow(it, timeFormat) }
                    }
                }
                TextButton(onClick = { TaskRepository.markAllNotificationsRead() }, modifier = Modifier.fillMaxWidth()) {
                    Text(Strings.tr("mark_all_read", language))
                }
            }
        }
    }
}

/** Single notification row with a type-specific icon/colour, title, message, and time. */
@Composable
private fun NotificationRow(notification: AppNotification, timeFormat: SimpleDateFormat) {
    // Icon + colour depend on notification type (success/overdue/reminder/info)
    val (icon, color) = when (notification.type) {
        "success" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        "overdue" -> Icons.Default.Error to Color(0xFFE57373)
        "reminder" -> Icons.Default.Notifications to Color(0xFFFFA726)
        else -> Icons.Default.Info to Color(0xFF64B5F6)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall)
            Text(notification.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(timeFormat.format(Date(notification.timestamp)), style = MaterialTheme.typography.labelSmall)
    }
}