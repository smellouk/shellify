package io.shellify.app.presentation.webview

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.shellify.app.core.engine.BrowserEngineCallback
import io.shellify.app.core.engine.ProxyConfig
import io.shellify.app.core.engine.NotificationDelegateFactory
import io.shellify.app.domain.model.NotificationPermission
import io.shellify.app.domain.model.WebApp
import io.shellify.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate

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

    // Background GeckoSession created only when the Activity for this app is not active.
    // Accessed exclusively on the main thread.
    private var backgroundSession: GeckoSession? = null
    private var backgroundAppId: Long = -1L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MUST be the very first statement: Android will ANR if startForeground is not
        // called promptly when the service is started via startForegroundService.
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(null))

        if (intent?.action == ACTION_STOP) {
            closeBackgroundSession()
            stopSelf()
            return START_NOT_STICKY
        }

        val appId = intent?.getLongExtra(EXTRA_APP_ID, -1L) ?: -1L
        if (appId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch { manageSession(appId) }
        return START_STICKY
    }

    private suspend fun manageSession(appId: Long) {
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

        // If the Activity is still showing this app, its GeckoSession and WebNotificationDelegate
        // are already handling notifications — just keep the process alive and do nothing else.
        if (provider.activeWebViewApps.value.contains(appId)) {
            Log.d(TAG, "Activity active for $appId, process keepalive only")
            return
        }

        // Activity is gone — open a background session to keep receiving notifications.
        withContext(Dispatchers.Main) {
            if (backgroundSession != null && backgroundAppId == appId) {
                Log.d(TAG, "Background session already running for $appId")
                return@withContext
            }
            // Close any session left over from a different app.
            backgroundSession?.close()
            backgroundSession = null

            val dispatcher = provider.notificationDispatcher ?: run {
                Log.w(TAG, "No dispatcher available; skipping background session")
                return@withContext
            }

            val cb = buildCallback(webApp, dispatcher)
            // Apply the correct proxy config so system properties are set before session.open().
            val proxyConfig = if (webApp.useTor) ProxyConfig.Socks5("127.0.0.1", 9050) else ProxyConfig.None
            val runtime = provider.geckoEngineManager.getRuntime(proxyConfig)

            runtime.setWebNotificationDelegate(object : WebNotificationDelegate {
                override fun onShowNotification(notification: WebNotification) {
                    val title = notification.title ?: return
                    cb.onNotificationReceived(title, notification.text, notification.imageUrl, notification.tag)
                }
                override fun onCloseNotification(notification: WebNotification) = Unit
            })

            val settings = GeckoSessionSettings.Builder()
                .contextId(webApp.isolationId)
                .build()
            val session = GeckoSession(settings)
            session.open(runtime)
            NotificationDelegateFactory.attach(session, cb)
            session.loadUri(webApp.url)

            backgroundSession = session
            backgroundAppId = appId
            Log.i(TAG, "Background session started for ${webApp.name} (appId=$appId)")
        }
    }

    private fun buildCallback(webApp: WebApp, dispatcher: PwaNotificationDispatcher): BrowserEngineCallback =
        object : BrowserEngineCallback {
            override fun onNotificationReceived(title: String, body: String?, iconUrl: String?, tag: String?) {
                scope.launch { dispatcher.dispatch(webApp, title, body, iconUrl, tag) }
            }
            override fun onNotificationPermissionRequested(onResult: (Boolean) -> Unit) {
                // Background — no dialog available; respect the stored permission.
                onResult(webApp.notificationPermission == NotificationPermission.GRANTED)
            }
            override fun onPageStarted(url: String?) = Unit
            override fun onPageFinished(url: String?) = Unit
            override fun onProgressChanged(progress: Int) = Unit
            override fun onTitleChanged(title: String?) = Unit
            override fun onIconReceived(icon: Bitmap?) = Unit
            override fun onError(errorCode: Int, description: String) {
                Log.w(TAG, "Background session error $errorCode: $description")
            }
            override fun onSslError(error: String) {
                Log.w(TAG, "Background session SSL error: $error")
            }
            override fun onExternalLink(url: String) = Unit
            override fun onShowCustomView(view: View?, callback: Any?) = Unit
            override fun onHideCustomView() = Unit
            override fun onDownloadStart(
                url: String, userAgent: String, contentDisposition: String,
                mimeType: String, contentLength: Long,
            ) = Unit
        }

    // Must be called on the main thread.
    private fun closeBackgroundSession() {
        backgroundSession?.close()
        backgroundSession = null
        backgroundAppId = -1L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        // onDestroy runs on the main thread — safe to close the session directly.
        closeBackgroundSession()
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
