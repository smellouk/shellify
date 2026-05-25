package io.shellify.app.util

import io.shellify.app.data.local.entity.CategoryEntity
import io.shellify.app.data.local.entity.WebAppEntity
import io.shellify.app.domain.model.Category
import io.shellify.app.domain.model.WebApp
import java.util.UUID

/**
 * Centralized test data builders for instrumented tests.
 * Provides consistent fake data across all test classes.
 */
object FakeData {

    // ─── WebApp domain model ───────────────────────────────────────────────────

    fun webApp(
        id: Long = 0L,
        name: String = "Test App",
        url: String = "https://example.com",
        categoryId: Long? = null,
        isolationId: String = UUID.randomUUID().toString(),
        isFullscreen: Boolean = false,
        adBlockEnabled: Boolean = true,
        translateEnabled: Boolean = false,
        // Privacy
        alwaysIncognito: Boolean = false,
        trackerBlockingEnabled: Boolean = false,
        // Tor
        useTor: Boolean = false,
        preserveTorIdentity: Boolean = false,
    ): WebApp = WebApp(
        id = id,
        name = name,
        url = url,
        categoryId = categoryId,
        isolationId = isolationId,
        isFullscreen = isFullscreen,
        adBlockEnabled = adBlockEnabled,
        translateEnabled = translateEnabled,
        alwaysIncognito = alwaysIncognito,
        trackerBlockingEnabled = trackerBlockingEnabled,
        useTor = useTor,
        preserveTorIdentity = preserveTorIdentity,
    )

    fun webAppList(count: Int = 3): List<WebApp> = (1..count).map { i ->
        webApp(
            id = i.toLong(),
            name = "App $i",
            url = "https://app$i.example.com",
        )
    }

    // ─── Category domain model ─────────────────────────────────────────────────

    fun category(
        id: Long = 0L,
        name: String = "Test Category",
        sortIndex: Int = 0,
        icon: String = "folder",
        color: String = "#6D28D9",
    ): Category = Category(
        id = id,
        name = name,
        sortIndex = sortIndex,
        icon = icon,
        color = color,
    )

    fun categoryList(count: Int = 2): List<Category> = (1..count).map { i ->
        category(id = i.toLong(), name = "Category $i", sortIndex = i - 1)
    }

    // ─── WebAppEntity (Room) ───────────────────────────────────────────────────

    fun webAppEntity(
        id: Long = 0L,
        name: String = "Test App",
        url: String = "https://example.com",
        categoryId: Long? = null,
        isolationId: String = UUID.randomUUID().toString(),
        isFullscreen: Boolean = false,
        adBlockEnabled: Boolean = true,
    ): WebAppEntity = WebAppEntity(
        id = id,
        name = name,
        url = url,
        categoryId = categoryId,
        isolationId = isolationId,
        isFullscreen = isFullscreen,
        adBlockEnabled = adBlockEnabled,
    )

    // ─── CategoryEntity (Room) ─────────────────────────────────────────────────

    fun categoryEntity(
        id: Long = 0L,
        name: String = "Test Category",
        sortIndex: Int = 0,
        icon: String = "folder",
        color: String = "#6D28D9",
    ): CategoryEntity = CategoryEntity(
        id = id,
        name = name,
        sortIndex = sortIndex,
        icon = icon,
        color = color,
    )
}
