package nl.jerker.fotomail

import android.content.Context
import android.content.res.ColorStateList
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

/** Huisstijlkleuren (Allbrass). */
object Brand {
    val YELLOW = Color.rgb(252, 221, 23)   // #FCDD17
    val BLACK = Color.rgb(17, 17, 17)      // #111111
    val HEADER = Color.BLACK
    val BROWN = Color.rgb(65, 50, 11)      // #41320B
    val GREY = Color.rgb(35, 35, 35)       // #232323
    val MUTED = Color.rgb(170, 170, 170)
    val BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
}

enum class ButtonStyle { PRIMARY, BROWN, BLACK }

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

fun LinearLayout.addRow(view: View, height: Int = LinearLayout.LayoutParams.WRAP_CONTENT, top: Int = 12): View {
    addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply {
        topMargin = context.dp(top)
    })
    return view
}

/** Hoekige knop in huisstijl: geel (hoofdactie), bruin met gele rand, of zwart met grijze rand. */
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

/** Kleine gele kop in hoofdletters, zoals op de website. */
fun Context.sectionLabel(t: String): TextView = TextView(this).apply {
    text = t.uppercase()
    textSize = 13f
    typeface = Brand.BOLD
    letterSpacing = 0.05f
    setTextColor(Brand.YELLOW)
}

/** Zwart kaartje met bruine balk links, voor statusberichten. */
fun Context.accentCard(content: TextView): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    setBackgroundColor(Brand.BLACK)
    addView(View(context).apply { setBackgroundColor(Brand.BROWN) },
        LinearLayout.LayoutParams(dp(6), LinearLayout.LayoutParams.MATCH_PARENT))
    content.setPadding(dp(16), dp(16), dp(16), dp(16))
    content.gravity = Gravity.CENTER_VERTICAL
    addView(content, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
}
