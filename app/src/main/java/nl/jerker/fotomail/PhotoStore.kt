package nl.jerker.fotomail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bestanden:
 *  - capture/ : waar de camera-app de foto naartoe schrijft
 *  - queue/   : foto's die klaarstaan om gemaild te worden (worden na verzenden gewist)
 */
object PhotoStore {
    private const val MAX_SIDE = 2560

    fun captureDir(ctx: Context) = File(ctx.filesDir, "capture").apply { mkdirs() }
    fun queueDir(ctx: Context) = File(ctx.filesDir, "queue").apply { mkdirs() }

    fun newCaptureFile(ctx: Context): File =
        File(captureDir(ctx), "capture_${System.currentTimeMillis()}.jpg")

    fun queued(ctx: Context): List<File> =
        queueDir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".jpg") }
            ?.sortedBy { it.name } ?: emptyList()

    /** Verwijdert achtergebleven camerabestanden, behalve [keep]. */
    fun cleanCaptures(ctx: Context, keep: String?) {
        captureDir(ctx).listFiles()?.forEach { if (it.name != keep) it.delete() }
    }

    /** Zet een gemaakte foto in de wachtrij (eventueel verkleind en rechtgezet). */
    fun enqueue(ctx: Context, src: File, keepOriginal: Boolean): File {
        val dst = uniqueQueueFile(ctx)
        if (keepOriginal || !shrink(src, dst)) {
            if (!src.renameTo(dst)) {
                src.copyTo(dst, overwrite = true)
                src.delete()
            }
        } else {
            src.delete()
        }
        return dst
    }

    private fun uniqueQueueFile(ctx: Context): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        var f = File(queueDir(ctx), "Foto_$stamp.jpg")
        var i = 2
        while (f.exists()) f = File(queueDir(ctx), "Foto_${stamp}_${i++}.jpg")
        return f
    }

    /** Verkleint naar max. [MAX_SIDE] px en draait volgens EXIF. Geeft false bij mislukken. */
    private fun shrink(src: File, dst: File): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.path, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return false

            var sample = 1
            while (maxOf(w, h) / (sample * 2) >= MAX_SIDE) sample *= 2
            var bmp = BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return false

            val longest = maxOf(bmp.width, bmp.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(
                    bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
                )
                if (scaled != bmp) bmp.recycle()
                bmp = scaled
            }

            val degrees = when (ExifInterface(src.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                val m = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (rotated != bmp) bmp.recycle()
                bmp = rotated
            }

            FileOutputStream(dst).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bmp.recycle()
            dst.length() > 0
        } catch (e: Throwable) {
            dst.delete()
            false
        }
    }
}
