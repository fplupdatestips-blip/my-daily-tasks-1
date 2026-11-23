import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}

tasks.test {
    // The shared test cases live at the repo root, used by the web tests too.
    systemProperty("repeatCases", project.file("../../shared/repeat-cases.json").absolutePath)
    systemProperty("calendarCases", project.file("../../shared/calendar-cases.json").absolutePath)
    systemProperty("footballCases", project.file("../../shared/football-cases.json").absolutePath)
}
