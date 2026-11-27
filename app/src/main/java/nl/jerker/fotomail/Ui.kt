package nl.jerker.fotomail

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Vaste huisstijlkleuren (Allbrass). */
object Brand {
    val YELLOW = Color.rgb(252, 221, 23)   // #FCDD17
    val BLACK = Color.rgb(17, 17, 17)      // #111111
    val HEADER = Color.BLACK
    val BROWN = Color.rgb(65, 50, 11)      // #41320B
    val GREY = Color.rgb(35, 35, 35)       // #232323
    val BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
}

/** Kleuren die meebewegen met de lichte of donkere modus van Android. */
class Palette(ctx: Context) {
    val dark = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val bg = if (dark) Brand.GREY else Color.rgb(242, 242, 242)
    val card = if (dark) Brand.BLACK else Color.WHITE
    val border = if (dark) Color.rgb(46, 46, 46) else Color.rgb(221, 221, 221)
    val text = if (dark) Color.WHITE else Brand.BLACK
    val textSoft = if (dark) Color.rgb(220, 220, 220) else Color.rgb(51, 51, 51)
    val muted = if (dark) Color.rgb(170, 170, 170) else Color.rgb(107, 107, 107)
    val label = if (dark) Brand.YELLOW else Brand.BROWN
    val error = if (dark) Color.rgb(240, 150, 135) else Color.rgb(192, 57, 43)
    val hint = if (dark) Color.rgb(138, 138, 138) else Color.rgb(150, 150, 150)
}

val Context.pal: Palette get() = Palette(this)

fun Context.isLandscape() =
    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

enum class ButtonStyle { PRIMARY, BROWN, BLACK }

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

fun Context.quantity(id: Int, n: Int): String = resources.getQuantityString(id, n, n)

fun LinearLayout.addRow(view: View, height: Int = LinearLayout.LayoutParams.WRAP_CONTENT, top: Int = 12): View {
    addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply {
        topMargin = context.dp(top)
    })
    return view
}

/** Hoekige knop in huisstijl: geel (hoofdactie), bruin met gele rand, of zwart met grijze rand.
 *  Hoogte groeit mee met de lettergrootte van het systeem. */
fun Context.brandButton(label: String, style: ButtonStyle, size: Float = 16f, onClick: () -> Unit): Button {
    val (fill, stroke, textColor) = when (style) {
        ButtonStyle.PRIMARY -> Triple(Brand.YELLOW, Brand.YELLOW, Brand.BLACK)
        ButtonStyle.BROWN -> Triple(Brand.BROWN, Brand.YELLOW, Color.WHITE)
        ButtonStyle.BLACK -> Triple(Color.BLACK, Color.rgb(90, 90, 90), Color.WHITE)
    }
    return Button(this).apply {
        text = label
        isAllCaps = true
        textSize = size
        typeface = Brand.BOLD
        letterSpacing = 0.04f
        setTextColor(textColor)
        background = brandBackground(fill, stroke, style == ButtonStyle.PRIMARY)
        stateListAnimator = null
        minHeight = dp(56)
        minimumHeight = dp(56)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setOnClickListener { onClick() }
    }
}

/** Hoekige achtergrond met rand en aanraakeffect. */
fun Context.brandBackground(fill: Int, stroke: Int, darkRipple: Boolean): RippleDrawable {
    val shape = GradientDrawable().apply {
        setColor(fill)
        setStroke(dp(1), stroke)
        cornerRadius = 0f
    }
    val ripple = if (darkRipple) Color.argb(60, 0, 0, 0) else Color.argb(60, 252, 221, 23)
    return RippleDrawable(ColorStateList.valueOf(ripple), shape, ColorDrawable(Color.WHITE))
}

/** Gele knop met alleen een (groot) icoon in het midden. */
fun Context.brandIconButton(iconRes: Int, iconDp: Int, description: String, onClick: () -> Unit): View =
    FrameLayout(this).apply {
        background = brandBackground(Brand.YELLOW, Brand.YELLOW, true)
        isClickable = true
        isFocusable = true
        contentDescription = description
        setOnClickListener { onClick() }
        addView(ImageView(context).apply {
            setImageResource(iconRes)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER))
    }

/** Kleine kop in hoofdletters (geel in donker, bruin in licht), zoals op de website. */
fun Context.sectionLabel(t: String): TextView = TextView(this).apply {
    text = t.uppercase()
    textSize = 13f
    typeface = Brand.BOLD
    letterSpacing = 0.05f
    setTextColor(pal.label)
}

/** Gewone tekst in de kleur van het thema. */
fun Context.bodyText(t: String, size: Float = 15f, soft: Boolean = true): TextView = TextView(this).apply {
    text = t
    textSize = size
    setTextColor(if (soft) pal.textSoft else pal.text)
    setLineSpacing(0f, 1.25f)
}

/** Zwarte kopbalk met app-logo links en Allbrass-logo rechts. */
fun Context.brandHeader(compact: Boolean): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setBackgroundColor(Brand.HEADER)
    val v = if (compact) 10 else 20
    setPadding(dp(24), dp(v), dp(24), dp(v))
    addView(ImageView(context).apply {
        setImageResource(R.drawable.app_logo)
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = getString(R.string.app_name)
    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(if (compact) 36 else 48)))
    addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
    addView(ImageView(context).apply {
        setImageResource(R.drawable.allbrass_logo)
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_END
        contentDescription = "Allbrass"
    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(if (compact) 40 else 56)).apply {
        leftMargin = dp(16)
    })
}
