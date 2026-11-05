package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TaskDetailsScreen — full view of a single task: photo (if attached),
 * status, description, due date, priority, category, subtask progress bar,
 * an editable subtask checklist, and quick actions to edit, delete, or mark
 * the task complete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(taskId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val language = TaskRepository.language.value
    val task = TaskRepository.tasks.find { it.id == taskId }
    // Defensive guard: if the task was deleted elsewhere (or a stale ID was
    // passed) while this screen is being composed, back out safely rather
    // than crash on a null task below.
    if (task == null) {
        onBack()
        return
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var newSubtask by remember { mutableStateOf("") }

    // NEW: decode the stored Base64 photo (if any) once per composition of this task
    val photoBitmap = remember(task.imageBase64) {
        task.imageBase64.takeIf { it.isNotBlank() }?.let { ImageUtils.base64ToBitmap(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    // Delete from the details screen: removes the task, then
                    // navigates back since there's nothing left to display
                    IconButton(onClick = { TaskRepository.deleteTask(task.id); onBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // NEW: photo banner, only shown when the task has one attached
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Task photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }

            AssistChip(onClick = {}, label = { Text(task.status) })
            Spacer(Modifier.height(12.dp))
            if (task.description.isNotBlank()) {
                Text(task.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
            }
            if (task.dueDate > 0) {
                Text("${Strings.tr("due_date", language)}: ${dateFormat.format(Date(task.dueDate))}")
                Spacer(Modifier.height(4.dp))
            }
            Text("${Strings.tr("priority", language)}: ${task.priority}")
            if (task.category.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("${Strings.tr("category", language)}: ${task.category}")
            }
            Spacer(Modifier.height(16.dp))

            // Progress bar only shown once there's at least one subtask to track
            if (task.subtasks.isNotEmpty()) {
                Text(Strings.tr("progress", language) + ": ${(task.subtaskProgress * 100).toInt()}%")
                LinearProgressIndicator(progress = { task.subtaskProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            Text(Strings.tr("subtasks", language), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(task.subtasks, key = { it.id }) { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = sub.done,
                            onCheckedChange = { checked ->
                                val updated = task.subtasks.map { if (it.id == sub.id) it.copy(done = checked) else it }
                                TaskRepository.updateTask(task.copy(subtasks = updated))
                            }
                        )
                        Text(sub.title, modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = newSubtask, onValueChange = { newSubtask = it },
                    label = { Text("New subtask") }, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    if (newSubtask.isNotBlank()) {
                        TaskRepository.updateTask(task.copy(subtasks = task.subtasks + Subtask(title = newSubtask.trim())))
                        newSubtask = ""
                    }
                }) { Text("Add") }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(Strings.tr("edit_task", language))
                }
                Button(
                    onClick = { TaskRepository.updateTask(task.copy(status = "Completed")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.tr("mark_complete", language))
                }
            }
        }
    }
}