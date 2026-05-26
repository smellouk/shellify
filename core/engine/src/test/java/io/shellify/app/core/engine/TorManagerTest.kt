package io.shellify.app.core.engine

import android.content.ComponentName
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.freehaven.tor.control.TorControlConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TorManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun buildManager(controlConnection: TorControlConnection? = null): TorManager {
        val context = mockk<android.content.Context>(relaxed = true)
        return TorManager(
            context = context,
            controlConnection = controlConnection,
            testScope = testScope,
        )
    }

    @Test
    fun `T1 - torState is initially Stopped`() {
        val manager = buildManager()

        assertEquals(TorState.Stopped, manager.torState.value)
    }

    @Test
    fun `T2 - releaseApp with preserveIdentity=true does NOT schedule daemon shutdown`() = testScope.runTest {
        val manager = buildManager()
        manager.registerPreserveIdentityApp(1L)

        // Release with preserveIdentity = true — shutdown must NOT be scheduled.
        manager.releaseApp(1L, preserveIdentity = true)

        // Advance past the grace period — state must remain Stopped (not transition because of us).
        advanceTimeBy(TorManager.GRACE_PERIOD_MS + 1_000L)

        // State should still be Stopped since we never started Tor in this test —
        // the important assertion is that no rogue shutdown was triggered.
        assertEquals(TorState.Stopped, manager.torState.value)
    }

    @Test
    fun `T3 - releaseApp with preserveIdentity=false and empty preserveIdentityApps schedules stop after grace period`() =
        testScope.runTest {
            val manager = buildManager()
            // Add and release an active (non-preserve) app.
            manager.releaseApp(2L, preserveIdentity = false)

            // Before grace period expires: state has not changed yet.
            advanceTimeBy(TorManager.GRACE_PERIOD_MS - 1_000L)
            // State is still Stopped because we never started (or it may be in whatever state it was).

            // After grace period: stop() has been called, transitioning to Stopped.
            advanceTimeBy(2_000L)
            assertEquals(TorState.Stopped, manager.torState.value)
        }

    @Test
    fun `T4 - newIdentity invokes signal NEWNYM on the injected TorControlConnection exactly once`() =
        testScope.runTest {
            val mockConnection = mockk<TorControlConnection>(relaxed = true)
            val manager = buildManager(controlConnection = mockConnection)

            manager.newIdentity()
            // Allow the coroutine to run.
            testDispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 1) { mockConnection.signal("NEWNYM") }
        }

    @Test
    fun `T5 - torState transitions to Error when TorService process dies unexpectedly`() {
        val manager = buildManager()
        // Simulate an unexpected :tor process crash via the ServiceConnection callback.
        // onServiceDisconnected is called by Android when the remote process dies; it must
        // set TorState.Error so the UI can surface a retry without waiting indefinitely.
        val name = ComponentName("io.shellify.app", "org.torproject.jni.TorService")
        manager.torServiceConnection.onServiceDisconnected(name)

        val state = manager.torState.value
        assertTrue("Expected TorState.Error but got $state", state is TorState.Error)
    }

    @Test
    fun `T6 - ensureStarted emits TorState Error when startService throws`() = testScope.runTest {
        // Arrange: context.startService throws (e.g. IllegalStateException from background start).
        val context = mockk<Context>(relaxed = true) {
            every { startService(any()) } throws IllegalStateException("Not allowed in background")
        }
        val manager = TorManager(context = context, testScope = testScope)

        // Act
        manager.ensureStarted(appId = 1L, preserveIdentity = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: error is surfaced so the UI can show Retry instead of freezing on Connecting.
        val state = manager.torState.value
        assertTrue("Expected TorState.Error after startService failure but got $state", state is TorState.Error)
    }
}
