package io.shellify.app.presentation.webview

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.shellify.app.domain.model.WebApp
import io.shellify.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BackgroundNotificationService : Service() {

    companion object {
        const val EXTRA_APP_ID = "extra_app_id"
        const val ACTION_STOP = "io.shellify.app.action.STOP_BG_NOTIFICATIONS"
        @Suppress("MagicNumber")
        private const val SERVICE_NOTIFICATION_ID = 0x06_5E_57_1C
        const val SERVICE_CHANNEL_ID = "shellify_bg_service"
        private const val TAG = "BgNotificationService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MUST be the very first statement: Android will ANR if startForeground is not
        // called promptly when the service is started via startForegroundService.
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(null))

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val appId = intent?.getLongExtra(EXTRA_APP_ID, -1L) ?: -1L
        if (appId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch { keepAlive(appId) }
        return START_STICKY
    }

    private suspend fun keepAlive(appId: Long) {
        val provider = application as WebViewServiceProvider

        // GeckoView native libs must be loaded before anything touches the runtime.
        provider.injectAndLoadGeckoView()

        val webApp: WebApp = provider.getWebAppById(appId) ?: run {
            Log.w(TAG, "WebApp $appId not found; stopping service")
            stopSelf()
            return
        }

        @Suppress("MissingPermission") // Foreground service notification; POST_NOTIFICATIONS declared in manifest
        NotificationManagerCompat.from(this@BackgroundNotificationService)
            .notify(SERVICE_NOTIFICATION_ID, buildServiceNotification(webApp.name))

        // This service's sole purpose is process keepalive: it prevents Android from killing
        // the process and throttling GeckoView's JS timers while WebViewActivity is backgrounded.
        // Known limitation: if Android destroys the Activity under memory pressure, the
        // GeckoSession and its WebNotificationDelegate are also destroyed and PWA notifications
        // stop arriving — the foreground service notification is the only thing that remains.
        // A future improvement would restore a background GeckoSession here to cover that gap.
        Log.d(TAG, "Process keepalive active for ${webApp.name} (appId=$appId)")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildServiceNotification(appName: String?): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(SERVICE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.bg_notification_service_title),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }

        val contentText = if (appName != null) {
            getString(R.string.bg_notification_service_text, appName)
        } else {
            getString(R.string.bg_notification_service_text_generic)
        }

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(getString(R.string.bg_notification_service_title))
            .setContentText(contentText)
            .setOngoing(true)
            .build()
    }
}
