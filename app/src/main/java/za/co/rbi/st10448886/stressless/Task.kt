package za.co.rbi.st10448886.stressless

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "TaskRepository"

// ---------- Data models ----------

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val done: Boolean = false
)

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val dueDate: Long = 0L,              // epoch millis
    val priority: String = "Medium",     // High / Medium / Low
    val status: String = "Pending",      // Pending / In Progress / Completed
    val category: String = "",
    val reminder: String = "On time",    // On time / 30 mins before / 1 hour before / 1 day before / None
    val subtasks: List<Subtask> = emptyList()
) {
    /** Percentage of subtasks that are marked done (0f..1f). */
    val subtaskProgress: Float
        get() = if (subtasks.isEmpty()) 0f else subtasks.count { it.done }.toFloat() / subtasks.size
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "info",   // info / success / reminder / overdue
    val read: Boolean = false
)

// ---------- Simple multi-language strings ----------

object Strings {
    private val en = mapOf(
        "app_name" to "Stressless",
        "tagline" to "Plan. Prioritize. Achieve.",
        "onboarding_title" to "Welcome to Stressless",
        "onboarding_body" to "Your smart companion for managing tasks, deadlines and academic life.",
        "get_started" to "Get Started",
        "already_have_account" to "I already have an account",
        "welcome_back" to "Welcome Back!",
        "login_continue" to "Login to continue",
        "create_account" to "Create Account",
        "join_today" to "Join Stressless today",
        "full_name" to "Full Name",
        "confirm_password" to "Confirm Password",
        "forgot_password" to "Forgot Password?",
        "or_continue_with" to "Or continue with",
        "good_morning" to "Good morning",
        "stay_focused" to "Stay focused and keep going.",
        "my_tasks" to "My Tasks", "add_task" to "Add Task", "title" to "Title",
        "description" to "Description", "due_date" to "Due date (YYYY-MM-DD)",
        "priority" to "Priority", "category" to "Category (optional)", "save" to "Save",
        "list" to "List", "calendar" to "Calendar", "settings" to "Settings",
        "language" to "Language", "dark_mode" to "Dark mode", "progress" to "Progress",
        "logout" to "Log out", "login" to "Log in", "register" to "Register",
        "email" to "Email", "password" to "Password", "no_account" to "Need an account? Register",
        "have_account" to "Already have an account? Log in",
        "all" to "All", "pending" to "Pending", "in_progress" to "In Progress", "completed" to "Completed",
        "reminder" to "Reminder", "subtasks" to "Subtasks", "edit_task" to "Edit Task",
        "mark_complete" to "Mark Complete", "stats" to "Stats", "profile" to "Profile",
        "home" to "Home", "notifications" to "Notifications", "view_all_events" to "View all events",
        "statistics" to "Statistics", "task_overview" to "Task Overview", "total_tasks" to "Total Tasks",
        "completion_rate" to "Completion Rate", "tasks_by_priority" to "Tasks by Priority",
        "mark_all_read" to "Mark all as read", "today" to "Today", "yesterday" to "Yesterday",
        "offline_title" to "You're Offline",
        "offline_body" to "No internet connection. You can view and edit your tasks offline. Changes will sync when you're back online.",
        "go_to_tasks" to "Go to My Tasks", "retry_connection" to "Retry Connection",
        "backup_sync" to "Backup & Sync", "export_data" to "Export Data", "about" to "About",
        "date_format" to "Date Format", "start_of_week" to "Start of Week", "theme" to "Theme",
        "push_notifications" to "Push Notifications", "due_date_reminders" to "Due Date Reminders",
        "daily_summary" to "Daily Summary", "preferences" to "Preferences", "other" to "Other",
        "status" to "Status",
        "about_title" to "About",
        "about_version" to "Version 1.0.0",
        "about_developer" to "Developed by Stack Masters",
        "about_description" to "Stressless is a student task tracker built for South African tertiary students. It helps you plan, prioritise and complete academic tasks with offline support, reminders and multi-language support."
    )
    private val af = mapOf(
        "app_name" to "Stressless",
        "tagline" to "Beplan. Prioritiseer. Bereik.",
        "onboarding_title" to "Welkom by Stressless",
        "onboarding_body" to "Jou slim metgesel vir die bestuur van take, sperdatums en akademiese lewe.",
        "get_started" to "Begin Nou",
        "already_have_account" to "Ek het reeds 'n rekening",
        "welcome_back" to "Welkom Terug!",
        "login_continue" to "Teken in om voort te gaan",
        "create_account" to "Skep Rekening",
        "join_today" to "Sluit vandag by Stressless aan",
        "full_name" to "Volle Naam",
        "confirm_password" to "Bevestig Wagwoord",
        "forgot_password" to "Wagwoord vergeet?",
        "or_continue_with" to "Of gaan voort met",
        "good_morning" to "Goeie môre",
        "stay_focused" to "Bly gefokus en hou aan.",
        "my_tasks" to "My Take", "add_task" to "Voeg Taak By", "title" to "Titel",
        "description" to "Beskrywing", "due_date" to "Sperdatum (JJJJ-MM-DD)",
        "priority" to "Prioriteit", "category" to "Kategorie (opsioneel)", "save" to "Stoor",
        "list" to "Lys", "calendar" to "Kalender", "settings" to "Instellings",
        "language" to "Taal", "dark_mode" to "Donker modus", "progress" to "Vordering",
        "logout" to "Teken uit", "login" to "Teken in", "register" to "Registreer",
        "email" to "E-pos", "password" to "Wagwoord", "no_account" to "Geen rekening? Registreer",
        "have_account" to "Het reeds 'n rekening? Teken in",
        "all" to "Almal", "pending" to "Hangend", "in_progress" to "Besig", "completed" to "Voltooi",
        "reminder" to "Herinnering", "subtasks" to "Subtake", "edit_task" to "Wysig Taak",
        "mark_complete" to "Merk as Voltooi", "stats" to "Statistieke", "profile" to "Profiel",
        "home" to "Tuis", "notifications" to "Kennisgewings", "view_all_events" to "Bekyk alle gebeurtenisse",
        "statistics" to "Statistieke", "task_overview" to "Taakoorsig", "total_tasks" to "Totale Take",
        "completion_rate" to "Voltooiingskoers", "tasks_by_priority" to "Take volgens Prioriteit",
        "mark_all_read" to "Merk alles as gelees", "today" to "Vandag", "yesterday" to "Gister",
        "offline_title" to "Jy is Vanlyn",
        "offline_body" to "Geen internetverbinding nie. Jy kan steeds jou take vanlyn bekyk en wysig. Veranderinge sal sinkroniseer wanneer jy weer aanlyn is.",
        "go_to_tasks" to "Gaan na My Take", "retry_connection" to "Probeer Weer",
        "backup_sync" to "Rugsteun & Sinkroniseer", "export_data" to "Voer Data Uit", "about" to "Meer Inligting",
        "date_format" to "Datumformaat", "start_of_week" to "Begin van Week", "theme" to "Tema",
        "push_notifications" to "Stootkennisgewings", "due_date_reminders" to "Sperdatum Herinneringe",
        "daily_summary" to "Daaglikse Opsomming", "preferences" to "Voorkeure", "other" to "Ander",
        "status" to "Status",
        "about_title" to "Meer Inligting",
        "about_version" to "Weergawe 1.0.0",
        "about_developer" to "Ontwikkel deur Stack Masters",
        "about_description" to "Stressless is 'n studentetaaknaspeurder wat vir Suid-Afrikaanse tersiêre studente gebou is. Dit help jou om akademiese take te beplan, te prioritiseer en te voltooi met vanlyn ondersteuning, herinneringe en veeltalige ondersteuning."
    )
    // NOTE: machine-drafted isiZulu — have a native speaker or your lecturer's
    // language resource sanity-check these before final submission.
    private val zu = mapOf(
        "app_name" to "Stressless",
        "tagline" to "Hlela. Beka isibaluleko. Finyelela.",
        "onboarding_title" to "Wamukelekile ku-Stressless",
        "onboarding_body" to "Umngane wakho ohlakaniphile wokuphatha imisebenzi, izikhathi zokugcina, kanye nempilo yezemfundo.",
        "get_started" to "Qala Manje",
        "already_have_account" to "Ngivele nginayo i-akhawunti",
        "welcome_back" to "Siyakwamukela Futhi!",
        "login_continue" to "Ngena ukuze uqhubeke",
        "create_account" to "Dala I-akhawunti",
        "join_today" to "Joyina i-Stressless namuhla",
        "full_name" to "Igama Eliphelele",
        "confirm_password" to "Qinisekisa Iphasiwedi",
        "forgot_password" to "Ukhohlwe iphasiwedi?",
        "or_continue_with" to "Noma qhubeka nge",
        "good_morning" to "Sawubona ekuseni",
        "stay_focused" to "Hlala ugxile futhi uqhubeke.",
        "my_tasks" to "Imisebenzi Yami", "add_task" to "Engeza Umsebenzi", "title" to "Isihloko",
        "description" to "Incazelo", "due_date" to "Usuku Lokugcina (YYYY-MM-DD)",
        "priority" to "Ukubaluleka", "category" to "Isigaba (okukhethekayo)", "save" to "Londoloza",
        "list" to "Uhlu", "calendar" to "Ikhalenda", "settings" to "Izilungiselelo",
        "language" to "Ulimi", "dark_mode" to "Imodi Emnyama", "progress" to "Inqubekelaphambili",
        "logout" to "Phuma", "login" to "Ngena", "register" to "Bhalisa",
        "email" to "I-imeyili", "password" to "Iphasiwedi", "no_account" to "Awunayo i-akhawunti? Bhalisa",
        "have_account" to "Usunayo i-akhawunti? Ngena",
        "all" to "Konke", "pending" to "Kusalindile", "in_progress" to "Kuyaqhubeka", "completed" to "Kuqediwe",
        "reminder" to "Isikhumbuzo", "subtasks" to "Imisebenzi Emincane", "edit_task" to "Hlela Umsebenzi",
        "mark_complete" to "Phawula Njengokuqediwe", "stats" to "Izibalo", "profile" to "Iphrofayela",
        "home" to "Ekhaya", "notifications" to "Izaziso", "view_all_events" to "Buka zonke izehlakalo",
        "statistics" to "Izibalo", "task_overview" to "Ukubuka Konke Kwemisebenzi", "total_tasks" to "Isamba Semisebenzi",
        "completion_rate" to "Izinga Lokuqedwa", "tasks_by_priority" to "Imisebenzi Ngokubaluleka",
        "mark_all_read" to "Phawula konke njengokufundiwe", "today" to "Namuhla", "yesterday" to "Izolo",
        "offline_title" to "Awuxhumekile",
        "offline_body" to "Awukho uxhumano lwe-inthanethi. Ungabuka futhi uhlele imisebenzi yakho ungaxhunyiwe. Izinguquko zizovumelaniswa lapho usuxhunywe futhi.",
        "go_to_tasks" to "Yiya Kwimisebenzi Yami", "retry_connection" to "Zama Futhi Ukuxhuma",
        "backup_sync" to "Isipele Nokuvumelanisa", "export_data" to "Thumela Idatha", "about" to "Mayelana",
        "date_format" to "Ifomethi Yosuku", "start_of_week" to "Ukuqala Kwesonto", "theme" to "Itimu",
        "push_notifications" to "Izaziso Ezisunguliwe", "due_date_reminders" to "Izikhumbuzo Zosuku Lokugcina",
        "daily_summary" to "Isifinyezo Sansuku Zonke", "preferences" to "Okuthandwayo", "other" to "Okunye",
        "status" to "Isimo",
        "about_title" to "Mayelana",
        "about_version" to "Uhlobo 1.0.0",
        "about_developer" to "Kwenziwe yi-Stack Masters",
        "about_description" to "I-Stressless iyisilandeleli semisebenzi yabafundi eyakhelwe abafundi bemfundo ephakeme baseNingizimu Afrika. Ikusiza uhlele, ubeke izinga lokubaluleka futhi uqedele imisebenzi yezemfundo ngokusekelwa kokungaxhunyiwe, izikhumbuzo, kanye nokusekelwa kwezilimi eziningi."
    )

