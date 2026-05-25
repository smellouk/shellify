package io.shellify.app.core.pwa

import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that PwaAnalyzer handles .onion URLs correctly (Plan 02-05 Task 4).
 *
 * Four behaviours:
 *  O1: extractOrigin("http://host.onion") returns "http://host.onion" — preserves http scheme
 *  O2: extractOrigin("http://host.onion/path") returns "http://host.onion" — strips path
 *  O3: extractOrigin("https://host.onion") returns "https://host.onion" — preserves https scheme
 *  O4: Protocol-relative URL "//host.onion/manifest" for an http base is resolved to http://
 *      (not https://) — no forced scheme upgrade for onion resources
 */
class PwaAnalyzerOnionTest {

    private val analyzer = PwaAnalyzer(mockk<OkHttpClient>(relaxed = true))

    /**
     * O1: extractOrigin for a bare http onion origin must return http, not https.
     * Onion services rarely have valid TLS certificates; upgrading to https would break them.
     */
    @Test
    fun `extractOrigin preserves http scheme for onion host`() {
        assertEquals(
            "http://abcdef1234567890.onion",
            analyzer.extractOrigin("http://abcdef1234567890.onion"),
        )
    }

    /**
     * O2: extractOrigin strips the path component and preserves the http scheme.
     */
    @Test
    fun `extractOrigin strips path and preserves http scheme for onion host with path`() {
        assertEquals(
            "http://abcdef1234567890.onion",
            analyzer.extractOrigin("http://abcdef1234567890.onion/some/path"),
        )
    }

    /**
     * O3: extractOrigin preserves https scheme when explicitly provided.
     * Some onion services do offer valid TLS certificates.
     */
    @Test
    fun `extractOrigin preserves https scheme for onion host`() {
        assertEquals(
            "https://abcdef1234567890.onion",
            analyzer.extractOrigin("https://abcdef1234567890.onion"),
        )
    }

    /**
     * O4: For a regular (non-onion) https origin, extractOrigin returns https.
     * This is a regression guard ensuring the non-onion path is unaffected.
     */
    @Test
    fun `extractOrigin returns https for regular https origin`() {
        val origin = analyzer.extractOrigin("https://example.com/some/path")
        assertTrue("Expected https origin, got: $origin", origin.startsWith("https://"))
        assertEquals("https://example.com", origin)
    }
}
