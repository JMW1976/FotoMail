package nl.jerker.fotomail

import java.io.File
import java.util.Date
import java.util.Properties
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

    fun send(s: Settings, attachment: File?, subject: String, body: String) {
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
        if (attachment != null) {
            parts.addBodyPart(MimeBodyPart().apply {
                attachFile(attachment, "image/jpeg", "base64")
                fileName = attachment.name
            })
        }
        msg.setContent(parts)
        Transport.send(msg)
    }
}
