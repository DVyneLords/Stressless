package za.co.rbi.st10448886.stressless

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "FCMService"

/**
 * StresslessMessagingService — receives Firebase Cloud Messaging pushes for
 * real-time alerts (e.g. reminders or updates triggered remotely, not just
 * the locally scheduled AlarmManager reminders in NotificationHelper).
 * Registered as a <service> in AndroidManifest.xml. The OS starts this even
 * if the app process isn't currently running.
 */
class StresslessMessagingService : FirebaseMessagingService() {

    /**
     * Called whenever FCM issues a new device token — on first install,
     * after the app's data is cleared, or on token rotation. The token is
     * how a message gets targeted at this specific device, so it must be
     * saved to Firestore every time it changes.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.saveFcmToken(token)
        }
    }

    /**
     * Called when a push notification arrives while the app process is alive
     * (foreground or background). Mirrors it into the in-app notification
     * list AND shows a system notification, so the user sees it either way.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Support both a "notification" payload (title/body) and a plain
        // "data" payload, so this works whether the push is sent from the
        // Firebase Console UI or from custom server code.
        val title = message.notification?.title ?: message.data["title"] ?: "Stressless"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new update"
        Log.d(TAG, "Push received: $title - $body")

        // Show up on NotificationsScreen too, not just as a system tray popup
        TaskRepository.addNotification(title, body, "reminder")

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}