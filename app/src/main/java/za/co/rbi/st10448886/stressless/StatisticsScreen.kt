package za.co.rbi.st10448886.stressless

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val TAG = "StatisticsScreen"

/**
 * Statistics screen — shows a summary of the user's tasks:
 *  - Total / Completed / In Progress / Pending counts
 *  - Donut chart of completion rate
 *  - Bar chart of tasks grouped by priority
 *
 * All stats are computed live from TaskRepository.tasks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onNavigate: (String) -> Unit) {
    val language = TaskRepository.language.value
    val tasks = TaskRepository.tasks

    val total = tasks.size
    val completed = tasks.count { it.status == "Completed" }
    val inProgress = tasks.count { it.status == "In Progress" }
    val pending = tasks.count { it.status == "Pending" }
    val rate = if (total > 0) completed.toFloat() / total else 0f

    val high = tasks.count { it.priority == "High" }
    val medium = tasks.count { it.priority == "Medium" }
    val low = tasks.count { it.priority == "Low" }
    val maxPriority = maxOf(high, medium, low, 1)

    // Use theme colours so dark mode works correctly
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Log.d(TAG, "Stats: total=$total completed=$completed inProgress=$inProgress pending=$pending rate=$rate")

    Scaffold(
        topBar = { TopAppBar(title = { Text(Strings.tr("statistics", language)) }) },
        bottomBar = { StresslessBottomBar(current = "stats", onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ---------- Task Overview ----------
            Text(Strings.tr("task_overview", language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(Strings.tr("total_tasks", language), total.toString(), Modifier.weight(1f))
                StatBox(Strings.tr("completed", language), completed.toString(), Modifier.weight(1f))
                StatBox(Strings.tr("in_progress", language), inProgress.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            StatBox(Strings.tr("pending", language), pending.toString(), Modifier.fillMaxWidth())

            // ---------- Completion Rate (Donut) ----------
            Spacer(Modifier.height(24.dp))
            Text(Strings.tr("completion_rate", language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val stroke = 14.dp.toPx()

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke),
                        size = Size(size.width - stroke, size.height - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2)
                    )

                    // Foreground progress arc (uses theme primary — works in light AND dark mode)
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * rate,
                        useCenter = false,
                        style = Stroke(width = stroke),
                        size = Size(size.width - stroke, size.height - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2)
                    )
                }
                Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$completed of $total tasks completed",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---------- Tasks by Priority ----------
            Spacer(Modifier.height(24.dp))
            Text(Strings.tr("tasks_by_priority", language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            PriorityBar("High", high, maxPriority, Color(0xFFE57373))
            Spacer(Modifier.height(8.dp))
            PriorityBar("Medium", medium, maxPriority, Color(0xFFFFD54F))
            Spacer(Modifier.height(8.dp))
            PriorityBar("Low", low, maxPriority, Color(0xFF81C784))

            // Bottom padding so content is not cut off by the nav bar
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PriorityBar(label: String, count: Int, max: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(64.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (max > 0) count.toFloat() / max else 0f)
                    .height(14.dp)
                    .background(color, RoundedCornerShape(7.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(count.toString())
    }
}