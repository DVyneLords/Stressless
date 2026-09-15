package za.co.rbi.st10448886.stressless

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * FirestoreRepository — handles all cloud database operations for tasks.
 *
 * Firestore structure:
 *   users/{uid}/tasks/{taskId}  -> Task document
 *
 * This class is the "API layer" of the app. Every task CRUD operation
 * goes through here, and is mirrored to the local TaskRepository
 * for offline viewing.
 *
 * References:
 *  - Firestore docs: https://firebase.google.com/docs/firestore
 */
object FirestoreRepository {

    private const val TAG = "FirestoreRepo"

    // Nullable + wrapped in try/catch: FirebaseFirestore/FirebaseAuth.getInstance()
    // throws if no FirebaseApp is initialized, which is the case in plain JVM
    // unit tests (no Android runtime, no google-services setup). Falling back
    // to null lets tests run without crashing; the real app always has
    // Firebase initialized, so this behaves exactly as before there.
    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore unavailable (expected in plain JVM unit tests)", e)
            null
        }

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable (expected in plain JVM unit tests)", e)
            null
        }

    // The signed-in user's task subcollection, or null if not logged in /
    // Firebase unavailable. Every read/write below goes through this.
    private val tasksCollection
        get() = auth?.currentUser?.uid?.let { uid ->
            db?.collection("users")?.document(uid)?.collection("tasks")
        }

    /** Convert a Task to a Map for Firestore (subtasks become list of maps). */
    private fun Task.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "dueDate" to dueDate,
        "priority" to priority,
        "status" to status,
        "category" to category,
        "reminder" to reminder,
        "subtasks" to subtasks.map { mapOf("id" to it.id, "title" to it.title, "done" to it.done) },
        // Server-side-friendly timestamp for last write; useful for future
        // conflict resolution or "last synced" display.
        "updatedAt" to System.currentTimeMillis()
    )

    /** Rebuild a Task from a raw Firestore document map, with safe defaults for any missing field. */
    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toTask(): Task = Task(
        id = this["id"] as? String ?: "",
        title = this["title"] as? String ?: "",
        description = this["description"] as? String ?: "",
        dueDate = (this["dueDate"] as? Number)?.toLong() ?: 0L,
        priority = this["priority"] as? String ?: "Medium",
        status = this["status"] as? String ?: "Pending",
        category = this["category"] as? String ?: "",
        reminder = this["reminder"] as? String ?: "On time",
        subtasks = (this["subtasks"] as? List<Map<String, Any?>>)?.mapNotNull { m ->
            val sid = m["id"] as? String ?: return@mapNotNull null
            Subtask(
                id = sid,
                title = m["title"] as? String ?: "",
                done = m["done"] as? Boolean ?: false
            )
        } ?: emptyList()
    )

    /**
     * Create or update a task in Firestore.
     * Uses SetOptions.merge() so partial writes never wipe out fields
     * that aren't included in this particular update.
     */
    suspend fun saveTask(task: Task): Result<Unit> = try {
        val col = tasksCollection ?: return Result.failure(Exception("Not logged in"))
        col.document(task.id).set(task.toMap(), SetOptions.merge()).await()
        Log.d(TAG, "Saved task ${task.id} to Firestore")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "saveTask failed", e)
        Result.failure(e)
    }

    /** Delete a task from Firestore. */
    suspend fun deleteTask(taskId: String): Result<Unit> = try {
        val col = tasksCollection ?: return Result.failure(Exception("Not logged in"))
        col.document(taskId).delete().await()
        Log.d(TAG, "Deleted task $taskId from Firestore")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "deleteTask failed", e)
        Result.failure(e)
    }

    /** Fetch all tasks for the current user (used on login and reconnect). */
    suspend fun loadTasks(): Result<List<Task>> = try {
        // Not logged in -> treat as "no tasks" rather than an error
        val col = tasksCollection ?: return Result.success(emptyList())
        val snapshot = col.get().await()
        val list = snapshot.documents.mapNotNull { it.data?.toTask() }
        Log.d(TAG, "Loaded ${list.size} tasks from Firestore")
        Result.success(list)
    } catch (e: Exception) {
        Log.e(TAG, "loadTasks failed", e)
        Result.failure(e)
    }
}