rootProject.name = "MinecraftMods"

fun mod(name: String) {
    include(":${name}", ":${name}:Common", ":${name}:Fabric", ":${name}:Forge")
}

mod("GamemodeOverhaul")
mod("Snowy")
mod("Template")
