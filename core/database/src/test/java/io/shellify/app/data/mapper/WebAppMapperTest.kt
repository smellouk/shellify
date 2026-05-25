package io.shellify.app.data.mapper

import io.shellify.app.data.local.entity.WebAppEntity
import io.shellify.app.domain.model.EngineType
import io.shellify.app.domain.model.IconSource
import io.shellify.app.domain.model.LockType
import io.shellify.app.domain.model.TranslateLanguage
import io.shellify.app.domain.model.UserAgentMode
import io.shellify.app.domain.model.WebApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAppMapperTest {

    private val isolationId = "550e8400-e29b-41d4-a716-446655440000"

    private fun baseEntity() = WebAppEntity(
        id = 1,
        name = "GitHub",
        url = "https://github.com",
        isolationId = isolationId,
    )

    private fun baseApp() = WebApp(
        id = 1,
        name = "GitHub",
        url = "https://github.com",
        isolationId = isolationId,
    )

    // ── toDomain ──────────────────────────────────────────────────────────────

    @Test
    fun `toDomain maps basic fields correctly`() {
        val entity = baseEntity()
        val domain = entity.toDomain()
        assertEquals(1L, domain.id)
        assertEquals("GitHub", domain.name)
        assertEquals("https://github.com", domain.url)
        assertEquals(isolationId, domain.isolationId)
    }

    @Test
    fun `toDomain uses iconSource when present and ignores iconPath`() {
        val source = IconSource.Path("/icons/github.png")
        val entity = baseEntity().copy(iconSource = source, iconPath = "/old/path.png")
        val domain = entity.toDomain()
        assertEquals(source, domain.iconSource)
    }

    @Test
    fun `toDomain falls back to legacy iconPath when iconSource is null`() {
        val entity = baseEntity().copy(iconSource = null, iconPath = "/legacy/icon.png")
        val domain = entity.toDomain()
        assertEquals(IconSource.Path("/legacy/icon.png"), domain.iconSource)
    }

    @Test
    fun `toDomain returns null iconSource when both are null`() {
        val entity = baseEntity().copy(iconSource = null, iconPath = null)
        val domain = entity.toDomain()
        assertNull(domain.iconSource)
    }

    @Test
    fun `toDomain parses valid uaMode enum`() {
        val entity = baseEntity().copy(uaMode = "CHROME_DESKTOP")
        assertEquals(UserAgentMode.CHROME_DESKTOP, entity.toDomain().uaMode)
    }

    @Test
    fun `toDomain defaults to CHROME_MOBILE for invalid uaMode`() {
        val entity = baseEntity().copy(uaMode = "INVALID_MODE")
        assertEquals(UserAgentMode.CHROME_MOBILE, entity.toDomain().uaMode)
    }

    @Test
    fun `toDomain parses valid engineType enum`() {
        val entity = baseEntity().copy(engineType = "GECKOVIEW")
        assertEquals(EngineType.GECKOVIEW, entity.toDomain().engineType)
    }

    @Test
    fun `toDomain defaults to SYSTEM_WEBVIEW for invalid engineType`() {
        val entity = baseEntity().copy(engineType = "UNKNOWN_ENGINE")
        assertEquals(EngineType.SYSTEM_WEBVIEW, entity.toDomain().engineType)
    }

    @Test
    fun `toDomain parses valid lockType enum`() {
        val entity = baseEntity().copy(lockType = "PASSWORD")
        assertEquals(LockType.PASSWORD, entity.toDomain().lockType)
    }

    @Test
    fun `toDomain defaults to NONE for invalid lockType`() {
        val entity = baseEntity().copy(lockType = "BAD_LOCK")
        assertEquals(LockType.NONE, entity.toDomain().lockType)
    }

    @Test
    fun `toDomain finds translateTarget by code`() {
        val entity = baseEntity().copy(translateTarget = "fr")
        assertEquals(TranslateLanguage.FRENCH, entity.toDomain().translateTarget)
    }

    @Test
    fun `toDomain defaults to ENGLISH for unknown translateTarget code`() {
        val entity = baseEntity().copy(translateTarget = "xx")
        assertEquals(TranslateLanguage.ENGLISH, entity.toDomain().translateTarget)
    }

    @Test
    fun `toDomain splits adBlockCustomRules on newlines`() {
        val entity = baseEntity().copy(adBlockCustomRules = "rule1\nrule2\nrule3")
        assertEquals(listOf("rule1", "rule2", "rule3"), entity.toDomain().adBlockCustomRules)
    }

    @Test
    fun `toDomain filters blank lines in adBlockCustomRules`() {
        val entity = baseEntity().copy(adBlockCustomRules = "rule1\n\n\nrule2")
        assertEquals(listOf("rule1", "rule2"), entity.toDomain().adBlockCustomRules)
    }

    @Test
    fun `toDomain returns empty list for empty adBlockCustomRules`() {
        val entity = baseEntity().copy(adBlockCustomRules = "")
        assertTrue(entity.toDomain().adBlockCustomRules.isEmpty())
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    fun `toEntity maps basic fields correctly`() {
        val app = baseApp()
        val entity = app.toEntity()
        assertEquals(1L, entity.id)
        assertEquals("GitHub", entity.name)
        assertEquals("https://github.com", entity.url)
        assertEquals(isolationId, entity.isolationId)
    }

    @Test
    fun `toEntity stores uaMode as enum name string`() {
        val app = baseApp().copy(uaMode = UserAgentMode.SAFARI_MOBILE)
        assertEquals("SAFARI_MOBILE", app.toEntity().uaMode)
    }

    @Test
    fun `toEntity stores engineType as enum name string`() {
        val app = baseApp().copy(engineType = EngineType.GECKOVIEW)
        assertEquals("GECKOVIEW", app.toEntity().engineType)
    }

    @Test
    fun `toEntity stores lockType as enum name string`() {
        val app = baseApp().copy(lockType = LockType.SYSTEM)
        assertEquals("SYSTEM", app.toEntity().lockType)
    }

    @Test
    fun `toEntity stores translateTarget as language code`() {
        val app = baseApp().copy(translateTarget = TranslateLanguage.JAPANESE)
        assertEquals("ja", app.toEntity().translateTarget)
    }

    @Test
    fun `toEntity joins adBlockCustomRules with newlines`() {
        val app = baseApp().copy(adBlockCustomRules = listOf("rule1", "rule2", "rule3"))
        assertEquals("rule1\nrule2\nrule3", app.toEntity().adBlockCustomRules)
    }

    @Test
    fun `toEntity stores empty string for empty adBlockCustomRules list`() {
        val app = baseApp().copy(adBlockCustomRules = emptyList())
        assertEquals("", app.toEntity().adBlockCustomRules)
    }

    @Test
    fun `toEntity preserves iconSource`() {
        val source = IconSource.SvgIcon("react", "#61dafb", "/cache/react.png")
        val app = baseApp().copy(iconSource = source)
        assertEquals(source, app.toEntity().iconSource)
    }

    @Test
    fun `toDomain maps showControlCenter true by default`() {
        assertEquals(true, baseEntity().toDomain().showControlCenter)
    }

    @Test
    fun `toDomain maps showControlCenter false`() {
        val entity = baseEntity().copy(showControlCenter = false)
        assertEquals(false, entity.toDomain().showControlCenter)
    }

    @Test
    fun `toEntity preserves showControlCenter false`() {
        val app = baseApp().copy(showControlCenter = false)
        assertEquals(false, app.toEntity().showControlCenter)
    }

    // ── swipeToRefreshEnabled ─────────────────────────────────────────────────

    @Test
    fun `toDomain maps swipeToRefreshEnabled true by default`() {
        assertEquals(true, baseEntity().toDomain().swipeToRefreshEnabled)
    }

    @Test
    fun `toDomain maps swipeToRefreshEnabled true`() {
        val entity = baseEntity().copy(swipeToRefreshEnabled = true)
        assertEquals(true, entity.toDomain().swipeToRefreshEnabled)
    }

    @Test
    fun `toDomain maps swipeToRefreshEnabled false`() {
        val entity = baseEntity().copy(swipeToRefreshEnabled = false)
        assertEquals(false, entity.toDomain().swipeToRefreshEnabled)
    }

    @Test
    fun `toEntity preserves swipeToRefreshEnabled true`() {
        val app = baseApp().copy(swipeToRefreshEnabled = true)
        assertEquals(true, app.toEntity().swipeToRefreshEnabled)
    }

    @Test
    fun `toEntity preserves swipeToRefreshEnabled false`() {
        val app = baseApp().copy(swipeToRefreshEnabled = false)
        assertEquals(false, app.toEntity().swipeToRefreshEnabled)
    }

    @Test
    fun `toEntity round-trip preserves swipeToRefreshEnabled true`() {
        val app = baseApp().copy(swipeToRefreshEnabled = true)
        assertEquals(true, app.toEntity().toDomain().swipeToRefreshEnabled)
    }

    @Test
    fun `toEntity round-trip preserves swipeToRefreshEnabled false`() {
        val app = baseApp().copy(swipeToRefreshEnabled = false)
        assertEquals(false, app.toEntity().toDomain().swipeToRefreshEnabled)
    }

    // ── Privacy + Tor fields (Phase 2) ────────────────────────────────────────

    @Test
    fun `WebApp constructed with defaults has all privacy and tor fields false`() {
        val app = baseApp()
        assertEquals(false, app.stealthMode)
        assertEquals(false, app.cookieAutoWipe)
        assertEquals(false, app.alwaysIncognito)
        assertEquals(false, app.trackerBlockingEnabled)
        assertEquals(false, app.useTor)
        assertEquals(false, app.preserveTorIdentity)
    }

    @Test
    fun `toEntity copies all 6 new boolean fields into WebAppEntity`() {
        val app = baseApp().copy(
            stealthMode = true,
            cookieAutoWipe = true,
            alwaysIncognito = true,
            trackerBlockingEnabled = true,
            useTor = true,
            preserveTorIdentity = true,
        )
        val entity = app.toEntity()
        assertEquals(true, entity.stealthMode)
        assertEquals(true, entity.cookieAutoWipe)
        assertEquals(true, entity.alwaysIncognito)
        assertEquals(true, entity.trackerBlockingEnabled)
        assertEquals(true, entity.useTor)
        assertEquals(true, entity.preserveTorIdentity)
    }

    @Test
    fun `toDomain copies all 6 new boolean fields back into WebApp`() {
        val entity = baseEntity().copy(
            stealthMode = true,
            cookieAutoWipe = true,
            alwaysIncognito = true,
            trackerBlockingEnabled = true,
            useTor = true,
            preserveTorIdentity = true,
        )
        val app = entity.toDomain()
        assertEquals(true, app.stealthMode)
        assertEquals(true, app.cookieAutoWipe)
        assertEquals(true, app.alwaysIncognito)
        assertEquals(true, app.trackerBlockingEnabled)
        assertEquals(true, app.useTor)
        assertEquals(true, app.preserveTorIdentity)
    }

    @Test
    fun `round-trip toEntity then toDomain preserves all 6 new booleans when true`() {
        val app = baseApp().copy(
            stealthMode = true,
            cookieAutoWipe = true,
            alwaysIncognito = true,
            trackerBlockingEnabled = true,
            useTor = true,
            preserveTorIdentity = true,
        )
        val roundTripped = app.toEntity().toDomain()
        assertEquals(true, roundTripped.stealthMode)
        assertEquals(true, roundTripped.cookieAutoWipe)
        assertEquals(true, roundTripped.alwaysIncognito)
        assertEquals(true, roundTripped.trackerBlockingEnabled)
        assertEquals(true, roundTripped.useTor)
        assertEquals(true, roundTripped.preserveTorIdentity)
    }

    @Test
    fun `round-trip toEntity then toDomain preserves all 6 new booleans when false`() {
        val app = baseApp().copy(
            stealthMode = false,
            cookieAutoWipe = false,
            alwaysIncognito = false,
            trackerBlockingEnabled = false,
            useTor = false,
            preserveTorIdentity = false,
        )
        val roundTripped = app.toEntity().toDomain()
        assertEquals(false, roundTripped.stealthMode)
        assertEquals(false, roundTripped.cookieAutoWipe)
        assertEquals(false, roundTripped.alwaysIncognito)
        assertEquals(false, roundTripped.trackerBlockingEnabled)
        assertEquals(false, roundTripped.useTor)
        assertEquals(false, roundTripped.preserveTorIdentity)
    }
}
