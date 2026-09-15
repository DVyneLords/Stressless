package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(taskId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val language = TaskRepository.language.value
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isNew = taskId == "new"
    val existing = if (!isNew) TaskRepository.tasks.find { it.id == taskId } else null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var dueDateText by remember {
        mutableStateOf(if (existing != null && existing.dueDate > 0) dateFormat.format(Date(existing.dueDate)) else "")
    }
    var priority by remember { mutableStateOf(existing?.priority ?: "Medium") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "Pending") }
    var reminder by remember { mutableStateOf(existing?.reminder ?: "On time") }
    var reminderMenuOpen by remember { mutableStateOf(false) }

    fun save() {
        val millis = try { dateFormat.parse(dueDateText)?.time ?: 0L } catch (_: Exception) { 0L }
        val task = Task(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title, description = description,
            dueDate = millis, priority = priority, category = category,
            status = status, reminder = reminder,
            subtasks = existing?.subtasks ?: emptyList()
        )
        if (isNew) TaskRepository.addTask(task) else TaskRepository.updateTask(task)
        if (millis > 0 && TaskRepository.notificationsEnabled.value) {
            NotificationHelper.scheduleReminder(context, task)
        }
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) Strings.tr("add_task", language) else Strings.tr("edit_task", language)) },
                actions = {
                    TextButton(onClick = { save() }, enabled = title.isNotBlank()) {
                        Text(Strings.tr("save", language))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(Strings.tr("title", language)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(Strings.tr("description", language)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = dueDateText, onValueChange = { dueDateText = it }, label = { Text(Strings.tr("due_date", language)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(Strings.tr("category", language)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            Text(Strings.tr("priority", language), modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("High", "Medium", "Low").forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) }, modifier = Modifier.padding(end = 8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            if (!isNew) {
                Text(Strings.tr("status", language), modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Pending", "In Progress", "Completed").forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            ExposedDropdownMenuBox(expanded = reminderMenuOpen, onExpandedChange = { reminderMenuOpen = it }) {
                OutlinedTextField(
                    value = reminder, onValueChange = {}, readOnly = true,
                    label = { Text(Strings.tr("reminder", language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderMenuOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                DropdownMenu(
                    expanded = reminderMenuOpen,
                    onDismissRequest = { reminderMenuOpen = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    listOf("On time", "30 mins before", "1 hour before", "1 day before", "None").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { reminder = option; reminderMenuOpen = false })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}