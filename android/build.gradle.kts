// Plugin versions are set in settings.gradle.kts. Declaring them here keeps
// the Android and Kotlin plugins in one classloader.
plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("org.jetbrains.kotlin.jvm") apply false
    id("com.google.gms.google-services") apply false
}
