package dev.pwaforge.core.shortcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.drawable.toBitmap
import dev.pwaforge.domain.model.WebApp
import java.io.File

object ShortcutIconBuilder {

    private const val SIZE = 192

    fun build(context: Context, app: WebApp): Bitmap {
        val iconBitmap = app.iconPath?.let { loadFile(it) }
        if (iconBitmap != null) return scaleCentered(iconBitmap)
        // Fallback 1: PWAForge launcher icon (already polished)
        val launcherBitmap = launcherIconBitmap(context)
        if (launcherBitmap != null) return launcherBitmap
        // Fallback 2: generated letter avatar
        return generateLetterAvatar(app.name, app.themeColor)
    }

    private fun loadFile(path: String): Bitmap? = runCatching {
        BitmapFactory.decodeFile(path)
    }.getOrNull()

    private fun launcherIconBitmap(context: Context): Bitmap? = runCatching {
        context.packageManager
            .getApplicationIcon(context.packageName)
            .toBitmap(SIZE, SIZE)
    }.getOrNull()

    private fun scaleCentered(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val scale = SIZE.toFloat() / maxOf(src.width, src.height)
        val w = (src.width * scale).toInt()
        val h = (src.height * scale).toInt()
        val left = (SIZE - w) / 2f
        val top = (SIZE - h) / 2f
        canvas.drawBitmap(Bitmap.createScaledBitmap(src, w, h, true), left, top, null)
        return out
    }

    private fun generateLetterAvatar(name: String, themeColor: String?): Bitmap {
        val bg = runCatching { Color.parseColor(themeColor) }.getOrDefault(0xFF1976D2.toInt())
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        canvas.drawRoundRect(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()), 40f, 40f, bgPaint)
        val letter = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "P"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = SIZE * 0.48f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, SIZE / 2f, SIZE / 2f - yOffset, textPaint)
        return out
    }
}
