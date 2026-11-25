plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "nl.jerker.fotomail"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.jerker.fotomail"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.7"
    }

    // Vaste sleutel, zodat elke nieuwe build als update over de vorige installeert
    signingConfigs {
        create("fotomail") {
            storeFile = file("fotomail.keystore")
            storePassword = "fotomail"
            keyAlias = "fotomail"
            keyPassword = "fotomail"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fotomail")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fotomail")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/LICENSE.md",
                "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/NOTICE.md"
            )
        }
    }
}

dependencies {
    // JavaMail voor Android: verstuurt de foto via Gmail-SMTP
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}