    fun tr(key: String, language: String): String {
        val map = when (language) {
            "Afrikaans" -> af
            "isiZulu" -> zu
            else -> en
        }
        return map[key] ?: key
    }
}

// ---------- Auth (Firebase) + local in-memory task store ----------

object TaskRepository {
    // Lazy + nullable: FirebaseAuth.getInstance() throws if no FirebaseApp
    // is initialized, which is the case in plain JVM unit tests (no
    // Android runtime, no google-services setup). Falling back to null
    // lets TaskRepositoryTest run without crashing, while the real app
    // (which does have Firebase initialized) behaves exactly as before.
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable (expected in plain JVM unit tests)", e)
            null
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    val tasks = mutableStateListOf<Task>()
    val notifications = mutableStateListOf<AppNotification>()
    val darkMode = mutableStateOf(false)
    val language = mutableStateOf("English")
    val notificationsEnabled = mutableStateOf(true)
    val dueDateRemindersEnabled = mutableStateOf(true)
    val dailySummaryEnabled = mutableStateOf(true)
    val isOnline = mutableStateOf(true)

    // ---------- Offline persistence + pending-sync queue ----------
    // Tasks are mirrored to SharedPreferences as JSON so they survive the
    // app being killed while offline. pendingSync/pendingDeletes track which
    // task IDs were changed offline and still need pushing to Firestore once
    // we're back online, so reconnecting never silently overwrites unsynced
    // local edits.
    private lateinit var prefs: android.content.SharedPreferences
    private var prefsReady = false
    private val pendingSync = mutableSetOf<String>()
    private val pendingDeletes = mutableSetOf<String>()

