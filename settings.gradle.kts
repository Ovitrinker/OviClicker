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
    // Multiversion-Verwaltung: https://stonecutter.kikugie.dev
    id("dev.kikugie.stonecutter") version "0.9.7"

    // Waehlt automatisch die passende Loom-Variante:
    // - Minecraft < 26.1 (obfuskiert)  -> "fabric-loom"
    // - Minecraft >= 26.1 (unobfuskiert) -> "net.fabricmc.fabric-loom"
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Laedt fehlende JDKs (z. B. Java 21 fuer 1.21.11) automatisch nach
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Versionsmatrix. Der erste Wert ist der Name des Build-Knotens,
        // der zweite die tatsaechliche Minecraft-Version, gegen die kompiliert wird.
        // 26.1, 26.1.1 und 26.1.2 sind untereinander kompatibel -> ein Knoten.
        version("1.21.11", "1.21.11")
        version("26.1.x", "26.1.2")
        version("26.2.x", "26.2")

        // Version, die im Git/IDE-Zustand aktiv ist
        vcsVersion = "26.2.x"
    }
}

rootProject.name = "AutoClicker"
