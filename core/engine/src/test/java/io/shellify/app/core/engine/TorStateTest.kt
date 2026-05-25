package io.shellify.app.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TorStateTest {

    @Test
    fun `Stopped equals Stopped`() {
        assertEquals(TorState.Stopped, TorState.Stopped)
    }

    @Test
    fun `Error with different messages are not equal`() {
        assertNotEquals(TorState.Error("a"), TorState.Error("b"))
    }

    @Test
    fun `Ready is not equal to Connecting`() {
        assertNotEquals(TorState.Ready, TorState.Connecting)
    }
}
