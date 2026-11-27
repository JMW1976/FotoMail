package nl.jerker.fotomail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView

class SettingsActivity : Activity() {

    private lateinit var s: Settings
    private lateinit var sender: EditText
    private lateinit var password: EditText
    private lateinit var recipient: EditText
    private lateinit var prefix: EditText
    private lateinit var keepOriginal: CheckBox
    private lateinit var autoCamera: CheckBox
    private lateinit var directSend: CheckBox
    private lateinit var testBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        s = Settings(this)
        val pal = pal

        // Liggend: iets meer zijmarge zodat de velden niet over de hele breedte lopen
        val side = if (isLandscape()) 64 else 24
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(side), dp(24), dp(side), dp(32))
            setBackgroundColor(pal.bg)
        }
        fun label(id: Int, top: Int = 16) = col.addRow(sectionLabel(getString(id)), top = top)
        // Vaste id's: Android bewaart zo de ingevulde tekst bij draaien van het scherm
        fun field(viewId: Int, value: String, type: Int, hintId: Int?) = col.addRow(EditText(this).apply {
            id = viewId
            setText(value)
            inputType = type
            isSingleLine = true
            setTextColor(pal.text)
            setHintTextColor(pal.hint)
            if (hintId != null) hint = getString(hintId)
        }, top = 2) as EditText
        fun check(viewId: Int, textId: Int, value: Boolean, top: Int = 4) = col.addRow(CheckBox(this).apply {
            id = viewId
            text = getString(textId)
            isChecked = value
            setTextColor(pal.text)
        }, top = top) as CheckBox

        col.addRow(brandButton(getString(R.string.help_button), ButtonStyle.BROWN, size = 14f) {
            startActivity(Intent(this, HelpActivity::class.java))
        }, top = 0)

        val email = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        label(R.string.lbl_sender, top = 24)
        sender = field(R.id.field_sender, s.sender, email, R.string.hint_sender)
        label(R.string.lbl_password)
        password = field(R.id.field_password, s.appPassword,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, null)
        password.hint = "abcd efgh ijkl mnop"
        col.addRow(bodyText(getString(R.string.password_note), size = 13f).apply { setTextColor(pal.muted) }, top = 4)
        col.addRow(brandButton(getString(R.string.create_app_password), ButtonStyle.BLACK, size = 14f) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords")))
        }, top = 8)

        label(R.string.lbl_recipient)
        recipient = field(R.id.field_recipient, s.recipient, email, R.string.hint_recipient)
        label(R.string.lbl_prefix)
        prefix = field(R.id.field_prefix, s.subjectPrefix, InputType.TYPE_CLASS_TEXT, null)

        keepOriginal = check(R.id.check_original, R.string.cb_original, s.keepOriginal, top = 16)
        autoCamera = check(R.id.check_autocamera, R.string.cb_autocamera, s.autoCamera)
        directSend = check(R.id.check_direct, R.string.cb_direct, s.directSend)

        col.addRow(brandButton(getString(R.string.save), ButtonStyle.PRIMARY) { if (save()) finish() }, top = 28)
        testBtn = col.addRow(brandButton(getString(R.string.send_test), ButtonStyle.BROWN) { sendTest() }, top = 12) as Button

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            setBackgroundColor(pal.bg)
            addView(col)
        })
    }

    private fun save(): Boolean {
        val from = sender.text.toString().trim()
        val to = recipient.text.toString().trim()
        if (!from.contains("@") || !to.contains("@")) {
            toast(getString(R.string.invalid_emails))
            return false
        }
        if (password.text.isBlank()) {
            toast(getString(R.string.enter_password))
            return false
        }
        s.sender = from
        s.appPassword = password.text.toString()
        s.recipient = to
        s.subjectPrefix = prefix.text.toString()
        s.keepOriginal = keepOriginal.isChecked
        s.autoCamera = autoCamera.isChecked
        s.directSend = directSend.isChecked
        // Staan er nog foto's te wachten, probeer ze dan nu te versturen
        if (PhotoStore.queued(this).isNotEmpty()) SendJobService.schedule(applicationContext)
        toast(getString(R.string.saved))
        return true
    }

    private fun sendTest() {
        if (!save()) return
        testBtn.isEnabled = false
        testBtn.text = getString(R.string.sending_busy)
        val app = applicationContext
        Thread {
            val result = try {
                Mailer.send(s, emptyList(), app.getString(R.string.test_subject), app.getString(R.string.test_body))
                app.getString(R.string.test_ok, s.recipient)
            } catch (e: javax.mail.AuthenticationFailedException) {
                app.getString(R.string.test_auth)
            } catch (e: Exception) {
                app.getString(R.string.test_fail, e.fullMessage())
            }
            runOnUiThread {
                if (!isDestroyed) {
                    testBtn.isEnabled = true
                    testBtn.text = getString(R.string.send_test)
                }
                app.toast(result)
            }
        }.start()
    }
}
