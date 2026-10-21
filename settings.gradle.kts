plugins {
    // Lets Gradle download JDK 21 automatically if it is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "flight-log-analyzer"
