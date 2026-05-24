package io.shellify.app.data.mapper

import io.shellify.app.data.local.entity.NetworkRequestLogEntity
import io.shellify.app.domain.model.NetworkRequestLog
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkRequestLogMapperTest {

    private val sampleEntity = NetworkRequestLogEntity(
        id = 42L,
        appId = 1L,
        sessionId = "session-abc",
        hostname = "example.com",
        url = "https://example.com/api/data",
        isBlocked = false,
        timestamp = 1_700_000_000_000L,
    )

    private val sampleDomain = NetworkRequestLog(
        id = 42L,
        appId = 1L,
        sessionId = "session-abc",
        hostname = "example.com",
        url = "https://example.com/api/data",
        isBlocked = false,
        timestamp = 1_700_000_000_000L,
    )

    // ── toDomain ──────────────────────────────────────────────────────────────

    @Test
    fun `toDomain maps all fields correctly`() {
        val domain = sampleEntity.toDomain()
        assertEquals(42L, domain.id)
        assertEquals(1L, domain.appId)
        assertEquals("session-abc", domain.sessionId)
        assertEquals("example.com", domain.hostname)
        assertEquals("https://example.com/api/data", domain.url)
        assertEquals(false, domain.isBlocked)
        assertEquals(1_700_000_000_000L, domain.timestamp)
    }

    @Test
    fun `toDomain maps isBlocked true correctly`() {
        val entity = sampleEntity.copy(isBlocked = true)
        assertEquals(true, entity.toDomain().isBlocked)
    }

    @Test
    fun `toDomain maps isBlocked false correctly`() {
        val entity = sampleEntity.copy(isBlocked = false)
        assertEquals(false, entity.toDomain().isBlocked)
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    fun `toEntity maps all fields correctly`() {
        val entity = sampleDomain.toEntity()
        assertEquals(42L, entity.id)
        assertEquals(1L, entity.appId)
        assertEquals("session-abc", entity.sessionId)
        assertEquals("example.com", entity.hostname)
        assertEquals("https://example.com/api/data", entity.url)
        assertEquals(false, entity.isBlocked)
        assertEquals(1_700_000_000_000L, entity.timestamp)
    }

    @Test
    fun `toEntity maps isBlocked true correctly`() {
        val domain = sampleDomain.copy(isBlocked = true)
        assertEquals(true, domain.toEntity().isBlocked)
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    fun `round-trip entity to domain and back preserves all fields`() {
        val roundTripped = sampleEntity.toDomain().toEntity()
        assertEquals(sampleEntity, roundTripped)
    }

    @Test
    fun `round-trip domain to entity and back preserves all fields`() {
        val roundTripped = sampleDomain.toEntity().toDomain()
        assertEquals(sampleDomain, roundTripped)
    }
}
