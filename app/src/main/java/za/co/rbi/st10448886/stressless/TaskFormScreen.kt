package za.co.rbi.st10448886.stressless

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * TaskFormScreen — single screen used for both creating a new task
 * (taskId == "new") and editing an existing one (taskId matches a real
 * task's id). Pre-fills fields from the existing task when editing.
 * Also lets the user attach a photo, which is compressed and stored as a
 * Base64 blob directly inside the Firestore task document (see ImageUtils) —
 * this stands in for paid Firebase Cloud Storage, which this project isn't using.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(taskId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val language = TaskRepository.language.value
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isNew = taskId == "new"
    // Look up the task being edited (null when creating a new one)
    val existing = if (!isNew) TaskRepository.tasks.find { it.id == taskId } else null

    // Form field state, pre-filled from `existing` when editing
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

    // Photo attachment state. Holds the Base64 string that will actually
    // be saved (starts pre-filled from the existing task when editing), plus
    // a decoded Bitmap kept only for the on-screen preview.
    var imageBase64 by remember { mutableStateOf(existing?.imageBase64 ?: "") }
    var previewBitmap by remember {
        mutableStateOf<Bitmap?>(
            existing?.imageBase64?.takeIf { it.isNotBlank() }?.let { ImageUtils.base64ToBitmap(it) }
        )
    }

    // Launches the system photo picker; on a result, compress + encode
    // the chosen image and update both the preview and the value that gets saved.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageUtils.uriToCompressedBase64(context, uri)
            if (encoded != null) {
                imageBase64 = encoded
                previewBitmap = ImageUtils.base64ToBitmap(encoded)
            }
        }
    }

    /** Builds a Task from current form state and saves it (create or update), then schedules a reminder if applicable. */
    fun save() {
        // Parse the typed date string; falls back to 0 (no due date) on any bad input
        // rather than crashing the form on an invalid/incomplete date.
        val millis = try { dateFormat.parse(dueDateText)?.time ?: 0L } catch (_: Exception) { 0L }
        val task = Task(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title, description = description,
            dueDate = millis, priority = priority, category = category,
            status = status, reminder = reminder,
            imageBase64 = imageBase64,
            // Preserve existing subtasks — this form doesn't edit them directly (TaskDetailsScreen does)
            subtasks = existing?.subtasks ?: emptyList()
        )
        if (isNew) TaskRepository.addTask(task) else TaskRepository.updateTask(task)
        // Only schedule a reminder if there's a valid due date AND the user
        // hasn't globally disabled notifications in Settings
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
                    // Save is disabled until a title is provided — the only required field
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
            // Photo attachment — shows the picked/existing image, or a
            // placeholder "add photo" box when none is set yet.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Task photo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                    // Small remove button in the corner
                    IconButton(
                        onClick = { imageBase64 = ""; previewBitmap = null },
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Add photo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(Strings.tr("title", language)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(Strings.tr("description", language)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            // Free-text date field (expects yyyy-MM-dd) — kept simple rather than
            // wiring up a full date picker dialog
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

            // Status chips only shown when editing — a brand-new task always starts as "Pending"
            if (!isNew) {
                Text(Strings.tr("status", language), modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Pending", "In Progress", "Completed").forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Reminder lead-time dropdown (read-only text field + menu, standard Material3 pattern)
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