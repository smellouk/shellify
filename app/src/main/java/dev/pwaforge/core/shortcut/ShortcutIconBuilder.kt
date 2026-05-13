package dev.pwaforge.core.shortcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import androidx.core.graphics.drawable.toBitmap
import dev.pwaforge.domain.model.IconSource
import dev.pwaforge.domain.model.WebApp
import java.io.File

object ShortcutIconBuilder {

    private const val SIZE = 192
    private const val ADAPTIVE_SIZE = 432   // 108dp at 4× — safe for all densities
    private const val SVG_ICON_SIZE = 108  // ~56% of canvas, centered

    fun build(context: Context, app: WebApp): Bitmap {
        return when (val src = app.iconSource) {
            is IconSource.SvgIcon -> {
                val rendered = src.renderedPath?.let { loadFile(it) }
                if (rendered != null) buildAdaptiveSvg(src.background, rendered)
                else fallback(context, app)
            }
            is IconSource.Path -> {
                val bmp = loadFile(src.path)
                if (bmp != null) buildAdaptivePath(bmp) else fallback(context, app)
            }
            null -> fallback(context, app)
        }
    }

    private fun buildAdaptivePath(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(ADAPTIVE_SIZE, ADAPTIVE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        // Scale proportionally so the icon fits within ~56% of canvas, centered
        val maxDraw = (ADAPTIVE_SIZE * 0.40f).toInt()
        val scale = maxDraw.toFloat() / maxOf(src.width, src.height)
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        canvas.drawBitmap(scaled, (ADAPTIVE_SIZE - w) / 2f, (ADAPTIVE_SIZE - h) / 2f, null)
        return out
    }

    private fun buildAdaptiveSvg(background: String, renderedIcon: Bitmap): Bitmap {
        val bgColor = runCatching { Color.parseColor(background) }.getOrDefault(0xFF1976D2.toInt())
        val out = Bitmap.createBitmap(ADAPTIVE_SIZE, ADAPTIVE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(bgColor)
        // Scale to 50% of canvas; the inner icon lands at ~36% of canvas, giving ~65px padding per side inside the safe zone
        val drawSize = (ADAPTIVE_SIZE * 0.50f).toInt()
        val offset = (ADAPTIVE_SIZE - drawSize) / 2f
        val scaled = Bitmap.createScaledBitmap(renderedIcon, drawSize, drawSize, true)
        canvas.drawBitmap(scaled, offset, offset, null)
        return out
    }

    private fun fallback(context: Context, app: WebApp): Bitmap {
        val launcherBitmap = launcherIconBitmap(context)
        if (launcherBitmap != null) return launcherBitmap
        return generateLetterAvatar(app.name, app.themeColor)
    }

    fun buildFromSlug(context: Context, slug: String, bgColorArgb: Int): Bitmap {
        val svgBitmap = fetchSvgBitmap(context, slug, SVG_ICON_SIZE) ?: return generateLetterAvatar("?", null)
        return buildAdaptiveSvg("#%06X".format(0xFFFFFF and bgColorArgb), svgBitmap)
    }

    private fun buildSvgIcon(context: Context, src: IconSource.SvgIcon): Bitmap {
        val svgBitmap = fetchSvgBitmap(context, src.slug, SVG_ICON_SIZE)
        val bgColor = runCatching { Color.parseColor(src.background) }.getOrDefault(0xFF1976D2.toInt())
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(bgColor)
        if (svgBitmap != null) {
            val offset = (SIZE - SVG_ICON_SIZE) / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(svgBitmap, offset, offset, paint)
        }
        return out
    }

    private fun fetchSvgBitmap(context: Context, slug: String, size: Int): Bitmap? = runCatching {
        val svgUrl = "https://cdn.jsdelivr.net/npm/simple-icons/icons/$slug.svg"
        val loader = coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .build()
        val req = coil.request.ImageRequest.Builder(context)
            .data(svgUrl)
            .size(size, size)
            .build()
        val result = kotlinx.coroutines.runBlocking { loader.execute(req) }
        (result as? coil.request.SuccessResult)
            ?.let { (it.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap }
    }.getOrNull()

    private fun loadFile(path: String): Bitmap? = runCatching {
        BitmapFactory.decodeFile(path)
    }.getOrNull()

    private fun launcherIconBitmap(context: Context): Bitmap? = runCatching {
        context.packageManager
            .getApplicationIcon(context.packageName)
            .toBitmap(SIZE, SIZE)
    }.getOrNull()

    fun scaleCentered(src: Bitmap): Bitmap {
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
