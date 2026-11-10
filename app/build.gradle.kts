plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Die Build-Nummer kommt von GitHub Actions (-PbuildNummer=...), damit jede neue
// Version über die alte drüberinstalliert werden kann.
val buildNummer = (project.findProperty("buildNummer") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "de.fernseher.startseite"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.fernseher.startseite"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNummer
        versionName = "0.1.$buildNummer"
    }

    signingConfigs {
        create("startseite") {
            // Fester Schlüssel, damit Updates ohne Deinstallieren funktionieren.
            storeFile = rootProject.file("signing/startseite.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORT") ?: "startseite"
            keyAlias = "startseite"
            keyPassword = System.getenv("KEYSTORE_PASSWORT") ?: "startseite"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("startseite")
        }
        debug {
            signingConfig = signingConfigs.getByName("startseite")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
