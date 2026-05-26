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

    // New tests for credential-bearing variants

    @Test
    fun `Socks5 with explicit nulls equals default two-arg constructor`() {
        assertEquals(
            ProxyConfig.Socks5("h", 1, null, null),
            ProxyConfig.Socks5("h", 1),
        )
    }

    @Test
    fun `Socks5 inequality when one credential differs`() {
        assertNotEquals(
            ProxyConfig.Socks5("h", 1, "u", "p"),
            ProxyConfig.Socks5("h", 1, "u", "DIFFERENT"),
        )
    }

    @Test
    fun `Http accepts optional credentials symmetrically`() {
        assertEquals(
            ProxyConfig.Http("h", 8080, null, null),
            ProxyConfig.Http("h", 8080),
        )
        assertNotEquals(
            ProxyConfig.Http("h", 8080, "u", "p"),
            ProxyConfig.Http("h", 8080, "u", "OTHER"),
        )
    }
}
