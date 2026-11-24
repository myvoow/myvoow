rootProject.name = "blockblast-patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "2.1.0"
}

settings {
    extensions {
        // Keeps the extension classes under their own names and drops everything else.
        // Must be an absolute path, otherwise extensions in subfolders do not find it.
        proguardFiles(rootProject.projectDir.resolve("extensions/proguard-rules.pro").toString())
    }
}

// Compile-only copies of the AppLovin MAX API the extension implements. Outside extensions/:
// the Morphe plugin turns every project in there into an extension.
include(":stub")
