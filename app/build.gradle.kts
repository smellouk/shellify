import java.util.Properties

val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }

val gitVersionName = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().removePrefix("v").ifEmpty { "1.0.0" } }

plugins {
    id("shellify.android.application")
    id("shellify.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.roborazzi)
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
        versionCode = gitCommitCount.get()
        versionName = gitVersionName.get()

        vectorDrawables { useSupportLibrary = true }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val propsFile = rootProject.file("app/release.properties")
            if (propsFile.exists()) props.load(propsFile.inputStream())

            fun prop(key: String) = props.getProperty(key) ?: System.getenv(key)

            val storeFilePath = prop("SIGNING_STORE_FILE")
            if (!storeFilePath.isNullOrEmpty()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = prop("SIGNING_STORE_PASSWORD")
                keyAlias = prop("SIGNING_KEY_ALIAS")
                keyPassword = prop("SIGNING_KEY_PASSWORD")
                storeType = "PKCS12"
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
        sarifReport = true
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
    implementation(project(":core:navigation"))

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
    implementation(project(":feature:link-dispatcher"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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
    testImplementation(libs.compose.ui.test.junit4)

    // Detekt formatting rules (ktlint-based)
    detektPlugins(libs.detekt.formatting)

    // Instrumented tests
    androidTestImplementation(libs.androidx.swiperefreshlayout)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.biometric)
    androidTestImplementation(libs.sqlcipher)
}

// espresso-core:3.7.0 requires concurrent-futures:1.2.0, but the Compose BOM strictly pins 1.1.0.
// Force 1.2.0 only for androidTest so the InputManager.getInstance fix works on API 35+.
aboutLibraries {
    // Ensures the plugin re-collects when Gradle inputs change, preventing stale empty JSON.
    configPath = "config/aboutlibraries"
    offlineMode = false
}

roborazzi {
    outputDir.set(file("src/test/snapshots/images"))
}

configurations.configureEach {
    if (name.contains("AndroidTest", ignoreCase = true)) {
        resolutionStrategy.force("androidx.concurrent:concurrent-futures:1.2.0")
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }
}