    private const val PREFS_NAME = "stressless_offline_store"
    private const val KEY_TASKS = "tasks_json"
    private const val KEY_PENDING_SYNC = "pending_sync"
    private const val KEY_PENDING_DELETES = "pending_deletes"

    /** Call once from MainActivity.onCreate before anything else touches tasks. */
    fun init(context: Context) {
        if (prefsReady) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefsReady = true
        loadLocalTasks()
    }

    private fun Task.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("description", description)
        put("dueDate", dueDate)
        put("priority", priority)
        put("status", status)
        put("category", category)
        put("reminder", reminder)
        val subtasksArray = JSONArray()
        subtasks.forEach { st ->
            subtasksArray.put(JSONObject().apply {
                put("id", st.id); put("title", st.title); put("done", st.done)
            })
        }
        put("subtasks", subtasksArray)
    }

    private fun JSONObject.toTaskLocal(): Task {
        val subtasksArray = optJSONArray("subtasks") ?: JSONArray()
        val subtaskList = (0 until subtasksArray.length()).map { i ->
            val obj = subtasksArray.getJSONObject(i)
            Subtask(id = obj.getString("id"), title = obj.getString("title"), done = obj.getBoolean("done"))
        }
        return Task(
            id = getString("id"),
            title = getString("title"),
            description = optString("description", ""),
            dueDate = optLong("dueDate", 0L),
            priority = optString("priority", "Medium"),
            status = optString("status", "Pending"),
            category = optString("category", ""),
            reminder = optString("reminder", "On time"),
            subtasks = subtaskList
        )
    }

    private fun persistTasks() {
        if (!prefsReady) return
        val array = JSONArray()
        tasks.forEach { array.put(it.toJson()) }
        prefs.edit()
            .putString(KEY_TASKS, array.toString())
            .putString(KEY_PENDING_SYNC, JSONArray(pendingSync.toList()).toString())
            .putString(KEY_PENDING_DELETES, JSONArray(pendingDeletes.toList()).toString())
            .apply()
    }

    private fun loadLocalTasks() {
        if (!prefsReady) return
        try {
            val json = prefs.getString(KEY_TASKS, null)
            if (json != null) {
                val array = JSONArray(json)
                tasks.clear()
                for (i in 0 until array.length()) {
                    tasks.add(array.getJSONObject(i).toTaskLocal())
                }
            }
            val pendingSyncJson = JSONArray(prefs.getString(KEY_PENDING_SYNC, "[]"))
            pendingSync.clear()
            for (i in 0 until pendingSyncJson.length()) pendingSync.add(pendingSyncJson.getString(i))

            val pendingDeletesJson = JSONArray(prefs.getString(KEY_PENDING_DELETES, "[]"))
            pendingDeletes.clear()
            for (i in 0 until pendingDeletesJson.length()) pendingDeletes.add(pendingDeletesJson.getString(i))

            Log.i(TAG, "Loaded ${tasks.size} tasks from local storage (${pendingSync.size} pending sync, ${pendingDeletes.size} pending delete)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local tasks", e)
        }
    }

    val isLoggedIn: Boolean
        get() = auth?.currentUser != null

    val displayName: String
        get() {
            val user = auth?.currentUser
            val name = user?.displayName
            if (!name.isNullOrBlank()) return name.substringBefore(" ")
            return user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "there"
        }

    /**
     * Registers a new user with Firebase Authentication.
     * Firebase hashes (encrypts) the password server-side — the raw
     * password is only sent once over HTTPS and is never stored as-is.
     */
    suspend fun register(name: String, email: String, password: String): Result<Unit> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not available"))
        return try {
            Log.d(TAG, "Registering $email")
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            result.user?.updateProfile(profileUpdates)?.await()
            Log.i(TAG, "Register success for $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Register failed", e)
            Result.failure(e)
        }
    }

    /** Logs in an existing user via Firebase Auth. */
    suspend fun login(email: String, password: String): Result<Unit> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not available"))
        return try {
            Log.d(TAG, "Logging in $email")
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.i(TAG, "Login success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }

    /** Signs in using a Google ID token obtained from the Google Sign-In (SSO) flow. */
    suspend fun signInWithGoogleCredential(idToken: String): Result<Unit> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not available"))
        return try {
            Log.d(TAG, "Signing in with Google credential")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Log.i(TAG, "Google sign-in success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            Result.failure(e)
        }
    }

    fun logout() {
        Log.d(TAG, "Logging out")
        auth?.signOut()
        tasks.clear()
        notifications.clear()
        pendingSync.clear()
        pendingDeletes.clear()
        if (prefsReady) prefs.edit().clear().apply()
    }

    /**
     * Load all tasks from Firestore. Before pulling down, this FIRST pushes
     * up anything changed or deleted while offline — otherwise reconnecting
     * would overwrite unsynced local edits with stale cloud data.
     */
    suspend fun loadTasksFromCloud() {
        if (!isOnline.value) {
            Log.d(TAG, "Skipping cloud load — offline")
            return
        }

        if (pendingSync.isNotEmpty()) {
            Log.i(TAG, "Flushing ${pendingSync.size} pending offline changes to Firestore")
            pendingSync.toList().forEach { id ->
                tasks.find { it.id == id }?.let { FirestoreRepository.saveTask(it) }
            }
            pendingSync.clear()
        }

        if (pendingDeletes.isNotEmpty()) {
            Log.i(TAG, "Flushing ${pendingDeletes.size} pending offline deletes to Firestore")
            pendingDeletes.toList().forEach { id -> FirestoreRepository.deleteTask(id) }
            pendingDeletes.clear()
        }

        val result = FirestoreRepository.loadTasks()
        result.onSuccess { list ->
            tasks.clear()
            tasks.addAll(list)
            persistTasks()
            Log.i(TAG, "Synced ${list.size} tasks from cloud")
        }.onFailure {
            Log.e(TAG, "Cloud load error", it)
        }
    }

    /** Adds a task locally, writes to Firestore (or queues it if offline), and notifies. */
    fun addTask(task: Task) {
        Log.d(TAG, "addTask: ${task.title}")
        tasks.add(0, task)
        addNotification("Task Added", "${task.title} was added to your list", "success")

        if (isOnline.value) {
            scope.launch { FirestoreRepository.saveTask(task) }
        } else {
            pendingSync.add(task.id)
            Log.d(TAG, "Offline — queued ${task.id} for sync")
        }
        persistTasks()
    }

    /** Updates an existing task and syncs to Firestore (or queues it if offline). */
    fun updateTask(updated: Task) {
        Log.d(TAG, "updateTask: ${updated.id} -> ${updated.status}")
        val index = tasks.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            val previous = tasks[index]
            tasks[index] = updated
            if (previous.status != "Completed" && updated.status == "Completed") {
                addNotification("Task Completed", "${updated.title} completed", "success")
            }
            if (isOnline.value) {
                scope.launch { FirestoreRepository.saveTask(updated) }
            } else {
                pendingSync.add(updated.id)
                Log.d(TAG, "Offline — queued ${updated.id} for sync")
            }
            persistTasks()
        }
    }

    /** Deletes a task locally and from Firestore (or queues the delete if offline). */
    fun deleteTask(taskId: String) {
        Log.d(TAG, "deleteTask: $taskId")
        tasks.removeAll { it.id == taskId }
        pendingSync.remove(taskId)
        if (isOnline.value) {
            scope.launch { FirestoreRepository.deleteTask(taskId) }
        } else {
            pendingDeletes.add(taskId)
            Log.d(TAG, "Offline — queued $taskId for delete sync")
        }
        persistTasks()
    }

    fun addNotification(title: String, message: String, type: String = "info") {
        notifications.add(0, AppNotification(title = title, message = message, type = type))
    }

    fun markAllNotificationsRead() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(read = true)
        }
    }
}

// ---------- Local reminder notifications (system) ----------

object NotificationHelper {
    const val CHANNEL_ID = "stressless_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun leadTimeMillis(reminder: String): Long = when (reminder) {
        "30 mins before" -> 30 * 60 * 1000L
        "1 hour before" -> 60 * 60 * 1000L
        "1 day before" -> 24 * 60 * 60 * 1000L
        "None" -> -1L
        else -> 30 * 60 * 1000L
    }

    fun scheduleReminder(context: Context, task: Task) {
        val lead = leadTimeMillis(task.reminder)
        if (lead < 0) return
        val triggerAt = task.dueDate - lead
        if (triggerAt <= System.currentTimeMillis()) return

        Log.d(TAG, "Scheduling reminder for ${task.title} at $triggerAt")
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("taskId", task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task due soon"
        Log.d(TAG, "Reminder fired for $title")
        TaskRepository.addNotification("Task Reminder", "$title is due soon", "reminder")
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Reminder: $title")
            .setContentText("This task is due soon")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(title.hashCode(), notification)
    }
}