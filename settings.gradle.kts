pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
                includeGroup("net.fabricmc.fabric-api")
            }
        }
        maven ("https://maven.neoforged.net/releases") {
            name = "NeoForge"
            content {
                includeGroup("codechicken")
                includeGroup("net.minecraftforge")
                includeGroup("net.neoforged")
                includeGroup("net.neoforged.gradle")
                includeGroup("net.neoforged.gradle.userdev")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "MinecraftMods"

fun mod(name: String) {
    include(":${name}", ":${name}:Common", ":${name}:Fabric", ":${name}:Forge")
}

mod("GamemodeOverhaul")
//mod("Snowy")
//mod("Template")
