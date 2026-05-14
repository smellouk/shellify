plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    config.setFrom("${rootDir}/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
    source.setFrom("src/main/java", "src/main/kotlin")
    ignoredBuildTypes = listOf("release")
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    lint {
        lintConfig = file("${rootDir}/config/lint/lint.xml")
        abortOnError = false
        warningsAsErrors = false
        htmlReport = true
        xmlReport = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
        jniLibs {
            // GeckoView native libs are downloaded at runtime — exclude from APK to keep it small
            excludes += "**/libxul.so"
            excludes += "**/libmozglue.so"
            excludes += "**/liblgpllibs.so"
        }
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // SQLCipher — encrypted SQLite database
    implementation(libs.sqlcipher) { isTransitive = true }
    implementation(libs.androidx.sqlite.ktx)

    // WebKit — WebView profiles (API 33+) and other compat APIs
    implementation(libs.androidx.webkit)

    // Network
    implementation(libs.okhttp)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // JSON
    implementation(libs.gson)

    // DataStore (global app preferences)
    implementation(libs.androidx.datastore.preferences)

    // AppCompat (required for BiometricPrompt → FragmentActivity)
    implementation(libs.androidx.appcompat)

    // Biometric — system lock / fingerprint prompt
    implementation(libs.androidx.biometric)

    // WorkManager — scheduled backups
    implementation(libs.androidx.work.runtime.ktx)

    // DocumentFile — SAF-based file writing for backups
    implementation(libs.androidx.documentfile)

    // QR code generation
    implementation(libs.zxing.core)

    // GeckoView — Java/Kotlin API only; .so files excluded from APK (see packagingOptions) and downloaded at runtime
    implementation(libs.geckoview)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json)

    // Konsist — architecture consistency tests
    testImplementation(libs.konsist)

    // Detekt formatting rules (ktlint-based)
    detektPlugins(libs.detekt.formatting)

    // Instrumented tests
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.14")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
