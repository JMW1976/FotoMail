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
            for (file in PhotoStore.queued(ctx)) {
                val pretty = file.nameWithoutExtension.removePrefix("Foto_").replace('_', ' ')
                try {
                    Mailer.send(
                        s, file,
                        subject = "${s.subjectPrefix} $pretty".trim(),
                        body = "Foto gemaakt op $pretty, verstuurd met Foto Mail."
                    )
                    file.delete()
                    s.lastStatus = "✅ ${file.name} verstuurd om ${time.format(Date())}"
                } catch (e: AuthenticationFailedException) {
                    s.lastStatus = "❌ Inloggen bij Gmail mislukt — controleer adres en app-wachtwoord."
                    return false
                } catch (e: Exception) {
                    s.lastStatus = "⚠️ Versturen mislukt (${e.javaClass.simpleName}: ${e.message}). " +
                        "Nieuwe poging volgt automatisch."
                    return false
                }
            }
            true
        }
    }
}
