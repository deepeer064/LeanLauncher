plugins { id("com.android.application") }
android {
    namespace = "com.example.leanlauncher"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.leanlauncher"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
