plugins { id("com.android.application") }
android {
    namespace = "de.naehe.gartenkueche"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.naehe.app.gartenkueche.lab1"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
