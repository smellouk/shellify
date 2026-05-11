package dev.pwaforge.core.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.presentation.shortcut.ShortcutActivity

object PwaShortcutManager {

    const val EXTRA_APP_ID = "app_id"

    private fun shortcutId(app: WebApp) = "pwa_${app.isolationId}"

    private fun buildInfo(context: Context, app: WebApp, label: String): ShortcutInfoCompat {
        val icon = ShortcutIconBuilder.build(context, app)
        return ShortcutInfoCompat.Builder(context, shortcutId(app))
            .setShortLabel(label.take(12))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setIntent(
                Intent(context, ShortcutActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(EXTRA_APP_ID, app.id)
                }
            )
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
        ShortcutManagerCompat.requestPinShortcut(context, buildInfo(context, app, app.name), null)
    }.getOrDefault(false)

    fun rename(context: Context, app: WebApp, newLabel: String): Boolean = runCatching {
        ShortcutManagerCompat.updateShortcuts(context, listOf(buildInfo(context, app, newLabel)))
    }.getOrDefault(false)

    fun refreshIcon(context: Context, app: WebApp, currentLabel: String): Boolean = runCatching {
        ShortcutManagerCompat.updateShortcuts(context, listOf(buildInfo(context, app, currentLabel)))
    }.getOrDefault(false)
}
