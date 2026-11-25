plugins { id("com.android.application") }

// Every tagged Lab version has its own applicationId so it installs NEXT TO
// earlier Labs and ZIMT instead of replacing them. Increase LAB_BUILD for each
// user-facing version; never silently reuse an old ID for a different version.
val labBuild = 16
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
        manifestPlaceholders["labLabel"] = "Garten & Küche $labVersion"
        resValue("string", "lab_version", labVersion)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
