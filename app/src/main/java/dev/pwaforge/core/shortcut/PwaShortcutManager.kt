package dev.pwaforge.core.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.pwaforge.domain.model.WebApp

object PwaShortcutManager {

    const val EXTRA_APP_ID = "app_id"

    private var shortcutActivityClass: Class<*>? = null

    fun init(activityClass: Class<*>) {
        shortcutActivityClass = activityClass
    }

    private fun shortcutId(app: WebApp) = "pwa_${app.isolationId}"

    private fun buildIntent(context: Context, appId: Long): Intent {
        val cls = checkNotNull(shortcutActivityClass) { "PwaShortcutManager.init() must be called before use" }
        return Intent(context, cls).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_APP_ID, appId)
        }
    }

    private fun buildInfo(context: Context, app: WebApp, label: String): ShortcutInfoCompat {
        val icon = ShortcutIconBuilder.build(context, app)
        return ShortcutInfoCompat.Builder(context, shortcutId(app))
            .setShortLabel(label.take(12))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithAdaptiveBitmap(icon))
            .setIntent(buildIntent(context, app.id))
            .build()
    }

    fun getPinnedShortcuts(context: Context): List<ShortcutInfoCompat> = runCatching {
        ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
    }.getOrDefault(emptyList())

    fun removeShortcut(context: Context, app: WebApp) {
        val id = shortcutId(app)
        runCatching { ShortcutManagerCompat.disableShortcuts(context, listOf(id), "This app has been removed") }
        runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id)) }
    }

    fun createShortcut(context: Context, app: WebApp): Boolean = runCatching {
        val id = shortcutId(app)
        val alreadyPinned = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .any { it.id == id }
        if (alreadyPinned) {
            ShortcutManagerCompat.enableShortcuts(context, listOf(buildInfo(context, app, app.name)))
            ShortcutManagerCompat.updateShortcuts(context, listOf(buildInfo(context, app, app.name)))
            true
        } else {
            ShortcutManagerCompat.requestPinShortcut(context, buildInfo(context, app, app.name), null)
        }
    }.getOrDefault(false)

    fun rename(context: Context, app: WebApp, newLabel: String): Boolean = runCatching {
        ShortcutManagerCompat.updateShortcuts(context, listOf(buildInfo(context, app, newLabel)))
    }.getOrDefault(false)

    fun refreshIcon(context: Context, app: WebApp, currentLabel: String): Boolean = runCatching {
        ShortcutManagerCompat.updateShortcuts(context, listOf(buildInfo(context, app, currentLabel)))
    }.getOrDefault(false)

    fun changeIcon(context: Context, app: WebApp, currentLabel: String, bitmap: Bitmap): Boolean = runCatching {
        val info = ShortcutInfoCompat.Builder(context, shortcutId(app))
            .setShortLabel(currentLabel.take(12))
            .setLongLabel(currentLabel)
            .setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
            .setIntent(buildIntent(context, app.id))
            .build()
        ShortcutManagerCompat.updateShortcuts(context, listOf(info))
    }.getOrDefault(false)
}
