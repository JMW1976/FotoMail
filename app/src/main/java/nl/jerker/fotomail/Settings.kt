package nl.jerker.fotomail

import android.content.Context

/** Eenvoudige opslag van de instellingen (blijft alleen op de telefoon). */
class Settings(context: Context) {
    private val app = context.applicationContext
    private val p = app.getSharedPreferences("fotomail", Context.MODE_PRIVATE)

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
        get() = p.getString("subjectPrefix", null) ?: app.getString(R.string.default_prefix)
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

    /** Details van de laatste geslaagde verzending (worden in de taal van de telefoon getoond). */
    val lastPhotos: Int get() = p.getInt("lastPhotos", 0)
    val lastMails: Int get() = p.getInt("lastMails", 0)
    val lastTime: Long get() = p.getLong("lastTime", 0L)
    /** Technische foutmelding van de laatste mislukte poging. */
    val lastError: String get() = p.getString("lastError", "") ?: ""

    fun setLastOk(photos: Int, mails: Int) {
        p.edit().putString("lastState", "ok").putInt("lastPhotos", photos).putInt("lastMails", mails)
            .putLong("lastTime", System.currentTimeMillis()).apply()
    }

    fun setLastFailure(state: String, technical: String = "") {
        p.edit().putString("lastState", state).putString("lastError", technical).apply()
    }

    val isConfigured: Boolean
        get() = sender.contains("@") && appPassword.isNotBlank() && recipient.contains("@")

    companion object {
        const val DEFAULT_RECIPIENT = ""
        const val DEFAULT_SENDER = ""
    }
}
