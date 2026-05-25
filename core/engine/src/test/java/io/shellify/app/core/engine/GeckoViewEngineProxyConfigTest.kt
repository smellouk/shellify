package io.shellify.app.core.engine

import android.content.Context
import io.mockk.mockk
import io.shellify.app.domain.model.WebApp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Verifies the proxyConfigFor() logic on GeckoViewEngine and the companion constants it relies on.
 *
 * GeckoViewEngine has a non-trivial constructor but proxyConfigFor() only reads app.useTor —
 * no runtime interaction is needed, so mocking Context and GeckoEngineManager with relaxed mocks
 * is sufficient to instantiate the engine without triggering real GeckoRuntime creation.
 */
class GeckoViewEngineProxyConfigTest {

    private lateinit var engine: GeckoViewEngine

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        val engineManager = mockk<GeckoEngineManager>(relaxed = true)
        engine = GeckoViewEngine(context, engineManager)
    }

    // PC-1: useTor=true produces a Socks5 config pointing at the local Tor daemon
    @Test
    fun `proxyConfigFor returns Socks5 on 127-0-0-1 port 9050 when useTor is true`() {
        val app = WebApp(name = "SecureApp", url = "https://example.onion", useTor = true)
        val config = engine.proxyConfigFor(app)
        assertEquals(ProxyConfig.Socks5("127.0.0.1", 9050), config)
    }

    // PC-2: useTor=false produces ProxyConfig.None (direct connection)
    @Test
    fun `proxyConfigFor returns None when useTor is false`() {
        val app = WebApp(name = "RegularApp", url = "https://example.com", useTor = false)
        val config = engine.proxyConfigFor(app)
        assertEquals(ProxyConfig.None, config)
    }

    // PC-3: companion constants match the expected Tor default address and port
    @Test
    fun `TOR_PROXY_HOST constant is 127-0-0-1`() {
        assertEquals("127.0.0.1", GeckoViewEngine.TOR_PROXY_HOST)
    }

    @Test
    fun `TOR_PROXY_PORT constant is 9050`() {
        assertEquals(9050, GeckoViewEngine.TOR_PROXY_PORT)
    }
}
