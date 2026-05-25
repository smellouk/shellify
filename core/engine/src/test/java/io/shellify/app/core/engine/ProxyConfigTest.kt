package io.shellify.app.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProxyConfigTest {

    @Test
    fun `Socks5 data class equality holds for same host and port`() {
        assertEquals(
            ProxyConfig.Socks5("127.0.0.1", 9050),
            ProxyConfig.Socks5("127.0.0.1", 9050),
        )
    }

    @Test
    fun `Socks5 with different port produces different key`() {
        assertNotEquals(
            ProxyConfig.Socks5("127.0.0.1", 9050),
            ProxyConfig.Socks5("127.0.0.1", 9051),
        )
    }

    @Test
    fun `None singleton hashCode is stable`() {
        assertEquals(ProxyConfig.None.hashCode(), ProxyConfig.None.hashCode())
    }

    @Test
    fun `None is not equal to Socks5`() {
        assertNotEquals(ProxyConfig.None, ProxyConfig.Socks5("127.0.0.1", 9050))
    }
}
