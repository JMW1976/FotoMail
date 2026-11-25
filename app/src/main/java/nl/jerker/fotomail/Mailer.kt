package nl.jerker.fotomail

import java.io.File
import java.util.Date
import java.util.Properties
import javax.activation.CommandMap
import javax.activation.MailcapCommandMap
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/** Verstuurt mail via smtp.gmail.com (SSL, poort 465). Niet op de UI-thread aanroepen. */
object Mailer {

    /** Registreert de MIME-handlers expliciet; op Android vindt JavaMail ze anders niet. */
    private val handlersRegistered by lazy {
        val mc = CommandMap.getDefaultCommandMap() as MailcapCommandMap
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true")
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
        CommandMap.setDefaultCommandMap(mc)
        true
    }

    fun send(s: Settings, attachments: List<File>, subject: String, body: String, names: List<String>? = null) {
        // Achtergrondthreads hebben op Android geen bruikbare class loader voor JavaMail
        Thread.currentThread().contextClassLoader = Mailer::class.java.classLoader
        check(handlersRegistered)
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "465")
            put("mail.smtp.auth", "true")
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.ssl.checkserveridentity", "true")
            put("mail.smtp.connectiontimeout", "20000")
            put("mail.smtp.timeout", "60000")
            put("mail.smtp.writetimeout", "120000")
        }
        val user = s.sender
        val pass = s.appPassword
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
        })

        val msg = MimeMessage(session).apply {
            setFrom(InternetAddress(user))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(s.recipient))
            setSubject(subject, "UTF-8")
            sentDate = Date()
        }

        val parts = MimeMultipart()
        parts.addBodyPart(MimeBodyPart().apply { setText(body, "UTF-8") })
        attachments.forEachIndexed { i, f ->
            parts.addBodyPart(MimeBodyPart().apply {
                attachFile(f, "image/jpeg", "base64")
                fileName = names?.getOrNull(i) ?: f.name
            })
        }
        msg.setContent(parts)
        msg.saveChanges()
        Transport.send(msg)
    }
}

/** Volledige foutketen, zodat de melding in de app de echte oorzaak toont. */
fun Throwable.fullMessage(): String {
    val parts = mutableListOf<String>()
    var t: Throwable? = this
    while (t != null && parts.size < 4) {
        parts += "${t.javaClass.simpleName}: ${t.message}"
        t = (t as? javax.mail.MessagingException)?.nextException ?: t.cause
    }
    return parts.joinToString(" ← ")
}
