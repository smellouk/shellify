plugins {
    id("shellify.android.library")
    id("shellify.compose")
}
android { namespace = "io.shellify.feature.webview" }
dependencies {
    implementation(project(":core:domain"))
    api(project(":core:engine"))
    implementation(project(":core:isolation"))
    implementation(project(":core:security"))
    implementation(project(":core:translate"))
    implementation(project(":core:theme"))
    implementation(project(":core:ui"))
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
