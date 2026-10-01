pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // Multi-version management: https://stonecutter.kikugie.dev
    id("dev.kikugie.stonecutter") version "0.9.7"

    // Automatically picks the matching Loom variant:
    // - Minecraft < 26.1 (obfuscated)    -> "fabric-loom"
    // - Minecraft >= 26.1 (unobfuscated) -> "net.fabricmc.fabric-loom"
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Downloads missing JDKs automatically (e.g. Java 21 for 1.21.11)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Version matrix. The first value is the build node name,
        // the second the actual Minecraft version it compiles against.
        // 26.1, 26.1.1 and 26.1.2 are compatible with each other -> one node.
        version("1.21.11", "1.21.11")
        version("26.1.x", "26.1.2")
        version("26.2.x", "26.2")
        version("26.3.x", "26.3")

        // Version that is active in the Git/IDE state
        vcsVersion = "26.2.x"
    }
}

rootProject.name = "OviClicker"
