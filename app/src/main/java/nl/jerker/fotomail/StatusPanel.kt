package nl.jerker.fotomail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Statusblok: kop met gekleurde stip, daaronder rijen met label en waarde. */
class StatusPanel(private val ctx: Context) {

    object Dot {
        val OK = Color.rgb(111, 191, 115)
        val BUSY = Brand.YELLOW
        val ERROR = Color.rgb(229, 83, 61)
        val IDLE = Color.rgb(120, 120, 120)
    }

    private val dot = View(ctx)
    private val headline = TextView(ctx).apply {
        textSize = 18f
        typeface = Brand.BOLD
        setTextColor(Color.WHITE)
    }
    private val rows = linkedMapOf<String, Pair<View, TextView>>()

    val view: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(Brand.BLACK)
            setStroke(ctx.dp(1), Color.rgb(46, 46, 46))
        }
        setPadding(ctx.dp(20), ctx.dp(18), ctx.dp(20), ctx.dp(20))
        addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(dot, LinearLayout.LayoutParams(ctx.dp(10), ctx.dp(10)).apply { rightMargin = ctx.dp(12) })
            addView(headline)
        })
    }

    /** Rijen in vaste volgorde aanmaken. */
    fun addRow(key: String, label: String) {
        val value = TextView(ctx).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.15f)
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(View(ctx).apply { setBackgroundColor(Color.rgb(46, 46, 46)) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(1)).apply {
                    topMargin = ctx.dp(16); bottomMargin = ctx.dp(12)
                })
            addView(TextView(ctx).apply {
                text = label.uppercase()
                textSize = 11f
                typeface = Brand.BOLD
                letterSpacing = 0.08f
                setTextColor(Brand.MUTED)
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
    fun setRow(key: String, value: String?, color: Int = Color.WHITE) {
        val (row, tv) = rows[key] ?: return
        row.visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
        tv.text = value ?: ""
        tv.setTextColor(color)
    }
}
