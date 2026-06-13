package io.shellify.app.core.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [isExternalScheme], the scheme guard shared by the System WebView main frame and
 * its OAuth popup frame. http(s) navigations (including OAuth redirects to accounts.google.com)
 * must stay inside the WebView; everything else is handed to the host as an external link.
 */
class ExternalSchemeTest {

    @Test
    fun `https url is not external`() {
        assertFalse(isExternalScheme("https://accounts.google.com/o/oauth2/auth"))
    }

    @Test
    fun `http url is not external`() {
        assertFalse(isExternalScheme("http://example.com/login"))
    }

    @Test
    fun `scheme matching is case-insensitive`() {
        assertFalse(isExternalScheme("HTTPS://example.com"))
    }

    @Test
    fun `data url is not external`() {
        // OAuth popups and JS-generated documents load via data: — must stay in the WebView,
        // otherwise the popup is sent to startActivity() and its script never runs.
        assertFalse(isExternalScheme("data:text/html;base64,PGgxPmhpPC9oMT4="))
    }

    @Test
    fun `blob url is not external`() {
        assertFalse(isExternalScheme("blob:https://example.com/abc-123"))
    }

    @Test
    fun `about blank is not external`() {
        assertFalse(isExternalScheme("about:blank"))
    }

    @Test
    fun `javascript scheme is not external`() {
        assertFalse(isExternalScheme("javascript:void(0)"))
    }

    @Test
    fun `tel scheme is external`() {
        assertTrue(isExternalScheme("tel:+15551234567"))
    }

    @Test
    fun `mailto scheme is external`() {
        assertTrue(isExternalScheme("mailto:user@example.com"))
    }

    @Test
    fun `intent scheme is external`() {
        assertTrue(isExternalScheme("intent://scan/#Intent;scheme=zxing;end"))
    }

    @Test
    fun `custom oauth callback scheme is external`() {
        assertTrue(isExternalScheme("myapp://oauth/callback?code=abc"))
    }
}
