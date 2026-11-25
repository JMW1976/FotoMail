# Photo to Mail

Android-app: foto maken → wordt automatisch als bijlage gemaild naar een zelf in te stellen adres, verstuurd via een eigen Gmail-account (Gmail-SMTP). Een handleiding voor het instellen staat in de app.

## Werking
- App openen → camera opent direct (uit te zetten in Instellingen).
- Na de foto: verkleind tot max. 2560 px (of origineel), in een wachtrij gezet en verstuurd.
- Geen internet? De foto blijft in de wachtrij en gaat automatisch mee zodra er verbinding is (ook na een herstart).
- Elke foto = één mail, onderwerp `Foto 2026-11-24 07-30-12`.

## Eenmalig instellen
1. Google-account van de afzender: zet **tweestapsverificatie** aan.
2. Ga naar https://myaccount.google.com/apppasswords → maak een app-wachtwoord ("Foto Mail") → kopieer de 16 tekens.
3. App → Instellingen → plak het app-wachtwoord → **Testmail versturen**.

## APK bouwen
**Android Studio:** File → Open → map `FotoMail` → Build → Build APK(s).

**GitHub (zonder Android Studio):** zet de map in een (privé) repository en push. De workflow `Build APK` bouwt de APK; download die onder Actions → laatste run → Artifacts → `FotoMail-apk`.

Installeren: APK op de telefoon openen en "installeren uit onbekende bronnen" toestaan.

## Code
`app/src/main/java/nl/jerker/fotomail/`
- `MainActivity` – startscherm, camera, wachtrijstatus
- `SettingsActivity` – Gmail-gegevens, ontvanger, opties, testmail
- `SendJobService` – verstuurt de wachtrij (JobScheduler, met automatische herhaalpogingen)
- `Mailer` – SMTP via smtp.gmail.com:465 (JavaMail for Android)
- `PhotoStore` / `PhotoProvider` – opslag, verkleinen/rechtzetten, overdracht met de camera-app

Geen extra Android-bibliotheken; alleen `com.sun.mail:android-mail`.
