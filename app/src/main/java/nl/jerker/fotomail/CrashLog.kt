package nl.jerker.fotomail

import android.app.Application
import android.content.Context
import java.io.File

/** Legt een crash vast in een bestand, zodat de app hem bij de volgende start kan tonen. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}

object CrashLog {
    private fun file(ctx: Context) = File(ctx.filesDir, "crash.txt")

    fun install(ctx: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val version = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                val trace = e.stackTraceToString().lines().take(25).joinToString("\n")
                file(ctx).writeText("Versie $version\n$trace")
            } catch (_: Throwable) { }
            previous?.uncaughtException(t, e)
        }
    }

    /** Geeft de laatst vastgelegde crash terug (en wist hem), of null. */
    fun takeLast(ctx: Context): String? {
        val f = file(ctx)
        if (!f.exists()) return null
        val text = f.readText()
        f.delete()
        return text.ifBlank { null }
    }
}
