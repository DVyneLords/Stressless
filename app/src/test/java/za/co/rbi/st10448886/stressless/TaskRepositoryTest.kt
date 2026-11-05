package za.co.rbi.st10448886.stressless

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TaskRepository and Task model logic.
 * These run on the JVM (host machine) — no Android device needed.
 */
class TaskRepositoryTest {

    @Before
    fun setUp() {
        TaskRepository.tasks.clear()
        TaskRepository.notifications.clear()
    }

    @After
    fun tearDown() {
        TaskRepository.tasks.clear()
        TaskRepository.notifications.clear()
    }

    @Test
    fun addTask_increasesListSize() {
        val before = TaskRepository.tasks.size
        TaskRepository.addTask(Task(title = "Test Task"))
        assertEquals(before + 1, TaskRepository.tasks.size)
    }

    @Test
    fun addTask_createsNotification() {
        TaskRepository.addTask(Task(title = "Notify Me"))
        assertTrue(TaskRepository.notifications.isNotEmpty())
        assertEquals("Task Added", TaskRepository.notifications.first().title)
    }

    @Test
    fun updateTask_replacesExistingTask() {
        val task = Task(title = "Original")
        TaskRepository.addTask(task)
        val updated = task.copy(title = "Updated", status = "Completed")
        TaskRepository.updateTask(updated)

        val stored = TaskRepository.tasks.first { it.id == task.id }
        assertEquals("Updated", stored.title)
        assertEquals("Completed", stored.status)
    }

    @Test
    fun updateTask_toCompleted_addsCompletionNotification() {
        val task = Task(title = "Finish me")
        TaskRepository.addTask(task)
        TaskRepository.notifications.clear() // remove "added" notification

        TaskRepository.updateTask(task.copy(status = "Completed"))
        assertTrue(TaskRepository.notifications.any { it.title == "Task Completed" })
    }

    @Test
    fun deleteTask_removesFromList() {
        val task = Task(title = "Delete me")
        TaskRepository.addTask(task)
        TaskRepository.deleteTask(task.id)
        assertNull(TaskRepository.tasks.find { it.id == task.id })
    }

    @Test
    fun subtaskProgress_calculatesCorrectly() {
        val task = Task(
            subtasks = listOf(
                Subtask(title = "a", done = true),
                Subtask(title = "b", done = false),
                Subtask(title = "c", done = true)
            )
        )
        assertEquals(2f / 3f, task.subtaskProgress, 0.001f)
    }

    @Test
    fun subtaskProgress_isZeroWhenEmpty() {
        val task = Task()
        assertEquals(0f, task.subtaskProgress, 0.001f)
    }

    @Test
    fun markAllNotificationsRead_marksEveryNotification() {
        TaskRepository.addNotification("A", "msg")
        TaskRepository.addNotification("B", "msg")
        TaskRepository.markAllNotificationsRead()
        assertTrue(TaskRepository.notifications.all { it.read })
    }

    @Test
    fun strings_returnsEnglishByDefault() {
        assertEquals("Add Task", Strings.tr("add_task", "English"))
    }

    @Test
    fun strings_returnsAfrikaansWhenSelected() {
        assertEquals("Voeg Taak By", Strings.tr("add_task", "Afrikaans"))
    }

    @Test
    fun strings_fallsBackToKeyWhenMissing() {
        assertEquals("unknown_key", Strings.tr("unknown_key", "English"))
    }
}