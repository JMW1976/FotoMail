package nl.jerker.fotomail

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var status: StatusPanel
    private lateinit var retryBtn: Button
    private lateinit var sendCollectedBtn: Button
    private var pendingName: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        pendingName = savedInstanceState?.getString(KEY_PENDING)
        PhotoStore.cleanCaptures(this, pendingName)

        actionBar?.hide()

        // Kopbalk: app-logo links, Allbrass-logo rechts, op zwart
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Brand.HEADER)
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Photo to Mail"
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)))
        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.allbrass_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_END
            contentDescription = "Allbrass"
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(56)).apply {
            leftMargin = dp(16)
        })


        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(32))
        }
        col.addRow(brandIconButton(R.drawable.ic_camera_line, 112, "Foto maken") { startSeries() },
            height = dp(170), top = 24)
        sendCollectedBtn = col.addRow(brandButton("Verstuur", ButtonStyle.BROWN, size = 18f) {
            val n = PhotoStore.releaseCollected(applicationContext)
            SendJobService.schedule(applicationContext)
            toast("$n foto${if (n == 1) "" else "'s"} worden verstuurd")
            refresh()
        }, height = dp(72), top = 16) as Button
        status = StatusPanel(this).apply {
            addRow("subject", "Onderwerp")
            addRow("collected", "Verzameld")
            addRow("queue", "Wachtrij")
            addRow("last", "Laatste verzending")
        }
        col.addRow(status.view, top = 24)
        retryBtn = col.addRow(brandButton("Wachtrij nu versturen", ButtonStyle.BROWN) {
            SendJobService.schedule(applicationContext)
            toast("Verzenden gestart")
        }, top = 16) as Button
        col.addRow(brandButton("Instellingen", ButtonStyle.BLACK) { openSettings() }, top = 16)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Brand.GREY)
            addView(header)
            addView(col)
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Brand.HEADER)
            isFillViewport = true
            addView(page)
        })

        val crash = CrashLog.takeLast(this)
        if (crash != null) {
            AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle("De app is vorige keer vastgelopen")
                .setMessage("Maak een screenshot van deze melding voor de ontwikkelaar:\n\n$crash")
                .setPositiveButton("OK", null)
                .show()
        } else if (savedInstanceState == null) {
            if (!settings.isConfigured) startActivity(Intent(this, HelpActivity::class.java))
            else if (settings.autoCamera) startSeries()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING, pendingName)
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    /** Vraagt het onderwerp van de fotoreeks en start daarna de camera. */
    private fun startSeries() {
        if (!settings.isConfigured) {
            toast("Stel eerst je Gmail-gegevens in")
            openSettings()
            return
        }
        val input = EditText(this).apply {
            setText(settings.currentSubject)
            hint = "Bijv. Pers 3 – matrijsschade"
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(Color.WHITE)
            selectAll()
        }
        val box = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(this, R.style.AppDialog)
            .setTitle("Onderwerp fotoreeks")
            .setMessage("Wordt het onderwerp van de mail. Leeg laten mag.")
            .setView(box)
            .setPositiveButton("Start") { _, _ ->
                settings.currentSubject = input.text.toString()
                launchCamera()
            }
            .setNegativeButton("Annuleren", null)
            .create()
        input.setOnEditorActionListener { _, _, _ ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); true
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        input.requestFocus()
    }

    private fun launchCamera() {
        if (!settings.isConfigured) {
            toast("Stel eerst je Gmail-gegevens in")
            openSettings()
            return
        }
        val file = PhotoStore.newCaptureFile(this)
        val uri = PhotoProvider.uriFor(this, file.name)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            clipData = ClipData.newRawUri("foto", uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            pendingName = file.name
            startActivityForResult(intent, REQ_CAMERA)
        } catch (e: ActivityNotFoundException) {
            pendingName = null
            toast("Geen camera-app gevonden")
        }
    }

    @Deprecated("Bewust de eenvoudige, ingebouwde API gebruikt (geen extra bibliotheken)")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CAMERA) return
        val name = pendingName ?: return
        pendingName = null
        val file = File(PhotoStore.captureDir(this), name)

        if (resultCode != RESULT_OK || file.length() == 0L) {
            file.delete()
            return
        }
        status.setHeadline("Foto verwerken…", StatusPanel.Dot.BUSY)
        val keep = settings.keepOriginal
        val direct = settings.directSend
        val subject = settings.currentSubject
        val app = applicationContext
        Thread {
            try {
                PhotoStore.enqueue(app, file, keep, collect = !direct, subject = subject)
                if (direct) {
                    SendJobService.schedule(app)
                    runOnUiThread {
                        toast("Foto wordt verstuurd — maak de volgende of ga terug om te stoppen")
                        refresh()
                    }
                } else {
                    val n = PhotoStore.collected(app).size
                    runOnUiThread {
                        toast("Foto $n verzameld — maak de volgende of ga terug om te versturen")
                        refresh()
                    }
                }
                runOnUiThread { launchCamera() } // reeks: direct door naar de volgende foto
            } catch (e: Exception) {
                runOnUiThread { toast("Verwerken mislukt: ${e.message}") }
            }
        }.start()
    }

    private fun refresh() {
        val n = PhotoStore.queued(this).size
        val c = PhotoStore.collected(this)
        val cMb = c.sumOf { PhotoStore.mailSize(it) } / 1_000_000.0
        val mails = PhotoStore.batches(c).size
        fun foto(k: Int) = "$k foto${if (k == 1) "" else "'s"}"
        val state = settings.lastState
        val failed = n > 0 && (state == "error" || state == "auth")
        when {
            !settings.isConfigured -> status.setHeadline("Instellingen onvolledig", StatusPanel.Dot.ERROR)
            failed -> status.setHeadline("Verzenden mislukt", StatusPanel.Dot.ERROR)
            n > 0 -> status.setHeadline("Bezig met versturen", StatusPanel.Dot.BUSY)
            c.isNotEmpty() -> status.setHeadline("${foto(c.size)} klaar om te versturen", StatusPanel.Dot.BUSY)
            state == "ok" -> status.setHeadline("Alles verstuurd", StatusPanel.Dot.OK)
            else -> status.setHeadline("Klaar voor gebruik", StatusPanel.Dot.IDLE)
        }
        status.setRow("subject", settings.currentSubject.ifBlank { null })
        status.setRow("collected", if (c.isEmpty()) null
            else "${foto(c.size)}  ·  ${"%.1f".format(cMb)} MB  ·  $mails mail${if (mails == 1) "" else "s"}")
        status.setRow("queue", if (n == 0) null else "${foto(n)} nog te versturen")
        val lastText = when (state) {
            "ok" -> settings.lastDetail
            "auth", "error", "config" -> if (failed || state == "config") settings.lastDetail else null
            else -> null
        }
        val lastColor = if (state == "ok") Color.WHITE else Color.rgb(240, 150, 135)
        status.setRow("last", lastText, lastColor)
        retryBtn.visibility = if (n > 0) View.VISIBLE else View.GONE
        sendCollectedBtn.visibility = if (c.isNotEmpty()) View.VISIBLE else View.GONE
        sendCollectedBtn.text = "Verstuur ${c.size} foto${if (c.size == 1) "" else "'s"}"
    }

    companion object {
        private const val REQ_CAMERA = 1
        private const val KEY_PENDING = "pending"
    }
}
