package nl.jerker.fotomail

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Korte handleiding: Gmail instellen voor Photo to Mail. */
class HelpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(32))
            setBackgroundColor(Brand.GREY)
        }

        col.addRow(TextView(this).apply {
            text = "Gmail instellen"
            textSize = 26f
            typeface = Brand.BOLD
            setTextColor(Color.WHITE)
        }, top = 0)
        col.addRow(text(
            "Photo to Mail verstuurt je foto's via een Gmail-account. Dat stel je één keer in; " +
                "reken op ongeveer 5 minuten. Gebruik hiervoor een computer of deze telefoon."
        ), top = 8)

        step(col, 1, "Gmail-account",
            "Je hebt een Gmail-adres nodig om vanaf te versturen. Heb je er nog geen, maak dan gratis een account aan op gmail.com.")
        step(col, 2, "Verificatie in 2 stappen aanzetten",
            "Ga naar myaccount.google.com → Beveiliging → Verificatie in 2 stappen en zet dit aan. " +
                "Zonder deze stap kun je geen app-wachtwoord maken.")
        col.addRow(brandButton("Open Google-beveiliging", ButtonStyle.BLACK, size = 14f) {
            open("https://myaccount.google.com/security")
        }, top = 10)
        step(col, 3, "App-wachtwoord maken",
            "Ga naar myaccount.google.com/apppasswords. Vul als naam \"Photo to Mail\" in en tik op Maken. " +
                "Je krijgt een code van 16 letters (bijv. abcd efgh ijkl mnop). Kopieer deze meteen: " +
                "Google laat hem maar één keer zien.")
        col.addRow(brandButton("Open app-wachtwoorden", ButtonStyle.BLACK, size = 14f) {
            open("https://myaccount.google.com/apppasswords")
        }, top = 10)
        step(col, 4, "Gegevens invullen in de app",
            "Open Instellingen en vul in:\n" +
                "•  Je Gmail-adres (afzender)\n" +
                "•  Het app-wachtwoord van 16 letters (spaties mogen)\n" +
                "•  Versturen naar: het adres waar de foto's heen moeten, bijv. je werkmail\n" +
                "Tik daarna op Opslaan.")
        step(col, 5, "Testen",
            "Tik in Instellingen op Testmail versturen. Kijk of de mail aankomt; " +
                "controleer ook de map Spam of Ongewenste e-mail.")

        col.addRow(sectionLabel("Goed om te weten"), top = 28)
        col.addRow(text(
            "•  Gebruik nooit je gewone Gmail-wachtwoord; dat werkt hier niet.\n" +
                "•  Het app-wachtwoord blijft alleen op deze telefoon.\n" +
                "•  Telefoon kwijt? Verwijder het app-wachtwoord via myaccount.google.com/apppasswords.\n" +
                "•  Melding \"Inloggen mislukt\"? Controleer het Gmail-adres en maak zo nodig een nieuw app-wachtwoord.\n" +
                "•  Foto's worden standaard verkleind; mails zijn maximaal 15 MB."
        ), top = 8)

        col.addRow(brandButton("Naar instellingen", ButtonStyle.PRIMARY) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }, top = 28)

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            setBackgroundColor(Brand.GREY)
            addView(col)
        })
    }

    private fun text(t: String) = TextView(this).apply {
        text = t
        textSize = 15f
        setTextColor(Color.rgb(220, 220, 220))
        setLineSpacing(0f, 1.25f)
    }

    /** Genummerde stap: geel vierkant met nummer, titel en uitleg. */
    private fun step(col: LinearLayout, nr: Int, title: String, body: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(TextView(this).apply {
            text = nr.toString()
            textSize = 16f
            typeface = Brand.BOLD
            gravity = Gravity.CENTER
            setTextColor(Brand.BLACK)
            setBackgroundColor(Brand.YELLOW)
        }, LinearLayout.LayoutParams(dp(32), dp(32)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        texts.addView(TextView(this).apply {
            text = title
            textSize = 17f
            typeface = Brand.BOLD
            setTextColor(Color.WHITE)
        })
        texts.addView(text(body), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) })
        row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        col.addRow(row, top = 24)
    }

    private fun open(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
