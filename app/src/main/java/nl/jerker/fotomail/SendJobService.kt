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
                s.setLast("config", "Gmail-gegevens ontbreken. Vul ze in bij Instellingen.")
                return true
            }
            val time = SimpleDateFormat("HH:mm", Locale.getDefault())
            fun pretty(f: java.io.File) = f.nameWithoutExtension.removePrefix("Foto_").replace('_', ' ')
            var sentPhotos = 0
            var sentMails = 0
            for ((subject, batches) in PhotoStore.mailGroups(PhotoStore.queued(ctx))) {
                val base = PhotoStore.safeName(subject)
                var counter = 0
                batches.forEachIndexed { i, batch ->
                    val first = pretty(batch.first())
                    val part = if (batches.size > 1) " – deel ${i + 1}/${batches.size}" else ""
                    val count = if (batch.size == 1) "" else " (${batch.size} foto's)"
                    val mailSubject = if (subject.isNotBlank()) "$subject$count$part"
                                      else "${s.subjectPrefix} $first$count$part".trim()
                    val names = batch.map { f ->
                        counter++
                        if (base.isNotEmpty()) "${base}_%02d.jpg".format(counter) else f.name
                    }
                    val mb = "%.1f".format(Locale.US, batch.sumOf { PhotoStore.mailSize(it) } / 1_000_000.0)
                    val body = buildString {
                        if (subject.isNotBlank()) append("Onderwerp: $subject\n\n")
                        append(if (batch.size == 1) "Foto gemaakt op $first.\n" else "${batch.size} foto's:\n")
                        if (batch.size > 1) batch.forEachIndexed { j, f -> append("• ${names[j]} — ${pretty(f)}\n") }
                        append("\nOngeveer $mb MB, verstuurd met Photo to Mail.")
                    }
                    try {
                        Mailer.send(s, batch, mailSubject, body, names)
                        batch.forEach { PhotoStore.remove(it) }
                        sentPhotos += batch.size
                        sentMails++
                        s.setLast("ok", "$sentPhotos foto${if (sentPhotos == 1) "" else "'s"}  ·  " +
                            "$sentMails mail${if (sentMails == 1) "" else "s"}  ·  ${time.format(Date())}")
                    } catch (e: AuthenticationFailedException) {
                        s.setLast("auth", "Inloggen bij Gmail mislukt. Controleer adres en app-wachtwoord bij Instellingen.")
                        return false
                    } catch (e: Exception) {
                        s.setLast("error", "Nieuwe poging volgt automatisch.\n${e.fullMessage()}")
                        return false
                    }
                }
            }
            true
        }
    }
}
