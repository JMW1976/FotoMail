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

    /** true = elke foto meteen versturen; false = verzamelen en met één knop samen versturen. */
    var directSend: Boolean
        get() = p.getBoolean("directSend", true)
        set(v) = p.edit().putBoolean("directSend", v).apply()

    /** Onderwerp van de huidige/laatste fotoreeks. */
    var currentSubject: String
        get() = p.getString("currentSubject", "") ?: ""
        set(v) = p.edit().putString("currentSubject", v.trim()).apply()

    /** Resultaat van de laatste verzendpoging: "", "ok", "auth", "error" of "config". */
    var lastState: String
        get() = p.getString("lastState", "") ?: ""
        set(v) = p.edit().putString("lastState", v).apply()

    var lastDetail: String
        get() = p.getString("lastDetail", "") ?: ""
        set(v) = p.edit().putString("lastDetail", v).apply()

    fun setLast(state: String, detail: String) {
        p.edit().putString("lastState", state).putString("lastDetail", detail).apply()
    }

    val isConfigured: Boolean
        get() = sender.contains("@") && appPassword.isNotBlank() && recipient.contains("@")

    companion object {
        const val DEFAULT_RECIPIENT = ""
        const val DEFAULT_SENDER = ""
    }
}
