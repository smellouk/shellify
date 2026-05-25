pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.mozilla.org/maven2/") }
        // Guardian Project Maven — provides info.guardianproject:tor-android only.
        // jtorctl resolves from mavenCentral() above.
        maven { url = uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master") }
    }
}

rootProject.name = "Shellify"

include(":app")

// Core modules
include(":core:domain")
include(":core:crypto")
include(":core:security")
include(":core:locale")
include(":core:ui")
include(":core:database")
include(":core:engine")
include(":core:isolation")
include(":core:iconpack")
include(":core:pwa")
include(":core:shortcut")
include(":core:deeplink")
include(":core:translate")
include(":core:webbridge")
include(":core:theme")
include(":core:backup")
include(":core:navigation")

// Feature modules
include(":feature:home")
include(":feature:add")
include(":feature:category")
include(":feature:settings")
include(":feature:onboarding")
include(":feature:shortcuts")
include(":feature:translate")
include(":feature:webview")
include(":feature:share")
include(":feature:shortcut")
include(":feature:link-dispatcher")
