plugins {
    id("shellify.android.library")
}

android {
    namespace = "io.shellify.core.pwa"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:theme"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
