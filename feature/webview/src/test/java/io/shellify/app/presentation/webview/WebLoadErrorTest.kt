package io.shellify.app.presentation.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebLoadErrorTest {

    @Test
    fun `from maps ERR_INTERNET_DISCONNECTED to NoInternet`() {
        assertEquals(WebLoadError.NoInternet, WebLoadError.from(-2, "net::ERR_INTERNET_DISCONNECTED"))
    }

    @Test
    fun `from maps ERR_NETWORK_CHANGED to NoInternet`() {
        assertEquals(WebLoadError.NoInternet, WebLoadError.from(-21, "net::ERR_NETWORK_CHANGED"))
    }

    @Test
    fun `from maps ERR_NAME_NOT_RESOLVED to CannotReach`() {
        assertEquals(WebLoadError.CannotReach, WebLoadError.from(-2, "net::ERR_NAME_NOT_RESOLVED"))
    }

    @Test
    fun `from maps ERR_ADDRESS_UNREACHABLE to CannotReach`() {
        assertEquals(WebLoadError.CannotReach, WebLoadError.from(-15, "net::ERR_ADDRESS_UNREACHABLE"))
    }

    @Test
    fun `from maps ERR_CONNECTION_REFUSED to CannotReach`() {
        assertEquals(WebLoadError.CannotReach, WebLoadError.from(-102, "net::ERR_CONNECTION_REFUSED"))
    }

    @Test
    fun `from maps ERR_CONNECTION_RESET to CannotReach`() {
        assertEquals(WebLoadError.CannotReach, WebLoadError.from(-101, "net::ERR_CONNECTION_RESET"))
    }

    @Test
    fun `from maps ERR_TIMED_OUT to Timeout`() {
        assertEquals(WebLoadError.Timeout, WebLoadError.from(-7, "net::ERR_TIMED_OUT"))
    }

    @Test
    fun `from maps ERR_CONNECTION_TIMED_OUT to Timeout`() {
        assertEquals(WebLoadError.Timeout, WebLoadError.from(-118, "net::ERR_CONNECTION_TIMED_OUT"))
    }

    @Test
    fun `from maps ERR_SSL_PROTOCOL_ERROR to SslError`() {
        assertEquals(WebLoadError.SslError, WebLoadError.from(-107, "net::ERR_SSL_PROTOCOL_ERROR"))
    }

    @Test
    fun `from maps errorCode minus11 to SslError`() {
        assertEquals(WebLoadError.SslError, WebLoadError.from(-11, "SSL handshake failed"))
    }

    @Test
    fun `from maps unknown description to Generic`() {
        val error = WebLoadError.from(-1, "net::ERR_FAILED")
        assertTrue(error is WebLoadError.Generic)
        assertEquals("net::ERR_FAILED", (error as WebLoadError.Generic).description)
    }

    @Test
    fun `from matching is case-insensitive`() {
        assertEquals(WebLoadError.NoInternet, WebLoadError.from(-2, "Net::ERR_INTERNET_DISCONNECTED"))
    }
}
