package io.shellify.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProxyTypeTest {

    @Test
    fun `ProxyType has exactly three entries`() {
        assertEquals(3, ProxyType.values().size)
    }

    @Test
    fun `valueOf returns NONE for NONE string`() {
        assertEquals(ProxyType.NONE, ProxyType.valueOf("NONE"))
    }

    @Test
    fun `valueOf returns SOCKS5 for SOCKS5 string`() {
        assertEquals(ProxyType.SOCKS5, ProxyType.valueOf("SOCKS5"))
    }

    @Test
    fun `valueOf returns HTTP for HTTP string`() {
        assertEquals(ProxyType.HTTP, ProxyType.valueOf("HTTP"))
    }

    @Test
    fun `WebApp default proxy type is NONE`() {
        val app = WebApp(name = "Test", url = "https://example.com")
        assertEquals(ProxyType.NONE, app.customProxyType)
    }

    @Test
    fun `WebApp default proxy host is null`() {
        val app = WebApp(name = "Test", url = "https://example.com")
        assertNull(app.customProxyHost)
    }

    @Test
    fun `WebApp default proxy port is zero`() {
        val app = WebApp(name = "Test", url = "https://example.com")
        assertEquals(0, app.customProxyPort)
    }

    @Test
    fun `WebApp default proxy username is null`() {
        val app = WebApp(name = "Test", url = "https://example.com")
        assertNull(app.customProxyUsername)
    }

    @Test
    fun `WebApp default proxy password is null`() {
        val app = WebApp(name = "Test", url = "https://example.com")
        assertNull(app.customProxyPassword)
    }

    @Test
    fun `WebApp data class equality holds when proxy fields match`() {
        val app1 = WebApp(
            name = "Test",
            url = "https://example.com",
            customProxyType = ProxyType.SOCKS5,
            customProxyHost = "10.0.0.1",
            customProxyPort = 1080,
            customProxyUsername = "alice",
            customProxyPassword = "secret",
        )
        val app2 = app1.copy()
        assertEquals(app1, app2)
    }
}
