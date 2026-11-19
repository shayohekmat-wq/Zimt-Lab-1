plugins { id("com.android.application") }

// Every tagged Lab version has its own applicationId so it installs NEXT TO
// earlier Labs and ZIMT instead of replacing them. Increase LAB_BUILD for each
// user-facing version; never silently reuse an old ID for a different version.
val labBuild = 3
val labVersion = "0.0.$labBuild"

android {
    namespace = "de.naehe.gartenkueche"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.naehe.app.gartenkueche.lab$labBuild"
        minSdk = 23
        targetSdk = 34
        versionCode = labBuild
        versionName = labVersion
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
