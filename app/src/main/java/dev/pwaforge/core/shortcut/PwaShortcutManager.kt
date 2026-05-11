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

    fun createShortcut(context: Context, app: WebApp): Boolean = runCatching {
        val icon = ShortcutIconBuilder.build(context, app)
        val info = ShortcutInfoCompat.Builder(context, "pwa_${app.isolationId}")
            .setShortLabel(app.name.take(12))
            .setLongLabel(app.name)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setIntent(
                Intent(context, ShortcutActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(EXTRA_APP_ID, app.id)
                }
            )
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }.getOrDefault(false)
}
