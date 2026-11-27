package nl.jerker.fotomail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Statusblok: kop met gekleurde stip, daaronder rijen met label en waarde. Volgt licht/donker. */
class StatusPanel(private val ctx: Context) {

    object Dot {
        val OK = Color.rgb(76, 175, 80)
        val BUSY = Color.rgb(230, 190, 0)
        val ERROR = Color.rgb(229, 83, 61)
        val IDLE = Color.rgb(140, 140, 140)
    }

    private val pal = ctx.pal
    private val dot = View(ctx)
    private val headline = TextView(ctx).apply {
        textSize = 18f
        typeface = Brand.BOLD
        setTextColor(pal.text)
    }
    private val rows = linkedMapOf<String, Pair<View, TextView>>()

    val view: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(pal.card)
            setStroke(ctx.dp(1), pal.border)
        }
        setPadding(ctx.dp(20), ctx.dp(18), ctx.dp(20), ctx.dp(20))
        addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(dot, LinearLayout.LayoutParams(ctx.dp(10), ctx.dp(10)).apply { rightMargin = ctx.dp(12) })
            addView(headline, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
    }

    /** Rijen in vaste volgorde aanmaken. */
    fun addRow(key: String, label: String) {
        val value = TextView(ctx).apply {
            textSize = 15f
            setTextColor(pal.text)
            setLineSpacing(0f, 1.15f)
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(View(ctx).apply { setBackgroundColor(pal.border) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(1)).apply {
                    topMargin = ctx.dp(16); bottomMargin = ctx.dp(12)
                })
            addView(TextView(ctx).apply {
                text = label.uppercase()
                textSize = 11f
                typeface = Brand.BOLD
                letterSpacing = 0.08f
                setTextColor(pal.muted)
            })
            addView(value, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(4) })
        }
        view.addView(row)
        rows[key] = row to value
    }

    fun setHeadline(text: String, color: Int) {
        headline.text = text
        dot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    /** Waarde tonen; null of leeg verbergt de rij. */
    fun setRow(key: String, value: String?, color: Int = pal.text) {
        val (row, tv) = rows[key] ?: return
        row.visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
        tv.text = value ?: ""
        tv.setTextColor(color)
    }
}
