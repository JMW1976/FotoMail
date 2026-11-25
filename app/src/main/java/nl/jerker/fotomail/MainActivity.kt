package nl.jerker.fotomail

import android.app.Activity
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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var subtitle: TextView
    private lateinit var status: TextView
    private lateinit var retryBtn: Button
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

        // Kopbalk: zwart, gele accentbalk, witte titel, gele ondertitel
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Brand.HEADER)
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }
        header.addView(View(this).apply { setBackgroundColor(Brand.YELLOW) },
            LinearLayout.LayoutParams(dp(10), dp(56)))
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }
        titles.addView(TextView(this).apply {
            text = "Foto Mail"
            textSize = 30f
            typeface = Brand.BOLD
            setTextColor(Color.WHITE)
        })
        subtitle = TextView(this).apply {
            textSize = 12f
            typeface = Brand.BOLD
            letterSpacing = 0.04f
            setTextColor(Brand.YELLOW)
        }
        titles.addView(subtitle)
        header.addView(titles)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(32))
        }
        col.addRow(brandButton("📷  Foto maken", ButtonStyle.PRIMARY, size = 26f) { launchCamera() },
            height = dp(170), top = 24)
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.2f)
        }
        col.addRow(accentCard(status), top = 24)
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

        if (savedInstanceState == null) {
            if (!settings.isConfigured) openSettings()
            else if (settings.autoCamera) launchCamera()
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
        status.text = "Foto verwerken…"
        val keep = settings.keepOriginal
        val app = applicationContext
        Thread {
            try {
                PhotoStore.enqueue(app, file, keep)
                SendJobService.schedule(app)
                runOnUiThread { toast("Foto wordt verstuurd naar ${settings.recipient}"); refresh() }
            } catch (e: Exception) {
                runOnUiThread { toast("Verwerken mislukt: ${e.message}") }
            }
        }.start()
    }

    private fun refresh() {
        val n = PhotoStore.queued(this).size
        subtitle.text = "FOTO'S NAAR ${settings.recipient.uppercase()}"
        status.text = buildString {
            if (!settings.isConfigured) append("⚠️ Vul eerst je Gmail-gegevens in bij Instellingen.\n\n")
            append(if (n == 0) "Wachtrij leeg — alles is verstuurd." else "In wachtrij: $n foto${if (n == 1) "" else "'s"}")
            val last = settings.lastStatus
            if (last.isNotBlank()) append("\n\nLaatste melding:\n$last")
        }
        retryBtn.visibility = if (n > 0) View.VISIBLE else View.GONE
    }

    companion object {
        private const val REQ_CAMERA = 1
        private const val KEY_PENDING = "pending"
    }
}
