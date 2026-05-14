plugins {
    id("shellify.android.application")
    id("shellify.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.roborazzi)
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

    defaultConfig {
        applicationId = "io.shellify.app"
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

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
            excludes += "**/libxul.so"
            excludes += "**/libmozglue.so"
            excludes += "**/liblgpllibs.so"
        }
    }
}

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:crypto"))
    implementation(project(":core:security"))
    implementation(project(":core:locale"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:engine"))
    implementation(project(":core:isolation"))
    implementation(project(":core:iconpack"))
    implementation(project(":core:pwa"))
    implementation(project(":core:shortcut"))
    implementation(project(":core:deeplink"))
    implementation(project(":core:translate"))
    implementation(project(":core:theme"))
    implementation(project(":core:backup"))

    // Feature modules
    implementation(project(":feature:home"))
    implementation(project(":feature:add"))
    implementation(project(":feature:category"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:shortcuts"))
    implementation(project(":feature:translate"))
    implementation(project(":feature:webview"))
    implementation(project(":feature:share"))
    implementation(project(":feature:shortcut"))

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

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore (global app preferences)
    implementation(libs.androidx.datastore.preferences)

    // Room runtime (needed to access AppDatabase supertype from :core:database)
    implementation(libs.androidx.room.runtime)

    // AppCompat (required for BiometricPrompt → FragmentActivity)
    implementation(libs.androidx.appcompat)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json)

    // Konsist — architecture consistency tests
    testImplementation(libs.konsist)

    // Screenshot tests (Roborazzi + Robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")

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
