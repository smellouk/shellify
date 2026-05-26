package io.shellify.app.core.engine

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.freehaven.tor.control.TorControlConnection
import org.torproject.jni.TorService

/**
 * Manages the Tor daemon lifecycle and exposes [torState] for UI observation.
 *
 * On-demand daemon: [ensureStarted] starts the Tor daemon when the first Tor-enabled app opens.
 * [releaseApp] schedules a shutdown after [GRACE_PERIOD_MS] when the last app without
 * preserveIdentity closes. Apps with preserveIdentity = true keep the daemon alive indefinitely.
 *
 * TorService is started with [startService] rather than [startForegroundService] so Android
 * does not enforce the 5-second [Service.startForeground] deadline — TorService promotes itself
 * to foreground once its native library finishes loading. Doze safety is preserved because
 * TorService holds a foreground notification for its entire lifetime (T-02-21).
 *
 * @param controlConnection injected for testability; defaults to null (production wires via
 *   TorService.getTorControlConnection() after STATUS_ON is received).
 * @param testScope optional coroutine scope for unit tests; production code uses an internal
 *   IO-dispatched scope.
 */
class TorManager(
    private val context: Context,
    private val controlConnection: TorControlConnection? = null,
    testScope: CoroutineScope? = null,
) {

    companion object {
        private const val TAG = "TorManager"
        const val GRACE_PERIOD_MS = 30_000L
        internal const val CONTROL_PORT = 9051
    }

    // Use the injected test scope if provided, otherwise create a production IO scope.
    private val managerScope: CoroutineScope = testScope
        ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _torState = MutableStateFlow<TorState>(TorState.Stopped)
    val torState: StateFlow<TorState> = _torState.asStateFlow()

    // Guards all reads and writes to preserveIdentityApps and activeApps.
    // Both sets are mutated from multiple coroutine contexts (CR-05).
    private val setLock = Mutex()

    // Apps with preserveTorIdentity = true — daemon is never stopped while this set is non-empty.
    private val preserveIdentityApps = mutableSetOf<Long>()

    // All active (non-incognito, non-preserve) app session IDs.
    private val activeApps = mutableSetOf<Long>()

    private var torReceiver: BroadcastReceiver? = null

    // Tracks whether TorService was actually started so stop() only calls stopService() when needed.
    // Volatile because onServiceDisconnected (main thread) may write concurrently with stop() (IO).
    @Volatile private var serviceStarted = false

    // Tracks whether we have an active bindService() so unbindService() is only called once.
    @Volatile private var isBound = false

    /**
     * Monitors the :tor process for unexpected death.
     *
     * [onServiceDisconnected] fires when the remote :tor process crashes (not when we call
     * [unbindService] deliberately). On crash: cancels Android's automatic service restart,
     * so the crashing native binary doesn't loop; then emits [TorState.Error] so the UI
     * can surface a retry option.
     *
     * [onServiceConnected] is a no-op — state updates come via [BroadcastReceiver].
     *
     * Internal visibility to allow direct invocation in unit tests without reflection.
     */
    internal val torServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // :tor process died unexpectedly. Prevent Android from restarting TorService
            // automatically — the native crash (e.g. SELinux ioctl denial) would repeat.
            isBound = false
            runCatching {
                context.stopService(Intent(context, TorService::class.java))
            }.onFailure { Log.w(TAG, "Failed to cancel TorService restart after crash", it) }
            serviceStarted = false
            _torState.value = TorState.Error("Tor process terminated unexpectedly")
        }
    }

    /**
     * Called when a Tor-enabled PWA session opens.
     *
     * If [preserveIdentity] is true, the app is added to [preserveIdentityApps] so that
     * [releaseApp] can never schedule a shutdown while it remains registered.
     *
     * TorService is started via [startService] so Android does not enforce the 5-second
     * [Service.startForeground] deadline. TorService promotes itself to foreground once its
     * native library finishes loading. Notification copy belongs to Plan 05.
     */
    fun ensureStarted(appId: Long, preserveIdentity: Boolean) {
        managerScope.launch {
            setLock.withLock {
                if (preserveIdentity) preserveIdentityApps += appId
                activeApps += appId
            }

            val alreadyRunning = _torState.value == TorState.Ready || _torState.value == TorState.Connecting
            if (alreadyRunning) return@launch

            _torState.value = TorState.Connecting
            registerStatusReceiver()

            // Use startService() so Android does not enforce the 5-second startForeground()
            // deadline. TorService promotes itself to foreground internally once its native
            // library finishes loading, which can take longer than 5 seconds on first launch.
            // ensureStarted() is always called while the app is in the foreground (from
            // WebViewActivity), so startService() is permitted without the strict deadline (T-02-21).
            runCatching {
                val intent = Intent(context, TorService::class.java)
                context.startService(intent)
                serviceStarted = true
                // Bind to TorService so onServiceDisconnected fires if the :tor process dies
                // unexpectedly (e.g. SELinux ioctl denial causing a native abort). BIND_AUTO_CREATE
                // is required even though the service was already started — without it, bindService
                // returns false on some API levels when called on Application context.
                if (!isBound) {
                    val bound = context.bindService(intent, torServiceConnection, Context.BIND_AUTO_CREATE)
                    if (!bound) Log.w(TAG, "bindService returned false — process-death detection unavailable")
                }
            }.onFailure {
                // Surface the failure so the UI can offer a retry instead of freezing on Connecting.
                Log.e(TAG, "Failed to start TorService", it)
                _torState.value = TorState.Error("Failed to start Tor: ${it.message}")
            }
        }
    }

    /**
     * Called when a Tor-enabled PWA session closes.
     *
     * If [preserveIdentity] is true, the daemon must NEVER be shut down — the preserve-identity
     * flag means the user wants a persistent Tor circuit across sessions. The app is removed from
     * [activeApps] but NOT from [preserveIdentityApps] (D-07).
     *
     * Daemon shutdown is scheduled only when:
     *   1. [preserveIdentity] is false, AND
     *   2. [preserveIdentityApps] is empty, AND
     *   3. [activeApps] is empty.
     *
     * The [GRACE_PERIOD_MS] delay allows re-used sessions to cancel the pending shutdown
     * by calling [ensureStarted] again, re-populating [activeApps] (T-02-19).
     */
    fun releaseApp(appId: Long, preserveIdentity: Boolean) {
        managerScope.launch {
            val shouldScheduleShutdown = setLock.withLock {
                activeApps -= appId
                // When preserveIdentity = true, the app keeps its slot in preserveIdentityApps so the
                // daemon lives as long as the app is installed. Never schedule shutdown.
                if (preserveIdentity) return@withLock false
                // If any preserve-identity app is still registered, never schedule shutdown.
                if (preserveIdentityApps.isNotEmpty()) return@withLock false
                activeApps.isEmpty()
            }

            if (!shouldScheduleShutdown) return@launch

            delay(GRACE_PERIOD_MS)
            // Re-check after grace period — a new ensureStarted() may have arrived.
            val canStop = setLock.withLock { preserveIdentityApps.isEmpty() && activeApps.isEmpty() }
            if (canStop) stop()
        }
    }

    fun registerPreserveIdentityApp(appId: Long) {
        managerScope.launch { setLock.withLock { preserveIdentityApps += appId } }
    }

    fun unregisterPreserveIdentityApp(appId: Long) {
        managerScope.launch { setLock.withLock { preserveIdentityApps -= appId } }
    }

    /**
     * Forces an immediate Tor daemon stop, bypassing the grace period and all tracking state.
     *
     * Called from panic wipe so the Tor foreground service notification disappears along with all
     * other app data. Clears [preserveIdentityApps] and [activeApps] so stale entries for deleted
     * apps do not survive the wipe (WR-01).
     */
    fun forceStop() {
        managerScope.launch {
            setLock.withLock {
                preserveIdentityApps.clear()
                activeApps.clear()
            }
            stop()
        }
    }

    /**
     * Sends a NEWNYM signal to rotate the Tor circuit.
     *
     * Dispatched on [managerScope] (IO). Failure sets [torState] to [TorState.Error].
     */
    fun newIdentity() {
        val conn = controlConnection ?: return
        managerScope.launch {
            runCatching { conn.signal("NEWNYM") }
                .onFailure { _torState.value = TorState.Error("NEWNYM failed: ${it.message}") }
        }
    }

    private fun registerStatusReceiver() {
        if (torReceiver != null) return

        // Build the receiver locally so it is only assigned to torReceiver after successful
        // registration. If registerReceiver() throws and we assigned torReceiver first, the
        // null-guard at the top of this function would skip all future registration attempts,
        // leaving _torState stuck at Connecting forever (no broadcasts would ever arrive).
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val status = intent.getStringExtra(TorService.EXTRA_STATUS) ?: return
                when (status) {
                    TorService.STATUS_ON -> _torState.value = TorState.Ready
                    TorService.STATUS_STARTING -> _torState.value = TorState.Connecting
                    TorService.STATUS_OFF -> _torState.value = TorState.Stopped
                }
            }
        }

        // TorService.ACTION_STATUS is an internal broadcast — RECEIVER_NOT_EXPORTED is correct.
        // ContextCompat.registerReceiver() handles the API-level flag requirement automatically.
        runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(TorService.ACTION_STATUS),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            // Assign only on success so a failed registration leaves torReceiver null
            // and the next ensureStarted() call can attempt registration again.
            torReceiver = receiver
        }.onFailure {
            Log.e(TAG, "Failed to register TorService receiver", it)
            _torState.value = TorState.Error("Failed to listen for Tor status: ${it.message}")
        }
    }

    private fun stop() {
        runCatching {
            // Unbind before unregistering the receiver and stopping the service so that
            // onServiceDisconnected is not triggered by our own deliberate stop.
            if (isBound) {
                context.unbindService(torServiceConnection)
                isBound = false
            }
            torReceiver?.let {
                context.unregisterReceiver(it)
                torReceiver = null
            }
            // Only call stopService() when TorService was actually started, avoiding unnecessary
            // Android Intent construction in environments where the service was never launched.
            if (serviceStarted) {
                val intent = Intent(context, TorService::class.java)
                context.stopService(intent)
                serviceStarted = false
            }
        }.onFailure { Log.w(TAG, "Failed to stop TorService", it) }
        _torState.value = TorState.Stopped
    }
}
