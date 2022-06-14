buildscript {
    repositories {
        mavenCentral {
            content {
                includeGroup("com.fasterxml")
                includeGroup("com.fasterxml.jackson")
                includeGroup("com.fasterxml.jackson.core")
                includeGroup("com.google.code.findbugs")
                includeGroup("com.google.code.gson")
                includeGroup("com.google.errorprone")
                includeGroup("com.google.guava")
                includeGroup("com.google.j2objc")
                includeGroup("com.machinezoo.noexception")
                includeGroup("commons-codec")
                includeGroup("commons-io")
                includeGroup("commons-logging")
                includeGroup("de.siegmar")
                includeGroup("it.unimi.dsi")
                includeGroup("net.sf.jopt-simple")
                includeGroup("net.sf.trove4j")
                includeGroup("org.apache")
                includeGroup("org.apache.commons")
                includeGroup("org.apache.httpcomponents")
                includeGroup("org.apache.httpcomponents.client5")
                includeGroup("org.apache.httpcomponents.core5")
                includeGroup("org.apache.logging.log4j")
                includeGroup("org.apache.maven")
                includeGroup("org.cadixdev")
                includeGroup("org.checkerframework")
                includeGroup("org.codehaus.plexus")
                includeGroup("org.immutables")
                includeGroup("org.jetbrains")
                includeGroup("org.jetbrains.kotlinx")
                includeGroup("org.ow2")
                includeGroup("org.ow2.asm")
                includeGroup("org.slf4j")
                includeGroup("org.sonatype.oss")
                includeGroup("org.tukaani")
            }
        }
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
            content {
                includeGroup("fabric-loom")
                includeGroup("net.fabricmc")
                includeGroup("net.fabricmc.fabric-api")
            }
        }
        maven("https://server.bbkr.space/artifactory/libs-release/") {
            name = "Quiltflower"
            content {
                includeGroup("io.github.juuxel")
            }
        }
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge Snapshots"
            content {
                includeGroup("org.spongepowered")
                includeGroup("org.spongepowered.gradle.vanilla")
            }
        }
        maven ("https://maven.minecraftforge.net") {
            name = "Forge"
            content {
                includeGroup("net.minecraftforge")
                includeGroup("net.minecraftforge.gradle")
            }
        }
        gradlePluginPortal {
            content {
                includeGroup("com.modrinth.minotaur")
                includeGroup("gradle.plugin.org.cadixdev.gradle")
                includeGroup("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext")
                includeGroup("net.darkhax.curseforgegradle")
            }
        }
    }

    dependencies {
        classpath("com.modrinth.minotaur:Minotaur:2.3.0")
        classpath("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
        classpath("io.github.juuxel:loom-quiltflower:1.7.2")
        classpath("net.darkhax.curseforgegradle:CurseForgeGradle:1.0.11")
        classpath("net.fabricmc:fabric-loom:0.12-SNAPSHOT")
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.+") { isChanging = true }
        classpath("org.spongepowered.gradle.vanilla:org.spongepowered.gradle.vanilla.gradle.plugin:0.2.1-SNAPSHOT")
    }
}

