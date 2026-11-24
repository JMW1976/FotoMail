package nl.jerker.fotomail

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

/** Kleine hulpfuncties voor de (in code opgebouwde) schermen. */
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

fun LinearLayout.addRow(view: View, height: Int = LinearLayout.LayoutParams.WRAP_CONTENT, top: Int = 12): View {
    addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply {
        topMargin = context.dp(top)
    })
    return view
}
