// Lets the core module build on its own (no Android SDK needed):
//   gradle -p android/core test
pluginManagement {
    repositories { mavenCentral(); gradlePluginPortal() }
    plugins { id("org.jetbrains.kotlin.jvm") version "2.2.20" }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
rootProject.name = "core"
