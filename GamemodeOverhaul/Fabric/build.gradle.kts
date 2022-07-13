repositories {
    maven("https://maven.terraformersmc.com/releases/") {
        content {
            includeGroup("com.terraformersmc")
        }
    }
    maven("https://maven.shedaniel.me/") {
        content {
            includeGroup("me.shedaniel.cloth.api")
            includeGroup("me.shedaniel.cloth")
            includeGroup("me.shedaniel")
        }
    }
}

dependencies {
    "modImplementation"("com.terraformersmc:modmenu:${project.property("modmenu")}") { isTransitive = false }
    "modImplementation"("me.shedaniel.cloth:cloth-config-fabric:${project.property("cloth_config")}") {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
}
