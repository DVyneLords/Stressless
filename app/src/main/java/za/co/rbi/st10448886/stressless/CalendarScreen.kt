package za.co.rbi.st10448886.stressless

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * CalendarScreen — month-grid calendar view of tasks.
 * Tapping a day filters the task list below to that day's tasks;
 * with no day selected, it shows all upcoming tasks sorted by due date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onOpenTask: (String) -> Unit, onNavigate: (String) -> Unit) {
    val language = TaskRepository.language.value
    val tasks = TaskRepository.tasks
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    // Currently tapped day (null = no day selected, show "all upcoming" view instead)
    var selectedDay by remember { mutableStateOf<Calendar?>(null) }
    // Which month the grid is currently showing (changes via the chevron buttons)
    var visibleMonth by remember { mutableStateOf(Calendar.getInstance()) }

    // Tasks whose due date falls on the selected day (empty list if nothing selected)
    val dayTasks = selectedDay?.let { day -> tasks.filter { it.dueDate > 0 && isSameDay(it.dueDate, day) } } ?: emptyList()

    Scaffold(
        topBar = { TopAppBar(title = { Text(Strings.tr("calendar", language)) }) },
        bottomBar = { StresslessBottomBar(current = "calendar", onNavigate = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            MonthCalendar(
                tasks = tasks, visibleMonth = visibleMonth, selectedDay = selectedDay,
                onMonthChange = { visibleMonth = it }, onDaySelected = { selectedDay = it }
            )
            Spacer(Modifier.height(16.dp))

            if (selectedDay != null) {
                // A specific day is selected: show only that day's tasks
                Text(dateFormat.format(selectedDay!!.time), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (dayTasks.isEmpty()) {
                    Text("No tasks on this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn { items(dayTasks, key = { it.id }) { task -> TaskCard(task, dateFormat, onOpenTask) } }
                }
            } else {
                // No day selected: default view lists every task with a due date, soonest first
                Text(Strings.tr("view_all_events", language), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val upcoming = tasks.filter { it.dueDate > 0 }.sortedBy { it.dueDate }
                LazyColumn { items(upcoming, key = { it.id }) { task -> TaskCard(task, dateFormat, onOpenTask) } }
            }
        }
    }
}

/** True if [millis] (epoch time) falls on the same calendar day as [day]. */
fun isSameDay(millis: Long, day: Calendar): Boolean {
    val d = Calendar.getInstance().apply { timeInMillis = millis }
    return d.get(Calendar.YEAR) == day.get(Calendar.YEAR) && d.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
}

/**
 * Renders a single month as a 7-column grid (Sun–Sat), with a dot marker
 * under any day that has at least one task due, and highlights the
 * currently selected day.
 */
@Composable
fun MonthCalendar(
    tasks: List<Task>, visibleMonth: Calendar, selectedDay: Calendar?,
    onMonthChange: (Calendar) -> Unit, onDaySelected: (Calendar) -> Unit
) {
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    // Precompute the set of (day-of-month, month) pairs that have a task due,
    // so the grid loop below can do an O(1) lookup per cell instead of
    // re-scanning the whole task list for every day drawn.
    val daysWithTasks = remember(tasks, visibleMonth) {
        tasks.filter { it.dueDate > 0 }.map {
            Calendar.getInstance().apply { timeInMillis = it.dueDate }.get(Calendar.DAY_OF_MONTH) to
                    Calendar.getInstance().apply { timeInMillis = it.dueDate }.get(Calendar.MONTH)
        }.toSet()
    }

    Column {
        // Month header with prev/next navigation
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { val cal = visibleMonth.clone() as Calendar; cal.add(Calendar.MONTH, -1); onMonthChange(cal) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(monthFormat.format(visibleMonth.time), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { val cal = visibleMonth.clone() as Calendar; cal.add(Calendar.MONTH, 1); onMonthChange(cal) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
        Spacer(Modifier.height(8.dp))

        // Day-of-week header row (Sun..Sat)
        val firstOfMonth = (visibleMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        // How many empty cells to pad before day 1 (Calendar.DAY_OF_WEEK is 1-based, Sunday=1)
        val startOffset = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))

        // Build the grid row by row: leading/trailing cells are blank spacers,
        // real day cells are clickable boxes that select that day.
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        var day = 1
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < startOffset || day > daysInMonth) {
                        // Blank padding cell (before day 1 or after the last day of the month)
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val thisDay = day
                        val cal = (visibleMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, thisDay) }
                        val isSelected = selectedDay != null &&
                                selectedDay.get(Calendar.DAY_OF_MONTH) == thisDay &&
                                selectedDay.get(Calendar.MONTH) == visibleMonth.get(Calendar.MONTH)
                        val hasTask = daysWithTasks.contains(thisDay to visibleMonth.get(Calendar.MONTH))

                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onDaySelected(cal) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(thisDay.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                // Small dot indicator under any day with a task due
                                if (hasTask) {
                                    Box(
                                        modifier = Modifier.size(4.dp).background(
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    )
                                }
                            }
                        }
                        day++
                    }
                }
            }
        }
    }
}