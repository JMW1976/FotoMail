package nl.jerker.fotomail

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import java.io.File
import java.util.Date

class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var status: StatusPanel
    private lateinit var retryBtn: Button
    private lateinit var sendCollectedBtn: Button
    private var pendingName: String? = null

    // Onderwerp-venster: wordt bij draaien bewaard en opnieuw getoond
    private var subjectDialog: AlertDialog? = null
    private var subjectInput: EditText? = null

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

        val pal = pal
        val landscape = isLandscape()

        // Bouwstenen
        val cameraBtn = brandIconButton(R.drawable.ic_camera_line, 112, getString(R.string.take_photo)) { startSeries() }
        sendCollectedBtn = brandButton("", ButtonStyle.BROWN, size = 18f) {
            val n = PhotoStore.releaseCollected(applicationContext)
            SendJobService.schedule(applicationContext)
            toast(quantity(R.plurals.sending_n, n))
            refresh()
        }
        status = StatusPanel(this).apply {
            addRow("subject", getString(R.string.row_subject))
            addRow("collected", getString(R.string.row_collected))
            addRow("queue", getString(R.string.row_queue))
            addRow("last", getString(R.string.row_last))
            addRow("problem", getString(R.string.row_problem))
        }
        retryBtn = brandButton(getString(R.string.retry_now), ButtonStyle.BROWN) {
            SendJobService.schedule(applicationContext)
            toast(getString(R.string.sending_started))
        }
        val settingsBtn = brandButton(getString(R.string.settings), ButtonStyle.BLACK) { openSettings() }

        // Kolom met knoppen en status
        val side = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        side.addRow(sendCollectedBtn, top = 0)
        side.addRow(status.view, top = 16)
        side.addRow(retryBtn, top = 16)
        side.addRow(settingsBtn, top = 16)

        val body: View = if (landscape) {
            // Liggend: camera links, status en knoppen rechts
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(24), dp(16), dp(24), dp(24))
                addView(cameraBtn, LinearLayout.LayoutParams(0, dp(220), 1f))
                addView(side, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(24)
                })
            }
        } else {
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(24), dp(24), dp(32))
                addRow(cameraBtn, height = dp(170), top = 0)
                addRow(side, top = 16)
            }
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pal.bg)
            addView(brandHeader(compact = landscape))
            addView(body)
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Brand.HEADER)
            isFillViewport = true
            addView(page)
        })

        val crash = CrashLog.takeLast(this)
        when {
            crash != null -> AlertDialog.Builder(this, R.style.AppDialog)
                .setTitle(R.string.crash_title)
                .setMessage(getString(R.string.crash_msg, crash))
                .setPositiveButton(R.string.ok, null)
                .show()
            savedInstanceState?.getBoolean(KEY_DIALOG) == true ->
                startSeries(savedInstanceState.getString(KEY_DIALOG_TEXT))
            savedInstanceState == null -> {
                if (!settings.isConfigured) startActivity(Intent(this, HelpActivity::class.java))
                else if (settings.autoCamera) startSeries()
            }
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

    override fun onDestroy() {
        subjectDialog?.dismiss()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING, pendingName)
        if (subjectDialog?.isShowing == true) {
            outState.putBoolean(KEY_DIALOG, true)
            outState.putString(KEY_DIALOG_TEXT, subjectInput?.text?.toString())
        }
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    /** Vraagt het onderwerp van de fotoreeks en start daarna de camera. */
    private fun startSeries(prefill: String? = null) {
        if (!settings.isConfigured) {
            toast(getString(R.string.setup_first))
            openSettings()
            return
        }
        val input = EditText(this).apply {
            setText(prefill ?: settings.currentSubject)
            hint = getString(R.string.subject_hint)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(pal.text)
            setHintTextColor(pal.hint)
            if (prefill == null) selectAll() else setSelection(text.length)
        }
        val box = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(this, R.style.AppDialog)
            .setTitle(R.string.subject_title)
            .setMessage(R.string.subject_msg)
            .setView(box)
            .setPositiveButton(R.string.start) { _, _ ->
                settings.currentSubject = input.text.toString()
                launchCamera()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        input.setOnEditorActionListener { _, _, _ ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); true
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        subjectDialog = dialog
        subjectInput = input
        dialog.show()
        input.requestFocus()
    }

    private fun launchCamera() {
        if (!settings.isConfigured) {
            toast(getString(R.string.setup_first))
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
            toast(getString(R.string.no_camera))
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
        status.setHeadline(getString(R.string.processing), StatusPanel.Dot.BUSY)
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
                        toast(getString(R.string.photo_sending_next))
                        refresh()
                    }
                } else {
                    val n = PhotoStore.collected(app).size
                    runOnUiThread {
                        toast(getString(R.string.photo_collected_next, n))
                        refresh()
                    }
                }
                runOnUiThread { launchCamera() } // reeks: direct door naar de volgende foto
            } catch (e: Exception) {
                runOnUiThread { toast(getString(R.string.processing_failed, e.message ?: "")) }
            }
        }.start()
    }

    private fun refresh() {
        val n = PhotoStore.queued(this).size
        val c = PhotoStore.collected(this)
        val cMb = c.sumOf { PhotoStore.mailSize(it) } / 1_000_000.0
        val mails = PhotoStore.batches(c).size
        val state = settings.lastState
        val failed = n > 0 && (state == "error" || state == "auth")
        when {
            !settings.isConfigured -> status.setHeadline(getString(R.string.st_incomplete), StatusPanel.Dot.ERROR)
            failed -> status.setHeadline(getString(R.string.st_failed), StatusPanel.Dot.ERROR)
            n > 0 -> status.setHeadline(getString(R.string.st_busy), StatusPanel.Dot.BUSY)
            c.isNotEmpty() -> status.setHeadline(quantity(R.plurals.ready_n, c.size), StatusPanel.Dot.BUSY)
            state == "ok" -> status.setHeadline(getString(R.string.st_all_sent), StatusPanel.Dot.OK)
            else -> status.setHeadline(getString(R.string.st_idle), StatusPanel.Dot.IDLE)
        }
        status.setRow("subject", settings.currentSubject.ifBlank { null })
        status.setRow("collected", if (c.isEmpty()) null else listOf(
            quantity(R.plurals.photos_n, c.size), "%.1f MB".format(cMb), quantity(R.plurals.mails_n, mails)
        ).joinToString("  ·  "))
        status.setRow("queue", if (n == 0) null else quantity(R.plurals.queue_n, n))

        // Laatste geslaagde verzending, met tijd in het 12/24-uursformaat van de telefoon
        status.setRow("last", if (settings.lastPhotos == 0) null else listOf(
            quantity(R.plurals.photos_n, settings.lastPhotos),
            quantity(R.plurals.mails_n, settings.lastMails),
            DateFormat.getTimeFormat(this).format(Date(settings.lastTime))
        ).joinToString("  ·  "))

        val problem = when {
            !settings.isConfigured -> getString(R.string.err_config)
            failed && state == "auth" -> getString(R.string.err_auth)
            failed -> getString(R.string.err_retry) + "\n" + settings.lastError
            else -> null
        }
        status.setRow("problem", problem, pal.error)

        retryBtn.visibility = if (n > 0) View.VISIBLE else View.GONE
        sendCollectedBtn.visibility = if (c.isNotEmpty()) View.VISIBLE else View.GONE
        sendCollectedBtn.text = quantity(R.plurals.send_n, c.size)
    }

    companion object {
        private const val REQ_CAMERA = 1
        private const val KEY_PENDING = "pending"
        private const val KEY_DIALOG = "subjectDialog"
        private const val KEY_DIALOG_TEXT = "subjectDialogText"
    }
}
