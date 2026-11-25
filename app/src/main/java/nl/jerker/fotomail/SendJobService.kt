package nl.jerker.fotomail

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.mail.AuthenticationFailedException

/**
 * Verstuurt alle foto's in de wachtrij zodra er internet is.
 * Lukt het niet, dan probeert Android het later automatisch opnieuw.
 */
class SendJobService : JobService() {

    @Volatile private var stopped = false

    override fun onStartJob(params: JobParameters): Boolean {
        stopped = false
        Thread {
            val allSent = sendAll(applicationContext)
            if (!stopped) jobFinished(params, !allSent)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped = true
        return true // later opnieuw proberen
    }

    companion object {
        private const val JOB_ID = 4711
        private val lock = Any()

        fun schedule(ctx: Context) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(ctx, SendJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setPersisted(true)
                .build()
            ctx.getSystemService(JobScheduler::class.java).schedule(job)
        }

        /** @return true als de wachtrij leeg is (of verzenden zinloos is zonder instellingen). */
        fun sendAll(ctx: Context): Boolean = synchronized(lock) {
            val s = Settings(ctx)
            if (!s.isConfigured) {
                s.lastStatus = "Niet verstuurd: Gmail-gegevens ontbreken (Instellingen)."
                return true
            }
            val time = SimpleDateFormat("HH:mm", Locale.getDefault())
            val batches = PhotoStore.batches(PhotoStore.queued(ctx))
            fun pretty(f: java.io.File) = f.nameWithoutExtension.removePrefix("Foto_").replace('_', ' ')
            var sentPhotos = 0
            batches.forEachIndexed { i, batch ->
                val first = pretty(batch.first())
                val part = if (batches.size > 1) " – deel ${i + 1}/${batches.size}" else ""
                val subject = if (batch.size == 1) "${s.subjectPrefix} $first$part"
                              else "${s.subjectPrefix} $first (${batch.size} foto's)$part"
                val mb = "%.1f".format(Locale.US, batch.sumOf { PhotoStore.mailSize(it) } / 1_000_000.0)
                val body = buildString {
                    append(if (batch.size == 1) "Foto gemaakt op $first." else "${batch.size} foto's:\n")
                    if (batch.size > 1) batch.forEach { append("• ${pretty(it)}\n") }
                    append("\nOngeveer $mb MB, verstuurd met Foto to Mail.")
                }
                try {
                    Mailer.send(s, batch, subject.trim(), body)
                    batch.forEach { it.delete() }
                    sentPhotos += batch.size
                    s.lastStatus = "✅ $sentPhotos foto${if (sentPhotos == 1) "" else "'s"} verstuurd " +
                        "in ${i + 1} mail${if (i == 0) "" else "s"} om ${time.format(Date())}"
                } catch (e: AuthenticationFailedException) {
                    s.lastStatus = "❌ Inloggen bij Gmail mislukt — controleer adres en app-wachtwoord."
                    return false
                } catch (e: Exception) {
                    s.lastStatus = "⚠️ Versturen mislukt (${e.fullMessage()}). " +
                        "Nieuwe poging volgt automatisch."
                    return false
                }
            }
            true
        }
    }
}
