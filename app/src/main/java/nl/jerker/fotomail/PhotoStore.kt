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
    fun collectDir(ctx: Context) = File(ctx.filesDir, "collect").apply { mkdirs() }

    /** Max. grootte van één mail (incl. base64-codering van de bijlagen). */
    const val MAX_MAIL_BYTES = 15_000_000L
    private const val MAIL_OVERHEAD = 50_000L

    fun newCaptureFile(ctx: Context): File =
        File(captureDir(ctx), "capture_${System.currentTimeMillis()}.jpg")

    fun queued(ctx: Context): List<File> = jpgs(queueDir(ctx))
    fun collected(ctx: Context): List<File> = jpgs(collectDir(ctx))

    private fun jpgs(dir: File): List<File> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") }?.sortedBy { it.name } ?: emptyList()

    /** Verzamelde foto's naar de verzendwachtrij verplaatsen. */
    fun releaseCollected(ctx: Context): Int {
        val files = collected(ctx)
        files.forEach { f ->
            val dst = File(queueDir(ctx), f.name)
            if (!f.renameTo(dst)) { f.copyTo(dst, overwrite = true); f.delete() }
        }
        return files.size
    }

    /** Geschatte mailgrootte van een bijlage (base64 = 4/3 x zo groot + kopregels). */
    fun mailSize(f: File) = f.length() * 4 / 3 + 2_000

    /** Verdeelt foto's over mails van max. [MAX_MAIL_BYTES]. Een te grote losse foto gaat alleen. */
    fun batches(files: List<File>): List<List<File>> {
        val out = mutableListOf<List<File>>()
        var cur = mutableListOf<File>()
        var size = MAIL_OVERHEAD
        for (f in files) {
            val s = mailSize(f)
            if (cur.isNotEmpty() && size + s > MAX_MAIL_BYTES) {
                out += cur; cur = mutableListOf(); size = MAIL_OVERHEAD
            }
            cur += f; size += s
        }
        if (cur.isNotEmpty()) out += cur
        return out
    }

    /** Verwijdert achtergebleven camerabestanden, behalve [keep]. */
    fun cleanCaptures(ctx: Context, keep: String?) {
        captureDir(ctx).listFiles()?.forEach { if (it.name != keep) it.delete() }
    }

    /** Zet een gemaakte foto in de wachtrij (eventueel verkleind en rechtgezet). */
    fun enqueue(ctx: Context, src: File, keepOriginal: Boolean, collect: Boolean = false): File {
        val dst = uniqueFile(ctx)
        if (collect) {
            val target = File(collectDir(ctx), dst.name)
            store(src, target, keepOriginal)
            return target
        }
        store(src, dst, keepOriginal)
        return dst
    }

    private fun store(src: File, dst: File, keepOriginal: Boolean) {
        if (keepOriginal || !shrink(src, dst)) {
            if (!src.renameTo(dst)) {
                src.copyTo(dst, overwrite = true)
                src.delete()
            }
        } else {
            src.delete()
        }
    }

    private fun uniqueFile(ctx: Context): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        fun taken(n: String) = File(queueDir(ctx), n).exists() || File(collectDir(ctx), n).exists()
        var name = "Foto_$stamp.jpg"
        var i = 2
        while (taken(name)) name = "Foto_${stamp}_${i++}.jpg"
        return File(queueDir(ctx), name)
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
