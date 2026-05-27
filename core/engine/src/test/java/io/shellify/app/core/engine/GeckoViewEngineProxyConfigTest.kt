package io.shellify.app.core.engine

import android.content.Context
import io.mockk.mockk
import io.shellify.app.domain.model.ProxyType
import io.shellify.app.domain.model.WebApp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Verifies the proxyConfigFor() logic on GeckoViewEngine and the companion constants it relies on.
 *
 * GeckoViewEngine has a non-trivial constructor but proxyConfigFor() only reads app proxy fields —
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

    // PC-2: useTor=false and no custom proxy produces ProxyConfig.None (direct connection)
    @Test
    fun `proxyConfigFor returns None when useTor is false and no custom proxy`() {
        val app = WebApp(name = "RegularApp", url = "https://example.com", useTor = false)
        val config = engine.proxyConfigFor(app)
        assertEquals(ProxyConfig.None, config)
    }

    // Companion constant tests
    @Test
    fun `TOR_PROXY_HOST constant is 127-0-0-1`() {
        assertEquals("127.0.0.1", GeckoViewEngine.TOR_PROXY_HOST)
    }

    @Test
    fun `TOR_PROXY_PORT constant is 9050`() {
        assertEquals(9050, GeckoViewEngine.TOR_PROXY_PORT)
    }

    // PC-3: custom SOCKS5 with valid host, port, and credentials
    @Test
    fun `PC-3 proxyConfigFor returns Socks5 with credentials for custom SOCKS5 proxy`() {
        val app = WebApp(
            name = "App",
            url = "https://example.com",
            customProxyType = ProxyType.SOCKS5,
            customProxyHost = "10.0.0.1",
            customProxyPort = 1080,
            customProxyUsername = "u",
            customProxyPassword = "p",
        )
        assertEquals(ProxyConfig.Socks5("10.0.0.1", 1080, "u", "p"), engine.proxyConfigFor(app))
    }

    // PC-4: custom HTTP proxy with no credentials
    @Test
    fun `PC-4 proxyConfigFor returns Http for custom HTTP proxy`() {
        val app = WebApp(
            name = "App",
            url = "https://example.com",
            customProxyType = ProxyType.HTTP,
            customProxyHost = "proxy.example.com",
            customProxyPort = 8080,
        )
        assertEquals(ProxyConfig.Http("proxy.example.com", 8080, null, null), engine.proxyConfigFor(app))
    }

    // PC-5: custom SOCKS5 with port=0 must fall back to None (Pitfall 7 port guard)
    @Test
    fun `PC-5 proxyConfigFor returns None when custom SOCKS5 has port 0`() {
        val app = WebApp(
            name = "App",
            url = "https://example.com",
            customProxyType = ProxyType.SOCKS5,
            customProxyHost = "h",
            customProxyPort = 0,
        )
        assertEquals(ProxyConfig.None, engine.proxyConfigFor(app))
    }

    // PC-6: custom HTTP with blank host must fall back to None (host guard)
    @Test
    fun `PC-6 proxyConfigFor returns None when custom HTTP has blank host`() {
        val app = WebApp(
            name = "App",
            url = "https://example.com",
            customProxyType = ProxyType.HTTP,
            customProxyHost = "   ",
            customProxyPort = 8080,
        )
        assertEquals(ProxyConfig.None, engine.proxyConfigFor(app))
    }

    // PC-7: when BOTH useTor=true AND custom SOCKS5 with valid host/port, custom proxy wins
    @Test
    fun `PC-7 custom SOCKS5 proxy takes priority over Tor when both are configured`() {
        val app = WebApp(
            name = "App",
            url = "https://example.onion",
            useTor = true,
            customProxyType = ProxyType.SOCKS5,
            customProxyHost = "10.0.0.1",
            customProxyPort = 1080,
        )
        assertEquals(ProxyConfig.Socks5("10.0.0.1", 1080, null, null), engine.proxyConfigFor(app))
    }
}
