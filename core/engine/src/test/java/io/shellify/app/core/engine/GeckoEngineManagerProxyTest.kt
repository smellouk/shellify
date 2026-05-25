package io.shellify.app.core.engine

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mozilla.geckoview.GeckoRuntime

class GeckoEngineManagerProxyTest {

    private lateinit var manager: GeckoEngineManager

    @Before
    fun setUp() {
        val context = mockk<android.content.Context>(relaxed = true)
        manager = GeckoEngineManager(context)
        // Override the runtimeFactory so tests never call real GeckoRuntime.create().
        // Returns a distinct mock on each factory invocation; with a single runtime the factory
        // is called at most once per manager instance.
        manager.runtimeFactory = { _ -> mockk<GeckoRuntime>(relaxed = true) }
    }

    @Test
    fun `G1 - getRuntime(None) returns same instance on second call`() {
        val r1 = manager.getRuntime(ProxyConfig.None)
        val r2 = manager.getRuntime(ProxyConfig.None)

        assertSame("same ProxyConfig.None key must return the same GeckoRuntime instance", r1, r2)
    }

    @Test
    fun `G2 - getRuntime returns the same single instance regardless of ProxyConfig`() {
        // GeckoView allows only ONE GeckoRuntime per process. Both calls must return the same
        // object; per-ProxyConfig isolation is achieved via JVM system properties, not separate
        // runtimes.
        val rNone = manager.getRuntime(ProxyConfig.None)
        val rSocks5 = manager.getRuntime(ProxyConfig.Socks5("127.0.0.1", 9050))

        assertSame("GeckoRuntime is a process singleton — all ProxyConfig values share the same instance", rNone, rSocks5)
    }

    @Test
    fun `G3 - getRuntime(Socks5) returns the same instance on repeated calls`() {
        val r1 = manager.getRuntime(ProxyConfig.Socks5("127.0.0.1", 9050))
        val r2 = manager.getRuntime(ProxyConfig.Socks5("127.0.0.1", 9050))

        assertSame("repeated calls with identical ProxyConfig must return the same GeckoRuntime", r1, r2)
    }

    @Test
    fun `G4 - getRuntime() with no args defaults to ProxyConfig None`() {
        val rDefault = manager.getRuntime()
        val rNone = manager.getRuntime(ProxyConfig.None)

        assertSame("no-arg getRuntime() must return the same instance as getRuntime(ProxyConfig.None)", rDefault, rNone)
    }

    @Test
    fun `G5 - getRuntime(Socks5) sets JVM socksProxy system properties`() {
        manager.getRuntime(ProxyConfig.Socks5("127.0.0.1", 9050))

        assertEquals("127.0.0.1", System.getProperty("socksProxyHost"))
        assertEquals("9050", System.getProperty("socksProxyPort"))

        // Cleanup so other tests are not affected.
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
    }

    @Test
    fun `G6 - getRuntime(None) clears JVM socksProxy system properties`() {
        // Simulate a prior Tor session having set the properties.
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", "9050")

        manager.getRuntime(ProxyConfig.None)

        assertEquals(null, System.getProperty("socksProxyHost"))
        assertEquals(null, System.getProperty("socksProxyPort"))
    }

    @Test
    fun `G7 - clearDataForContext does not throw when runtime exists`() {
        manager.getRuntime(ProxyConfig.None)
        // GeckoRuntime is a relaxed mock — just assert no exception is thrown.
        manager.clearDataForContext("test-isolation-id")
    }

    @Test
    fun `G8 - clearDataForContext is a no-op when runtime has not been created`() {
        // No getRuntime() call — runtime is null. Must not throw.
        manager.clearDataForContext("test-isolation-id")
    }
}
