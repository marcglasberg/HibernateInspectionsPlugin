import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

java {
    toolchain {
        // Java version required by the IntelliJ Platform we compile against (2026.2 requires Java 25).
        // See https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    intellijPlatform {
        // The OLDEST IDE we support (we only support the latest IntelliJ IDEA).
        // We compile against it, so we can't use APIs that don't exist there.
        // If you change it, also change `sinceBuild` below and the Java toolchain version above.
        intellijIdea("2026.2.3")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            // No until-build: the plugin stays compatible with future IDE versions.
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        // Fail the build on anything JetBrains could complain about, including deprecated API usage.
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
            VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
            VerifyPluginTask.FailureLevel.DEPRECATED_API_USAGES,
            VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
            VerifyPluginTask.FailureLevel.EXPERIMENTAL_API_USAGES,
            VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES,
            VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES,
            VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
        )
        ides {
            // Latest stable release (same as the one we compile against).
            create(IntelliJPlatformType.IntellijIdea, "2026.2.3")
            // Latest EAP (JetBrains' emails report against EAPs). Update the build number as new EAPs come out.
            // List: https://data.services.jetbrains.com/products/releases?code=IIU&type=eap&latest=true
            create(IntelliJPlatformType.IntellijIdea, "263.6259.32")
        }
    }

    publishing {
        // Create a token at https://plugins.jetbrains.com/author/me/tokens and put it in the PUBLISH_TOKEN environment variable.
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}
