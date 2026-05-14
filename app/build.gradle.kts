plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "io.shellify.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.shellify.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs {
            // GeckoView native libs are downloaded at runtime — exclude from APK to keep it small
            excludes += "**/libxul.so"
            excludes += "**/libmozglue.so"
            excludes += "**/liblgpllibs.so"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // SQLCipher — encrypted SQLite database
    implementation("net.zetetic:android-database-sqlcipher:4.5.4") { isTransitive = true }
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // WebKit — WebView profiles (API 33+) and other compat APIs
    implementation("androidx.webkit:webkit:1.12.1")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // DataStore (global app preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // AppCompat (required for BiometricPrompt → FragmentActivity)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Biometric — system lock / fingerprint prompt
    implementation("androidx.biometric:biometric:1.1.0")

    // WorkManager — scheduled backups
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // DocumentFile — SAF-based file writing for backups
    implementation("androidx.documentfile:documentfile:1.0.1")

    // QR code generation
    implementation("com.google.zxing:core:3.5.3")

    // GeckoView — Java/Kotlin API only; .so files excluded from APK (see packagingOptions) and downloaded at runtime
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:128.0.20240704121409")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
