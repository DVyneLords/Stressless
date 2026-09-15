package za.co.rbi.st10448886.stressless

import android.Manifest
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import za.co.rbi.st10448886.stressless.ui.theme.StresslessTheme

private const val TAG = "MainActivity"

/**
 * MainActivity — single-activity entry point. Sets up the notification
 * channel, initializes local offline storage, tracks live connectivity via
 * ConnectivityManager, and hosts the full Compose navigation graph.
 * An OfflineScreen overlay is shown on top of whatever screen is active
 * whenever the device loses its internet connection.
 */
class MainActivity : ComponentActivity() {
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Required once, before any reminder notification can be shown (Android 8+)
        NotificationHelper.createChannel(this)
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        // Loads any locally cached tasks/pending-sync state from SharedPreferences
        TaskRepository.init(applicationContext)
        TaskRepository.isOnline.value = isCurrentlyOnline()
        Log.i(TAG, "App started. Online=${TaskRepository.isOnline.value}")

        // If we're already logged in and have a connection at startup,
        // refresh from the cloud immediately (also flushes any pending offline changes).
        if (TaskRepository.isLoggedIn && TaskRepository.isOnline.value) {
            lifecycleScope.launch { TaskRepository.loadTasksFromCloud() }
        }
        setContent {
            // Runtime notification permission is required from Android 13 (TIRAMISU) onward
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }

            // Registers a live network callback for the lifetime of this composition,
            // so isOnline updates immediately when connectivity changes (rather than
            // only being checked once at app start).
            DisposableEffect(Unit) {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Network available")
                        TaskRepository.isOnline.value = true
                        // Sync pending changes when we come back online
                        lifecycleScope.launch { TaskRepository.loadTasksFromCloud() }
                    }
                    override fun onLost(network: Network) {
                        Log.d(TAG, "Network lost")
                        TaskRepository.isOnline.value = isCurrentlyOnline()
                    }
                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        TaskRepository.isOnline.value = isCurrentlyOnline()
                    }
                }
                networkCallback = callback
                connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
                // Unregister when this composable leaves composition, to avoid leaking the callback
                onDispose {
                    networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
                }
            }

            StresslessTheme(darkTheme = TaskRepository.darkMode.value) {
                val navController = rememberNavController()
                val isOnline = TaskRepository.isOnline.value

                Box {
                    StresslessNavHost(navController)
                    // Overlay shown on top of the current screen whenever offline —
                    // does not replace navigation, so the underlying screen state is preserved.
                    if (!isOnline) {
                        OfflineScreen(
                            onGoToTasks = { navController.navigate("dashboard") { launchSingleTop = true } },
                            onRetry = { TaskRepository.isOnline.value = isCurrentlyOnline() }
                        )
                    }
                }
            }
        }
    }

    /** Checks the OS-reported connectivity state directly (used for manual "Retry" and initial checks). */
    private fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * StresslessNavHost — declares every screen route and the navigation
 * transitions between them. This is the single source of truth for app flow:
 * splash -> onboarding -> (login|register) -> dashboard -> (calendar|stats|settings|...).
 */
@Composable
private fun StresslessNavHost(navController: NavHostController) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onFinished = {
                // Skip onboarding entirely if a session is already active
                val next = if (TaskRepository.isLoggedIn) "dashboard" else "onboarding"
                navController.navigate(next) { popUpTo("splash") { inclusive = true } }
            })
        }

        composable("onboarding") {
            OnboardingScreen(
                onGetStarted = { navController.navigate("register") },
                onHaveAccount = { navController.navigate("login") }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Pull tasks from Firestore on successful login
                    scope.launch { TaskRepository.loadTasksFromCloud() }
                    // Clears onboarding/login/register from the back stack so the
                    // system back button doesn't return the user to the login screen
                    navController.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } }
                },
                onGoToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } }
                },
                onGoToLogin = { navController.navigate("login") }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onAddTask = { navController.navigate("form/new") },
                onOpenTask = { taskId -> navController.navigate("details/$taskId") },
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        // "new" as the taskId signals TaskFormScreen to create a new task;
        // any other value means editing an existing one.
        composable("form/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: "new"
            TaskFormScreen(
                taskId = taskId,
                onDone = { navController.popBackStack() }
            )
        }

        composable("details/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailsScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("form/$taskId") }
            )
        }

        composable("calendar") {
            CalendarScreen(
                onOpenTask = { taskId -> navController.navigate("details/$taskId") },
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable("stats") {
            StatisticsScreen(
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable("settings") {
            SettingsScreen(
                onLogout = {
                    TaskRepository.logout()
                    // popUpTo(0) clears the ENTIRE back stack — after logging out,
                    // the user should never be able to navigate "back" into the app
                    navController.navigate("onboarding") { popUpTo(0) }
                },
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }

        composable("notifications") {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}