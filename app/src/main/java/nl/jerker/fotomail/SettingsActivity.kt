package nl.jerker.fotomail

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var s: Settings
    private lateinit var sender: EditText
    private lateinit var password: EditText
    private lateinit var recipient: EditText
    private lateinit var prefix: EditText
    private lateinit var keepOriginal: CheckBox
    private lateinit var autoCamera: CheckBox
    private lateinit var testBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        s = Settings(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
            setBackgroundColor(Brand.GREY)
        }
        fun label(t: String, top: Int = 16) = col.addRow(sectionLabel(t), top = top)
        fun field(value: String, type: Int) = col.addRow(EditText(this).apply {
            setText(value); inputType = type; isSingleLine = true; setTextColor(Color.WHITE)
        }, top = 2) as EditText

        label("Je Gmail-adres (afzender)", top = 0)
        sender = field(s.sender, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        label("Gmail app-wachtwoord (16 tekens)")
        password = field(s.appPassword, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        col.addRow(TextView(this).apply {
            textSize = 13f
            setTextColor(Brand.MUTED)
            text = "Gmail accepteert hier niet je gewone wachtwoord. Zet tweestapsverificatie aan, " +
                "maak een app-wachtwoord aan en plak de 16 tekens hierboven. Het wordt alleen op deze telefoon bewaard."
        }, top = 4)
        col.addRow(brandButton("App-wachtwoord aanmaken", ButtonStyle.BLACK, size = 14f) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords")))
        }, top = 8)

        label("Versturen naar")
        recipient = field(s.recipient, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        label("Onderwerp begint met")
        prefix = field(s.subjectPrefix, InputType.TYPE_CLASS_TEXT)

        keepOriginal = col.addRow(CheckBox(this).apply {
            text = "Originele grootte versturen (anders verkleind tot 2560 px, sneller)"
            isChecked = s.keepOriginal
            setTextColor(Color.WHITE)
        }, top = 16) as CheckBox
        autoCamera = col.addRow(CheckBox(this).apply {
            text = "Camera direct openen bij starten van de app"
            isChecked = s.autoCamera
            setTextColor(Color.WHITE)
        }, top = 4) as CheckBox

        col.addRow(brandButton("Opslaan", ButtonStyle.PRIMARY) { if (save()) finish() }, top = 28)
        testBtn = col.addRow(brandButton("Testmail versturen", ButtonStyle.BROWN) { sendTest() }, top = 12) as Button

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            setBackgroundColor(Brand.GREY)
            addView(col)
        })
    }

    private fun save(): Boolean {
        val from = sender.text.toString().trim()
        val to = recipient.text.toString().trim()
        if (!from.contains("@") || !to.contains("@")) {
            toast("Vul geldige e-mailadressen in")
            return false
        }
        if (password.text.isBlank()) {
            toast("Vul het app-wachtwoord in")
            return false
        }
        s.sender = from
        s.appPassword = password.text.toString()
        s.recipient = to
        s.subjectPrefix = prefix.text.toString()
        s.keepOriginal = keepOriginal.isChecked
        s.autoCamera = autoCamera.isChecked
        // Staan er nog foto's te wachten, probeer ze dan nu te versturen
        if (PhotoStore.queued(this).isNotEmpty()) SendJobService.schedule(applicationContext)
        toast("Opgeslagen")
        return true
    }

    private fun sendTest() {
        if (!save()) return
        testBtn.isEnabled = false
        testBtn.text = "Bezig met versturen…"
        Thread {
            val result = try {
                Mailer.send(s, null, "Foto to Mail – testbericht", "Het werkt! Foto's worden voortaan naar dit adres gestuurd.")
                "✅ Testmail verstuurd naar ${s.recipient}"
            } catch (e: javax.mail.AuthenticationFailedException) {
                "❌ Inloggen mislukt — controleer Gmail-adres en app-wachtwoord"
            } catch (e: Exception) {
                "❌ Mislukt: ${e.fullMessage()}"
            }
            runOnUiThread {
                testBtn.isEnabled = true
                testBtn.text = "Testmail versturen"
                toast(result)
            }
        }.start()
    }
}
