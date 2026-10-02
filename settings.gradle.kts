import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "HibernateInspectionsPlugin"

plugins {
    // Lets Gradle download the JDK requested by the Java toolchain (see build.gradle.kts) if it's not installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // IntelliJ Platform Gradle Plugin: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
    id("org.jetbrains.intellij.platform.settings") version "2.19.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
