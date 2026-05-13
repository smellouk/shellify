package dev.pwaforge.core.shortcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import dev.pwaforge.domain.model.IconSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SvgIconRenderer {

    private const val ICON_SIZE = 140
    private const val CANVAS_SIZE = 192

    /**
     * Downloads the Simple Icons SVG for [slug], renders it white on a [bgColorArgb] background,
     * saves the 192×192 PNG to disk, and returns the [IconSource.SvgIcon] ready for storage.
     *
     * This is the canonical icon rendering used by both the Add screen and the Shortcuts screen.
     */
    suspend fun render(
        context: Context,
        slug: String,
        bgColorArgb: Int,
        isolationId: String,
        existingIconPath: String? = null,
    ): IconSource.SvgIcon? = withContext(Dispatchers.IO) {
        runCatching {
            val svgUrl = "https://cdn.jsdelivr.net/npm/simple-icons/icons/$slug.svg"
            val offset = (CANVAS_SIZE - ICON_SIZE) / 2

            val loader = ImageLoader.Builder(context)
                .components { add(SvgDecoder.Factory()) }
                .build()
            val req = ImageRequest.Builder(context)
                .data(svgUrl)
                .size(ICON_SIZE, ICON_SIZE)
                .build()
            val result = loader.execute(req)
            if (result !is SuccessResult) return@runCatching null
            val svgBitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: return@runCatching null

            val output = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(bgColorArgb)
            val paint = Paint().apply {
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(svgBitmap, offset.toFloat(), offset.toFloat(), paint)

            val dir = File(context.filesDir, "icons").also { it.mkdirs() }
            existingIconPath?.let { File(it).delete() }
            val file = File(dir, "${isolationId}_${System.currentTimeMillis()}.png")
            file.outputStream().use { out -> output.compress(Bitmap.CompressFormat.PNG, 100, out) }

            val bgHex = "#%06X".format(0xFFFFFF and bgColorArgb)
            IconSource.SvgIcon(slug = slug, background = bgHex, renderedPath = file.absolutePath)
        }.getOrNull()
    }
}
