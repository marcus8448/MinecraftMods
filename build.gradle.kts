import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.*

plugins {
    id("org.cadixdev.licenser") version("0.6.1")
    id("com.modrinth.minotaur") version("2.8.7") apply(false)
    id("net.darkhax.curseforgegradle") version("1.1.24") apply(false)
    id("fabric-loom") version("1.7-SNAPSHOT") apply(false)
    id("net.neoforged.gradle.userdev") version("7.0.153") apply(false)
//        id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

enum class Environment(val jsonName: String) {
    BOTH("*"), CLIENT("client"), SERVER("server");

    fun matches(other: Environment): Boolean {
        return this == BOTH || this == other
    }
}

val javaVersion = Integer.parseInt(project.property("java.version").toString())
val minecraftVersion = project.property("minecraft.version").toString()
val minecraftVersionMajorMinor = minecraftVersion.split(".")[0] + "." + minecraftVersion.split(".")[1]
val fabricLoader = project.property("fabric.loader").toString()
val fabricApix = project.property("fabric.api").toString()
val fmlVersion = project.property("fml").toString()
val forgeVersion = project.property("forge").toString()
val currentYear = Integer.parseInt(project.property("year").toString())

subprojects ModProject@{
    val modName = project.property("mod.name").toString()
    val modId = project.property("mod.id").toString()
    val modGroup = project.property("mod.group").toString()
    val modAuthors = project.property("mod.authors").toString().split(",")
    val modVersion = project.property("mod.version").toString()
    val modDescription = project.property("mod.description").toString()
    val modStartYear = Integer.parseInt(project.property("mod.start_year").toString())
    val modEnvironment = Environment.valueOf((project.findProperty("mod.environment") ?: "both").toString().uppercase())

    val decoratedModVersion = "${modVersion}+${minecraftVersion}"

    version = decoratedModVersion
    group = modGroup

    subprojects SubProject@{
        apply(plugin = "java")
        apply(plugin = "maven-publish")
        apply(plugin = "org.cadixdev.licenser")

        val sourceSets = this@SubProject.extensions.getByType(JavaPluginExtension::class).sourceSets
        val commonMainSourceSet =
            this@ModProject.project("Common").extensions.getByType(JavaPluginExtension::class).sourceSets["main"]
        val loaderName = this@SubProject.name
        var loaderId = loaderName.lowercase()
        if (loaderId == "forge") loaderId = "neoforge"

        val modrinthId = (this@SubProject.findProperty("modrinth.id") ?: "").toString()
        val curseforgeId = (this@SubProject.findProperty("curseforge.id") ?: "").toString()

        fun configureModrinth(
            task: Task,
            deps: (deps: com.modrinth.minotaur.dependencies.container.NamedDependencyContainer.Required) -> Unit
        ) {
            configure<com.modrinth.minotaur.ModrinthExtension> {
                uploadFile.set(task)
            }

            tasks.getByName<com.modrinth.minotaur.TaskModrinthUpload>("modrinth") modrinth@{
                beforeEvaluate {
                    if (this@modrinth.project.name == loaderName) {
                        configure<com.modrinth.minotaur.ModrinthExtension> {
                            token.set(System.getenv("MODRINTH_TOKEN"))
                            projectId.set(modrinthId)
                            versionNumber.set("${decoratedModVersion}-${loaderId}")
                            versionName.set("$modName v${this@ModProject.version} (${loaderName})")
                            if (System.getenv().containsKey("BETA") && System.getenv("BETA").toBoolean()) {
                                versionType.set("beta")
                            } else {
                                versionType.set("release")
                            }
                            uploadFile.set(task)
                            this@modrinth.dependsOn(uploadFile.get())
                            gameVersions.addAll(minecraftVersion)
                            loaders.add(loaderId)
                            syncBodyFrom.set(this@ModProject.file("README.md").readText())

                            deps.invoke(required)

                            if (System.getenv().containsKey("CHANGELOG")) {
                                changelog.set(System.getenv("CHANGELOG").toString())
                            }
                        }
                    }
                }
            }
        }

        fun configureCurseforge(task: Task, deps: (artifact: net.darkhax.curseforgegradle.UploadArtifact) -> Unit) {
            tasks.create("publishCurseforge", net.darkhax.curseforgegradle.TaskPublishCurseForge::class) {
                apiToken = System.getenv("CURSEFORGE_TOKEN").toString()
                val mainFile = upload(curseforgeId, task)
                mainFile.addGameVersion(loaderId)
                mainFile.addGameVersion(minecraftVersion)
                mainFile.addJavaVersion("Java $javaVersion")
                mainFile.displayName = "$modName $modVersion (${loaderName} ${minecraftVersion})"
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
                deps.invoke(mainFile)
            }
        }

        version = decoratedModVersion
        group = modGroup

        configure<BasePluginExtension> {
            archivesName.set("${modId}-${name.lowercase()}")
        }

        configure<org.cadixdev.gradle.licenser.LicenseExtension> {
            properties {
                set("mod", modName)
                set(
                    "author", if (modAuthors.size == 1) {
                        modAuthors[0]
                    } else {
                        var s = "";
                        for ((index, author) in modAuthors.withIndex()) {
                            if (index != 0) {
                                s += ", "
                            }
                            s += author
                        }
                        s
                    }
                )
                set(
                    "year_desc", if (modStartYear == currentYear) {
                        currentYear
                    } else {
                        "${modStartYear}-${currentYear}"
                    }
                )
            }
            setHeader(rootProject.file("LICENSE_HEADER"))
            include("**/io/github/marcus8448/**/*.java", "build.gradle.kts")
        }

        // Minify json resources (https://stackoverflow.com/a/41029113)
        tasks.withType(ProcessResources::class) {
            doLast {
                fileTree(
                    mapOf(
                        "dir" to outputs.files.asPath,
                        "includes" to listOf("**/*.json", "**/*.mcmeta")
                    )
                ).forEach { file: File ->
                    file.writeText(groovy.json.JsonOutput.toJson(parseJson(file)))
                }
            }
        }

        configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
            sourceCompatibility = JavaVersion.toVersion(javaVersion)
            targetCompatibility = JavaVersion.toVersion(javaVersion)
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType(Javadoc::class) {
            exclude("**/impl/**")
        }

        tasks.withType(Jar::class) Jar@{
            from(rootProject.projectDir) {
                include("LICENSE").rename { "${it}_${modName}" }
            }

            manifest {
                attributes(
                    "Specification-Title" to modName,
                    "Specification-Vendor" to modAuthors[0],
                    "Specification-Version" to modVersion,
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to this@Jar.archiveVersion,
                    "Implementation-Vendor" to modAuthors[0],
                    "Implementation-Timestamp" to LocalDateTime.now().format(ISO_DATE_TIME),
                    "Timestamp" to System.currentTimeMillis(),
                    "Built-On-Java" to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                    "Built-On-Minecraft" to minecraftVersion,
                    "Automatic-Module-Name" to modId
                )
            }
        }

        tasks.withType(JavaCompile::class).configureEach {
            options.encoding = "UTF-8"
            options.release.set(javaVersion)
        }

        // Disables Gradle's custom module metadata from being published to maven. The
        // metadata includes mapped dependencies which are not reasonably consumable by
        // other mod developers.
        tasks.withType(GenerateModuleMetadata::class) {
            enabled = false
        }

        when (name) {
            "Common" -> {
                apply(plugin = "fabric-loom")

                configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
                    if (project.parent!!.project("Fabric").file("src/main/resources/${modId}.accesswidener")
                            .exists()
                    ) {
                        accessWidenerPath.set(
                            project.parent!!.project("Fabric").file("src/main/resources/${modId}.accesswidener")
                        )
                    }
                }

                dependencies {
                    "minecraft"("com.mojang:minecraft:${minecraftVersion}")
                    "mappings"(
                        this@SubProject.extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class)
                            .officialMojangMappings()
                    )

                    "modCompileOnly"("net.fabricmc:fabric-loader:${fabricLoader}")
                    "implementation"("com.google.code.findbugs:jsr305:3.0.1")
                }

                tasks.withType(ProcessResources::class) {
                    filesMatching("pack.mcmeta") {
                        expand(
                            "mod_name" to modName,
                            "mod_id" to modId
                        )
                    }
                }

                configure<PublishingExtension> {
                    publications {
                        register("mavenJava", MavenPublication::class) {
                            groupId = this@SubProject.group.toString()
                            artifactId =
                                this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                            version = this@SubProject.version.toString()
                            from(components["java"])
                            addPomInfo(modName, modDescription, modAuthors)
                        }
                    }

                    repositories {
                        mavenLocal()
                    }
                }

                tasks.register("prepareWorkspace")
            }

            "Fabric" -> {
                apply(plugin = "fabric-loom")
                apply(plugin = "idea")

                val fabricModules = project.property("fabric.api.modules").toString().split(",".toRegex())
                val fabricModJson = parseJson(file("src/main/resources/fabric.mod.json")) // NOT EXPANDED
                val datagenEnabled = fabricModJson.obj("entrypoints").containsKey("fabric-datagen")

                configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
                    if (project.file("src/main/resources/${modId}.accesswidener").exists()) {
                        accessWidenerPath.set(project.file("src/main/resources/${modId}.accesswidener"))
                    }

                    runs {
                        if (modEnvironment.matches(Environment.CLIENT)) {
                            getByName("client") {
                                client()
                                configName = "Client"
                                ideConfigGenerated(true)
                                runDir("run")
                            }
                        }
                        if (modEnvironment.matches(Environment.SERVER)) {
                            getByName("server") {
                                server()
                                configName = "Server"
                                vmArg("-ea")
                                ideConfigGenerated(true)
                                runDir("run")
                            }
                        }
                        if (datagenEnabled) {
                            if (modEnvironment.matches(Environment.SERVER)) {
                                create("datagen") {
                                    server()
                                    configName = "Datagen"
                                    vmArgs(
                                        "-Dfabric-api.datagen",
                                        "-Dfabric-api.datagen.output-dir=${file("src/main/generated")}",
                                        "-Dfabric-api.datagen.strict-validation"
                                    )
                                    ideConfigGenerated(true)
                                    runDir("build/datagen")
                                }
                            }
                            if (modEnvironment.matches(Environment.CLIENT)) {
                                create("datagenClient") {
                                    client()
                                    configName = "Datagen Client"
                                    vmArgs(
                                        "-Dfabric-api.datagen",
                                        "-Dfabric-api.datagen.output-dir=${file("src/main/generated")}",
                                        "-Dfabric-api.datagen.strict-validation"
                                    )
                                    ideConfigGenerated(true)
                                    runDir("build/datagen")
                                }
                            }
                        }
                    }
                }

                if (datagenEnabled) sourceSets["main"].resources.srcDir("src/main/generated")

                dependencies {
                    "minecraft"("com.mojang:minecraft:${minecraftVersion}")
                    "mappings"(
                        this@SubProject.extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class)
                            .officialMojangMappings()
                    )
                    "modImplementation"("net.fabricmc:fabric-loader:${fabricLoader}")
                    "compileOnly"(project(":${this@ModProject.name}:Common", "namedElements"))

                    if (fabricModules.size == 1 && fabricModules[0] == "*") {
                        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${fabricApix}")
                    } else {
                        if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) {
                            val apiExt =
                                this@SubProject.extensions.getByType(net.fabricmc.loom.configuration.FabricApiExtension::class)
                            for (module in fabricModules) {
                                "modCompileOnly"(
                                    "net.fabricmc.fabric-api:${module}:${apiExt.moduleVersion(module, fabricApix).toString()}"
                                ) {
                                    isTransitive = true
                                    exclude(module = "fabric-loader")
                                }
                            }
                            "modRuntimeOnly"("net.fabricmc.fabric-api:fabric-api:${fabricApix}")
                        }
                    }
                    "implementation"("com.google.code.findbugs:jsr305:3.0.1")
                }

                tasks.withType(JavaCompile::class) {
                    source(commonMainSourceSet.allSource)
                }

                tasks.withType(ProcessResources::class) {
                    from(commonMainSourceSet.resources)
                    filesMatching("fabric.mod.json") {
                        expand(
                            "mod_id" to modId,
                            "mod_name" to modName,
                            "mod_version" to modVersion,
                            "mod_description" to modDescription,
                            "mc_major_minor" to minecraftVersionMajorMinor,
                            "project_name" to this@ModProject.name,
                            "java_version" to javaVersion
                        )
                    }

                    doLast {
                        val modJson = outputs.files.singleFile.resolve("fabric.mod.json")
                        if (modJson.exists()) {
                            val json = parseJson(modJson)
                            var file1: File
                            if (!json.containsKey("mixins")) {
                                val mixins = ArrayList<String>()
                                file1 = modJson.resolveSibling("${modId}.mixins.json")
                                if (file1.exists()) mixins.add(file1.name)
                                file1 = modJson.resolveSibling("${modId}.client.mixins.json")
                                if (file1.exists()) mixins.add(file1.name)
                                file1 = modJson.resolveSibling("${modId}.server.mixins.json")
                                if (file1.exists()) mixins.add(file1.name)
                                json["mixins"] = mixins
                            }
                            if (!json.containsKey("accessWidener")) {
                                file1 = modJson.resolveSibling("${modId}.accesswidener")
                                if (file1.exists()) json["accessWidener"] = file1.name
                            }

                            if (!json.containsKey("contact")) {
                                val contact = HashMap<String, Any>();
                                contact["homepage"] =
                                    "https://github.com/marcus8448/MinecraftMods/tree/${minecraftVersionMajorMinor}/${this@ModProject.name}"
                                contact["sources"] = "https://github.com/marcus8448/MinecraftMods/tree/"
                                json["contact"] = contact;
                            }

                            if (!json.containsKey("icon")) {
                                file1 = modJson.resolveSibling("${modId}.png")
                                if (file1.exists()) json["icon"] = file1.name
                            }

                            if (!json.containsKey("license")) json["license"] = "LGPL-3.0-only"
                            if (!json.containsKey("environment")) json["environment"] = modEnvironment.jsonName
                            if (!json.containsKey("authors")) json["authors"] = modAuthors

                            val depends = json.obj("depends")
                            if (fabricModules.isEmpty() || fabricModules[0] == "*") {
                                depends["fabric"] = "*"
                            } else if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) {
                                fabricModules.forEach { depends[it] = "*" }
                            }

                            modJson.writeText(groovy.json.JsonOutput.toJson(json))
                        }
                    }
                }

                configure<PublishingExtension> {
                    publications {
                        register("mavenJava", MavenPublication::class) {
                            groupId = this@SubProject.group.toString()
                            artifactId =
                                this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                            version = this@SubProject.version.toString()
                            from(components["java"])
                            addPomInfo(modName, modDescription, modAuthors)
                        }
                    }

                    repositories {
                        mavenLocal()
                    }
                }

                if (modrinthId.isNotBlank() && System.getenv().containsKey("MODRINTH_TOKEN")) {
                    apply(plugin = "com.modrinth.minotaur")

                    configureModrinth(tasks.getByName("remapJar"), deps = {
                        if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) it.project("P7dR8mSH") // Fabric Api
                    })
                }

                if (curseforgeId.isNotBlank() && System.getenv().containsKey("CURSEFORGE_TOKEN")) {
                    apply(plugin = "net.darkhax.curseforgegradle")

                    configureCurseforge(tasks.getByName("remapJar"), deps = {
                        if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) it.addRequirement("fabric-api")
                    })
                }

                tasks.register("prepareWorkspace")
            }
            "Forge" -> {
                apply(plugin = "net.neoforged.gradle.userdev")

                dependencies {
                    "implementation"("net.neoforged:neoforge:${forgeVersion}")
                    "compileOnly"(project(":${this@ModProject.name}:Common", "namedElements"))
                    "implementation"("com.google.code.findbugs:jsr305:3.0.1")
                }

                tasks.getByName<Test>("test") {
                    enabled = false
                }

                (extensions["minecraft"] as net.neoforged.gradle.common.extensions.MinecraftExtension).apply {
                    if (file("src/main/resources/META-INF/accesstransformer.cfg").exists()) {
                        accessTransformers.entry(file("src/main/resources/META-INF/accesstransformer.cfg").toString())
                        project.logger.debug("Loading access transformer for ${this@ModProject.name}.")
                    }
                }

                (extensions["runs"] as NamedDomainObjectContainer<net.neoforged.gradle.dsl.common.runs.run.Run>).apply {
                    if (modEnvironment.matches(Environment.CLIENT)) {
                        getByName("client") {
                            workingDirectory(project.file("run"))

                            modSources.add(sourceSets["main"])
//                            modSources.add(commonMainSourceSet)
                        }
                    }

                    if (modEnvironment.matches(Environment.SERVER)) {
                        getByName("server") {
                            workingDirectory(project.file("run"))

                            modSources.add(sourceSets["main"])
//                            modSources.add(commonMainSourceSet)
                        }
                    }

                    getByName("data") {
                        workingDirectory(project.file("run"))

                        programArguments(
                            "--mod",
                            modId,
                            "--all",
                            "--output",
                            file("src/main/generated/").absolutePath,
                            "--existing",
                            file("src/main/resources/").absolutePath
                        )

                        modSources.add(sourceSets["main"])
//                        modSources.add(commonMainSourceSet)
                    }

                }

                sourceSets["main"].resources.srcDir("src/main/generated")

                tasks.withType(JavaCompile::class) {
                    source(commonMainSourceSet.allSource)
                }

                tasks.withType(ProcessResources::class) {
                    from(commonMainSourceSet.resources)
                    filesMatching("META-INF/neoforge.mods.toml") {
                        expand(
                            "mod_id" to modId,
                            "mod_name" to modName,
                            "mod_version" to modVersion,
                            "mod_description" to modDescription,
                            "mod_authors" to if (modAuthors.size == 1) {
                                modAuthors[0]
                            } else {
                                var s = "";
                                for ((index, author) in modAuthors.withIndex()) {
                                    if (index != 0) {
                                        s += ", "
                                    }
                                    s += author
                                }
                                s
                            },
                            "fml_major" to fmlVersion.split(".")[0],
                            "forge_major" to forgeVersion.split(".")[0],
                            "mc_major_minor" to minecraftVersionMajorMinor
                        )
                    }
                }

                configure<PublishingExtension> {
                    publications {
                        register("mavenJava", MavenPublication::class) {
                            groupId = this@SubProject.group.toString()
                            artifactId =
                                this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                            version = this@SubProject.version.toString()
                            artifact(tasks["jar"])
                            addPomInfo(modName, modDescription, modAuthors)
                        }
                    }

                    repositories {
                        mavenLocal()
                    }
                }

                if (modrinthId.isNotBlank() && System.getenv().containsKey("MODRINTH_TOKEN")) {
                    apply(plugin = "com.modrinth.minotaur")
                    configureModrinth(tasks.getByName("jar"), deps = {})
                }

                if (curseforgeId.isNotBlank() && System.getenv().containsKey("CURSEFORGE_TOKEN")) {
                    apply(plugin = "net.darkhax.curseforgegradle")
                    configureCurseforge(tasks.getByName("jar"), deps = {})
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

fun MavenPublication.addPomInfo(modName: String, modDescription: String, modAuthors: List<String>) {
    pom {
        name.set(modName)
        description.set(modDescription)

        organization {
            name.set("marcus8448")
            url.set("https://github.com/marcus8448")
        }

        developers {
            modAuthors.forEach() {
                developer {
                    id.set(it)
                    name.set(it)
                }
            }
        }

        scm {
            url.set("https://github.com/marcus8448/MinecraftMods")
            connection.set("scm:git:git://github.com/marcus8448/MinecraftMods.git")
            developerConnection.set("scm:git:git@github.com:marcus8448/MinecraftMods.git")
        }

        issueManagement {
            system.set("github")
            url.set("https://github.com/marcus8448/MinecraftMods/issues")
        }

        ciManagement {
            system.set("github-actions")
            url.set("https://github.com/marcus8448/MinecraftMods/actions")
        }

        licenses {
            license {
                name.set("LGPL-3.0-only")
                url.set("https://github.com/marcus8448/MinecraftMods/blob/${minecraftVersionMajorMinor}/LICENSE")
            }
        }
    }
}

fun parseJson(file: File): MutableMap<String, Any> {
    return groovy.json.JsonSlurper().parse(file) as MutableMap<String, Any>
}

fun MutableMap<String, Any>.obj(key: String): MutableMap<String, Any> {
    return this[key] as MutableMap<String, Any>
}

fun MutableMap<String, Any>.str(key: String): String {
    return this[key] as String
}
