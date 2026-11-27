package nl.jerker.fotomail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Korte handleiding: Gmail instellen voor Photo to Mail. Volgt taal, thema en lettergrootte. */
class HelpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pal = pal
        val side = if (isLandscape()) 64 else 24

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(side), dp(24), dp(side), dp(32))
            setBackgroundColor(pal.bg)
        }

        col.addRow(TextView(this).apply {
            text = getString(R.string.help_title)
            textSize = 26f
            typeface = Brand.BOLD
            setTextColor(pal.text)
        }, top = 0)
        col.addRow(bodyText(getString(R.string.help_intro)), top = 8)

        step(col, 1, R.string.step1_t, R.string.step1_b)
        step(col, 2, R.string.step2_t, R.string.step2_b)
        col.addRow(brandButton(getString(R.string.open_security), ButtonStyle.BLACK, size = 14f) {
            open("https://myaccount.google.com/security")
        }, top = 10)
        step(col, 3, R.string.step3_t, R.string.step3_b)
        col.addRow(brandButton(getString(R.string.open_apppw), ButtonStyle.BLACK, size = 14f) {
            open("https://myaccount.google.com/apppasswords")
        }, top = 10)
        step(col, 4, R.string.step4_t, R.string.step4_b)
        step(col, 5, R.string.step5_t, R.string.step5_b)

        col.addRow(sectionLabel(getString(R.string.good_to_know)), top = 28)
        col.addRow(bodyText(getString(R.string.tips)), top = 8)

        col.addRow(brandButton(getString(R.string.to_settings), ButtonStyle.PRIMARY) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }, top = 28)

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            setBackgroundColor(pal.bg)
            addView(col)
        })
    }

    /** Genummerde stap: geel vierkant met nummer, titel en uitleg. Groeit mee met de lettergrootte. */
    private fun step(col: LinearLayout, nr: Int, titleId: Int, bodyId: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(TextView(this).apply {
            text = nr.toString()
            textSize = 16f
            typeface = Brand.BOLD
            gravity = Gravity.CENTER
            setTextColor(Brand.BLACK)
            setBackgroundColor(Brand.YELLOW)
            minWidth = dp(32)
            minHeight = dp(32)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        texts.addView(TextView(this).apply {
            text = getString(titleId)
            textSize = 17f
            typeface = Brand.BOLD
            setTextColor(pal.text)
        })
        texts.addView(bodyText(getString(bodyId)), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) })
        row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        col.addRow(row, top = 24)
    }

    private fun open(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
