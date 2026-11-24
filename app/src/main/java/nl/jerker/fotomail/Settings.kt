package nl.jerker.fotomail

import android.content.Context

/** Eenvoudige opslag van de instellingen (blijft alleen op de telefoon). */
class Settings(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("fotomail", Context.MODE_PRIVATE)

    var sender: String
        get() = p.getString("sender", DEFAULT_SENDER) ?: DEFAULT_SENDER
        set(v) = p.edit().putString("sender", v.trim()).apply()

    /** Gmail app-wachtwoord (16 tekens), niet je gewone wachtwoord. */
    var appPassword: String
        get() = p.getString("appPassword", "") ?: ""
        set(v) = p.edit().putString("appPassword", v.replace(" ", "").trim()).apply()

    var recipient: String
        get() = p.getString("recipient", DEFAULT_RECIPIENT) ?: DEFAULT_RECIPIENT
        set(v) = p.edit().putString("recipient", v.trim()).apply()

    var subjectPrefix: String
        get() = p.getString("subjectPrefix", "Foto") ?: "Foto"
        set(v) = p.edit().putString("subjectPrefix", v.trim()).apply()

    /** false = foto verkleinen tot max. 2560 px (sneller versturen), true = origineel. */
    var keepOriginal: Boolean
        get() = p.getBoolean("keepOriginal", false)
        set(v) = p.edit().putBoolean("keepOriginal", v).apply()

    /** Camera meteen openen bij het starten van de app. */
    var autoCamera: Boolean
        get() = p.getBoolean("autoCamera", true)
        set(v) = p.edit().putBoolean("autoCamera", v).apply()

    var lastStatus: String
        get() = p.getString("lastStatus", "") ?: ""
        set(v) = p.edit().putString("lastStatus", v).apply()

    val isConfigured: Boolean
        get() = sender.contains("@") && appPassword.isNotBlank() && recipient.contains("@")

    companion object {
        const val DEFAULT_RECIPIENT = "j.wouters@allbrass.com"
        const val DEFAULT_SENDER = "jerker.wouters@gmail.com"
    }
}
