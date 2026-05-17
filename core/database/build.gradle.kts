plugins {
    id("shellify.android.library")
    id("shellify.ksp")
}

// Must be top-level (not inside android {}) for Room's KSP processor to pick it up.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "io.shellify.core.database"

    sourceSets {
        // Expose the generated schema JSON files as assets in the androidTest APK so
        // MigrationTestHelper can read them when validating migration correctness.
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:crypto"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // SQLCipher
    implementation(libs.sqlcipher) { isTransitive = true }
    implementation(libs.androidx.sqlite.ktx)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
