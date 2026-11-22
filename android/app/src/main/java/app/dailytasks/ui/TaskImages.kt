package app.dailytasks.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Task pictures are stored inside the synced task as a small (128px)
 * "data:image/...;base64," URL, the same format the web app writes, so they
 * need no extra storage and work on both.
 */
object TaskImages {
    private const val SIZE = 128
    private val cache = HashMap<String, ImageBitmap?>()

    /** Decodes a stored picture, or null if it can't be read. */
    fun decode(dataUrl: String): ImageBitmap? = cache.getOrPut(dataUrl) {
        try {
            val comma = dataUrl.indexOf(',')
            val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    /** Reads a picked picture, crops it to a square and shrinks it. Null if it can't be read. */
    fun fromUri(context: Context, uri: Uri): String? = try {
        val src: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val s = info.size
                val scale = maxOf(1, minOf(s.width, s.height) / (SIZE * 2))
                decoder.setTargetSize(maxOf(1, s.width / scale), maxOf(1, s.height / scale))
            }
        } else {
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } ?: error("unreadable")
        }
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
        val out = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        small.compress(format, 85, out)
        "data:image/webp;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