subprojects ModProject@ {
    val minecraftVersion = project.property("minecraft.version").toString()
    val minecraftVersionMajorMinor = minecraftVersion.split(".")[0] + "." + minecraftVersion.split(".")[1]
    val fabricLoader = project.property("fabric.loader").toString()
    val fabricApi = project.property("fabric.api").toString()
    val forgeVersion = project.property("forge").toString()

    val modName = project.property("mod_name").toString()
    val modId = project.property("mod_id").toString()
    val modAuthor = project.property("mod_author").toString()
    val modVersion = project.property("mod_version").toString()
    val decoratedModVersion = "${modVersion}+${minecraftVersion}"

    version = decoratedModVersion
    group = project.property("mod_group").toString()

    subprojects SubProject@ {
        apply(plugin = "java")
        apply(plugin = "maven-publish")
        apply(plugin = "org.cadixdev.licenser")

        version = decoratedModVersion
        group = project.property("mod_group").toString()

        configure<BasePluginExtension> {
            archivesName.set("${modName}-${name.toLowerCase()}")
        }

        configure<org.cadixdev.gradle.licenser.LicenseExtension> {
            properties {
                set("mod", modName)
                set("author", modAuthor)
                set("year", "2022")
            }
            setHeader(rootProject.file("LICENSE_HEADER"))
            include("**/io/github/marcus8448/**/*.java", "build.gradle.kts")
        }
        
        val sourceSets = this@SubProject.extensions.getByType(JavaPluginExtension::class).sourceSets
        val commonMainSourceSet = this@ModProject.project("Common").extensions.getByType(JavaPluginExtension::class).sourceSets["main"]

        // Minify json resources (https://stackoverflow.com/a/41029113)
        tasks.withType(ProcessResources::class) {
            doLast {
                fileTree(mapOf("dir" to outputs.files.asPath, "includes" to listOf("**/*.json", "**/*.mcmeta"))).forEach {
                        file: File -> file.writeText(groovy.json.JsonOutput.toJson(groovy.json.JsonSlurper().parse(file)))
                }
            }
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType(Jar::class) {
            from(rootProject.projectDir) {
                include("LICENSE").rename { "${it}_${modName}" }
            }

            manifest {
                attributes(
                        "Specification-Title"      to modName,
                        "Specification-Vendor"     to modAuthor,
                        "Specification-Version"    to this@withType.archiveVersion,
                        "Implementation-Title"     to project.name,
                        "Implementation-Version"   to this@withType.archiveVersion,
                        "Implementation-Vendor"    to modAuthor,
                        "Implementation-Timestamp" to java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME),
                        "Timestampe"               to System.currentTimeMillis(),
                        "Built-On-Java"            to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                        "Build-On-Minecraft"       to minecraftVersion
                )
            }
        }

        repositories {
            mavenCentral()

            maven("https://repo.spongepowered.org/repository/maven-public/") {
                name = "Sponge / Mixin"
            }
        }

        tasks.withType(JavaCompile::class).configureEach {
            options.encoding = "UTF-8"
            options.release.set(17)
        }

        // Disables Gradle's custom module metadata from being published to maven. The
        // metadata includes mapped dependencies which are not reasonably consumable by
        // other mod developers.
        tasks.withType(GenerateModuleMetadata::class) {
            enabled = false
        }

        when (name) {
            "Common" -> {
                apply(plugin = "org.spongepowered.gradle.vanilla")

                configure<org.spongepowered.gradle.vanilla.MinecraftExtension> {
                    version(minecraftVersion)
                }

                dependencies {
                    "compileOnly"("org.spongepowered:mixin:0.8.5")
                    "implementation"("com.google.code.findbugs:jsr305:3.0.1")
                }

                tasks.withType(ProcessResources::class) {
                    filesMatching("pack.mcmeta") {
                        expand(this@ModProject.properties)
                    }
                }

                configure<PublishingExtension> {
                    publications {
                        register("mavenJava", MavenPublication::class) {
                            groupId = this@SubProject.group.toString()
                            artifactId = this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                            version = this@SubProject.version.toString()
                            from(components["java"])
                        }
                    }

                    repositories {
                        mavenLocal()
                    }
                }
            }
            "Fabric" -> {
                apply(plugin = "fabric-loom")
                apply(plugin = "idea")

                val fabricModules = project.property("fabric.api.modules").toString().split(",".toRegex())

                configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
                    if (project.file("src/main/resources/${modId}.accesswidener").exists()) {
                        accessWidenerPath.set(project.file("src/main/resources/${modId}.accesswidener"))
                    }

                    runs {
                        getByName("client") {
                            client()
                            configName = "Fabric Client"
                            ideConfigGenerated(true)
                            runDir("run")
                        }
                        getByName("server") {
                            server()
                            configName = "Fabric Server"
                            ideConfigGenerated(true)
                            runDir("run")
                        }
                    }
                }

                dependencies {
                    "minecraft"("com.mojang:minecraft:${minecraftVersion}")
                    "mappings"(this@SubProject.extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class).officialMojangMappings())
                    "modImplementation"("net.fabricmc:fabric-loader:${fabricLoader}")
                    "implementation"("com.google.code.findbugs:jsr305:3.0.1")
                    "implementation"(this@ModProject.project("Common"))

                    if (fabricModules.size == 1 && fabricModules[0] == "*") {
                        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${fabricApi}")
                    } else {
                        if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) {
                            val apiExt = this@SubProject.extensions.getByType(net.fabricmc.loom.configuration.FabricApiExtension::class)
                            for (module in fabricModules) {
                                "modImplementation"(
                                    "net.fabricmc.fabric-api:${module}:${
                                        apiExt.moduleVersion(
                                            module,
                                            fabricApi
                                        )
                                    }"
                                ) {
                                    isTransitive = true
                                    exclude(module = "fabric-loader")
                                }
                            }
                        }
                    }
                }

                tasks.withType(ProcessResources::class) {
                    from(commonMainSourceSet.resources)
                    filesMatching("fabric.mod.json") {
                        val map = HashMap(this@ModProject.properties)
                        map["mc_major_minor"] = minecraftVersionMajorMinor
                        expand(map)
                    }

                    doLast {
                        val modJson = outputs.files.singleFile.resolve("fabric.mod.json")
                        if (modJson.exists()) {
                            val json = groovy.json.JsonSlurper().parse(modJson) as MutableMap<String, Any>
                            run {
                                var file1: File
                                if (!json.containsKey("mixins")) {
                                    val mixins = ArrayList<String>()
                                    file1 = modJson.resolveSibling("${modId}.mixins.json");
                                    if (file1.exists()) mixins.add(file1.name)
                                    file1 = modJson.resolveSibling("${modId}.client.mixins.json");
                                    if (file1.exists()) mixins.add(file1.name)
                                    file1 = modJson.resolveSibling("${modId}.server.mixins.json");
                                    if (file1.exists()) mixins.add(file1.name)
                                    json["mixins"] = mixins
                                }
                                if (!json.containsKey("accessWidener")) {
                                    file1 = modJson.resolveSibling("${modId}.accesswidener")
                                    if (file1.exists()) json["accessWidener"] = file1.name
                                }

                                if (!json.containsKey("icon")) {
                                    file1 = modJson.resolveSibling("${modId}.png")
                                    if (file1.exists()) json["icon"] = file1.name
                                }

                                if (!json.containsKey("license")) json["license"] = "LGPL-3.0-only"

                                val depends = json["depends"] as MutableMap<String, Any>
                                if (fabricModules.isEmpty() || fabricModules[0] == "*") {
                                    depends["fabric"] = "*"
                                } else if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) {
                                    fabricModules.forEach { depends[it] = "*" }
                                }
                            }
                            modJson.writeText(groovy.json.JsonOutput.toJson(json))
                        }
                        fileTree(mapOf("dir" to outputs.files.asPath, "includes" to listOf("**/*.json", "**/*.mcmeta"))).forEach {
                                file1: File -> file1.writeText(groovy.json.JsonOutput.toJson(groovy.json.JsonSlurper().parse(file1)))
                        }
                    }
                }

                tasks.withType(JavaCompile::class) {
                    source(commonMainSourceSet.allSource)
                }

                configure<PublishingExtension> {
                    publications {
                        register("mavenJava", MavenPublication::class) {
                            groupId = this@SubProject.group.toString()
                            artifactId = this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                            version = this@SubProject.version.toString()
                            from(components["java"])
                        }
                    }

                    repositories {
                        mavenLocal()
                    }
                }

                val modrinthId = if (this@ModProject.hasProperty("modrinth.id.fabric")) { this@ModProject.property("modrinth.id.fabric").toString() } else { this@ModProject.property("modrinth.id").toString() }
                val curseforgeId = if (this@ModProject.hasProperty("curseforge.id.fabric")) { this@ModProject.property("curseforge.id.fabric").toString() } else { this@ModProject.property("curseforge.id").toString() }

                if (modrinthId.isNotBlank() && System.getenv().containsKey("MODRINTH_TOKEN")) {
                    apply(plugin = "com.modrinth.minotaur")

                    configure<com.modrinth.minotaur.ModrinthExtension> {
                        uploadFile.set(tasks.getByName("remapJar"))
                    }

                    tasks.getByName<com.modrinth.minotaur.TaskModrinthUpload>("modrinth") modrinth@ {
                        beforeEvaluate {
                            if (this@modrinth.project.name == "Fabric") {
                                configure<com.modrinth.minotaur.ModrinthExtension> {
                                    if (this@SubProject.name == "Fabric") {
                                        token.set(System.getenv("MODRINTH_TOKEN"))
                                        projectId.set(modrinthId)
                                        versionNumber.set("${decoratedModVersion}-fabric")
                                        versionName.set("$modName v${this@ModProject.version} (Fabric)")
                                        if (System.getenv().containsKey("BETA") && System.getenv("BETA").toBoolean()) {
                                            versionType.set("beta")
                                        } else {
                                            versionType.set("release")
                                        }
                                        uploadFile.set(tasks.getByName("remapJar"))
                                        this@modrinth.dependsOn(uploadFile.get())
                                        gameVersions.addAll(minecraftVersion)
                                        loaders.add("fabric")
                                        syncBodyFrom.set(this@ModProject.file("README.md").readText())

                                        dependencies {
                                            if (fabricModules.isNotEmpty() && fabricModules[0]
                                                    .isNotBlank()
                                            ) required.project("P7dR8mSH") // Fabric Api
                                        }

                                        if (System.getenv().containsKey("CHANGELOG")) {
                                            changelog.set(System.getenv("CHANGELOG").toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (curseforgeId.isNotBlank() && System.getenv().containsKey("CURSEFORGE_TOKEN")) {
                    apply(plugin = "net.darkhax.curseforgegradle")

                    tasks.create("publishCurseforge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
                        apiToken = System.getenv("CURSEFORGE_TOKEN").toString()
                        val mainFile = upload(curseforgeId, tasks.getByName("remapJar"))
                        mainFile.addGameVersion("fabric")
                        mainFile.addGameVersion(minecraftVersion)
                        mainFile.addJavaVersion("Java 17")
                        mainFile.displayName = "$modName $modVersion (Fabric ${minecraftVersion})"
                        if (System.getenv().containsKey("BETA") && System.getenv("BETA").toBoolean()) {
                            mainFile.releaseType = "beta"
                        } else {
                            mainFile.releaseType = "release"
                        }

                        if (System.getenv().containsKey("CHANGELOG")) {
                            mainFile.changelog = System.getenv("CHANGELOG").toString()
                        } else {
                            mainFile.changelog = "No changelog provided."
                        }
                    }
                }
            }
            "Forge" -> {
                apply(plugin = "net.minecraftforge.gradle")

                (extensions["minecraft"] as net.minecraftforge.gradle.common.util.MinecraftExtension).apply {
                    mappings("official", minecraftVersion)

                    if (file("src/main/resources/META-INF/accesstransformer.cfg").exists()) {
                        accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))
                        project.logger.debug("Loading access transformer for ${this@ModProject.name}.")
                    }

                    runs {
                        create("client") {
                            workingDirectory(project.file("run"))
                            ideaModule("${rootProject.name}.${this@ModProject.name}.${project.name}.main")
                            property("mixin.env.remapRefMap", "true")
                            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
                            mods {
                                create(modId) {
                                    source(sourceSets["main"])
                                    source(commonMainSourceSet)
                                }
                            }
                        }

                        create("server") {
                            workingDirectory(project.file("run"))
                            ideaModule("${rootProject.name}.${this@ModProject.name}.${project.name}.main")
                            property("mixin.env.remapRefMap", "true")
                            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
                            mods {
                                create(modId) {
                                    source(sourceSets["main"])
                                    source(commonMainSourceSet)
                                }
                            }
                        }

                        create("data") {
                            workingDirectory(project.file("run"))
                            ideaModule("${rootProject.name}.${this@ModProject.name}.${project.name}.main")
                            args("--mod", modId, "--all", "--output", file("src/generated/resources/"), "--existing", file("src/main/resources/"))
                            property("mixin.env.remapRefMap", "true")
                            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
                            mods {
                                create(modId) {
                                    source(sourceSets["main"])
                                    source(commonMainSourceSet)
                                }
                            }
                        }
                    }
                }
                sourceSets["main"].resources.srcDir("src/generated/resources")

                dependencies {
                    "minecraft"("net.minecraftforge:forge:${minecraftVersion}-${forgeVersion}")
                    "compileOnly"(this@ModProject.project("Common"))
                }

                tasks.withType(JavaCompile::class) {
                    source(commonMainSourceSet.allSource)
                }

                tasks.withType(ProcessResources::class) {
                    from(commonMainSourceSet.resources)
                    filesMatching("META-INF/mods.toml") {
                        val map = HashMap(this@ModProject.properties);
                        map["forge_major"] = forgeVersion.split(".")[0];
                        map["mc_major_minor"] = minecraftVersionMajorMinor;
                        expand(map)
                    }
                }

                tasks["jar"].finalizedBy("reobfJar")

                configure<PublishingExtension> {
                    publications {
                        register("mavenJava", MavenPublication::class) {
                            groupId = this@SubProject.group.toString()
                            artifactId = this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                            version = this@SubProject.version.toString()
                            artifact(tasks["jar"])
                        }
                    }

                    repositories {
                        mavenLocal()
                    }
                }

                val modrinthId = if (this@ModProject.hasProperty("modrinth.id.forge")) { this@ModProject.property("modrinth.id.forge").toString() } else { this@ModProject.property("modrinth.id").toString() }
                val curseforgeId = if (this@ModProject.hasProperty("curseforge.id.forge")) { this@ModProject.property("curseforge.id.forge").toString() } else { this@ModProject.property("curseforge.id").toString() }

                if (modrinthId.isNotBlank() && System.getenv().containsKey("MODRINTH_TOKEN")) {
                    apply(plugin = "com.modrinth.minotaur")

                    configure<com.modrinth.minotaur.ModrinthExtension> {
                        uploadFile.set(tasks.getByName("jar"))
                    }

                    tasks.getByName<com.modrinth.minotaur.TaskModrinthUpload>("modrinth") modrinth@{
                        beforeEvaluate {
                            if (this@modrinth.project.name == "Forge") {
                                configure<com.modrinth.minotaur.ModrinthExtension> {
                                    token.set(System.getenv("MODRINTH_TOKEN"))
                                    projectId.set(modrinthId)
                                    versionNumber.set("{$decoratedModVersion}-forge")
                                    versionName.set("$modName v${this@ModProject.version} (Forge)")
                                    if (System.getenv().containsKey("BETA") && System.getenv("BETA").toBoolean()) {
                                        versionType.set("beta")
                                    } else {
                                        versionType.set("release")
                                    }
                                    uploadFile.set(tasks.getByName("jar"))
                                    this@modrinth.dependsOn(uploadFile.get())
                                    gameVersions.addAll(minecraftVersion)
                                    loaders.add("forge")
                                    syncBodyFrom.set(this@ModProject.file("README.md").readText())

                                    if (System.getenv().containsKey("CHANGELOG")) {
                                        changelog.set(System.getenv("CHANGELOG").toString())
                                    }
                                }
                            }
                        }
                    }
                }

                if (curseforgeId.isNotBlank() && System.getenv().containsKey("CURSEFORGE_TOKEN")) {
                    apply(plugin = "net.darkhax.curseforgegradle")

                    tasks.create("publishCurseForge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
                        apiToken = System.getenv("CURSEFORGE_TOKEN").toString()
                        val mainFile = upload(curseforgeId, tasks.getByName("jar"))
                        if (System.getenv().containsKey("BETA") && System.getenv("BETA").toBoolean()) {
                            mainFile.releaseType = "beta"
                        } else {
                            mainFile.releaseType = "release"
                        }
                        mainFile.addGameVersion("forge")
                        mainFile.addGameVersion(minecraftVersion)
                        mainFile.addJavaVersion("Java 17")
                        mainFile.displayName = "$modName $modVersion (Forge ${minecraftVersion})"

                        if (System.getenv().containsKey("CHANGELOG")) {
                            mainFile.changelog = System.getenv("CHANGELOG").toString()
                        } else {
                            mainFile.changelog = "No changelog provided."
                        }
                    }
                }
            }
        }
    }
}

if (System.getenv().containsKey("RELEASE_NAME")) {
    val publishRelease = tasks.create("publishRelease") {}
    val names = System.getenv("RELEASE_NAME").split(" ")
    for (name in names) {
        if (project.childProjects.contains(name)) {
            val modProject = project.childProjects[name]!!
            modProject.subprojects.forEach { innerProject ->
                innerProject.tasks.forEach { task ->
                    if (task.name == "modrinth" || task.name == "publishCurseforge") {
                        publishRelease.finalizedBy(task)
                    }
                }
            }
        } else {
            println("Skipping: $name")
        }
    }
}
