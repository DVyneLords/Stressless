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

class MainActivity : ComponentActivity() {
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        TaskRepository.init(applicationContext)
        TaskRepository.isOnline.value = isCurrentlyOnline()
        Log.i(TAG, "App started. Online=${TaskRepository.isOnline.value}")

        if (TaskRepository.isLoggedIn && TaskRepository.isOnline.value) {
            lifecycleScope.launch { TaskRepository.loadTasksFromCloud() }
        }
        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }

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
                onDispose {
                    networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
                }
            }

            StresslessTheme(darkTheme = TaskRepository.darkMode.value) {
                val navController = rememberNavController()
                val isOnline = TaskRepository.isOnline.value

                Box {
                    StresslessNavHost(navController)
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

    private fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

@Composable
private fun StresslessNavHost(navController: NavHostController) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onFinished = {
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