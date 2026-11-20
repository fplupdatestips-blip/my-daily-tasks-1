pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.0"
        id("org.jetbrains.kotlin.android") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("com.google.gms.google-services") version "4.4.3"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DailyTasks"

// :core is plain Kotlin (repeat rules, dates); it also builds on its own
// with `gradle -p core test`, which needs no Android SDK.
include(":core")
include(":app")
