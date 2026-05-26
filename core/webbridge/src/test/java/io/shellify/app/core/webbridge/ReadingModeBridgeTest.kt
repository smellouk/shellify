package io.shellify.app.core.webbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingModeBridgeTest {

    private val fakeReadabilityJs =
        "function Readability(doc){} Readability.prototype.parse=function(){return null;};"
    private val noContentMessage = "No content"

    @Test
    fun `buildScript returns non-empty string with load guard`() {
        val script = ReadingModeBridge.buildScript(fakeReadabilityJs, noContentMessage)
        assertTrue("Script must not be empty", script.isNotEmpty())
        assertTrue(
            "Script must contain the idempotency guard __shellifyReaderLoaded",
            script.contains("__shellifyReaderLoaded"),
        )
    }

    @Test
    fun `buildScript contains Readability constructor call`() {
        val script = ReadingModeBridge.buildScript(fakeReadabilityJs, noContentMessage)
        assertTrue(
            "Script must invoke new Readability()",
            script.contains("new Readability("),
        )
    }

    @Test
    fun `buildScript contains null-check for article`() {
        val script = ReadingModeBridge.buildScript(fakeReadabilityJs, noContentMessage)
        assertTrue(
            "Script must guard against null parse result",
            script.contains("if (!article)") || script.contains("if(!article)"),
        )
    }

    @Test
    fun `buildScript is stable across calls`() {
        val first = ReadingModeBridge.buildScript(fakeReadabilityJs, noContentMessage)
        val second = ReadingModeBridge.buildScript(fakeReadabilityJs, noContentMessage)
        assertEquals("buildScript() must be deterministic", first, second)
    }
}
