@Suppress("SpellCheckingInspection")
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
                includeGroup("org.apache.logging")
                includeGroup("org.apache.logging.log4j")
                includeGroup("org.apache.maven")
                includeGroup("org.cadixdev")
                includeGroup("org.checkerframework")
                includeGroup("org.codehaus.plexus")
                includeGroup("org.immutables")
                includeGroup("org.jetbrains")
                includeGroup("org.jetbrains.kotlinx")
                includeGroup("org.junit")
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

enum class Environment(val jsonName: String) {
    BOTH("*"), CLIENT("client"), SERVER("server");

    fun matches(other: Environment): Boolean {
        return this == BOTH || this == other
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

fun Project.propertyOrDefault(key: String, default: String): String {
    return this.findProperty(key)?.toString() ?: default
}

fun Project.propertyOrBlank(key: String): String {
    return this.findProperty(key)?.toString() ?: ""
}

fun Project.propertyStr(key: String): String {
    return this.propertyOrDefault(key, "")
}

val javaVersion = Integer.parseInt(project.propertyStr("java.version"))
val minecraftVersion = project.propertyStr("minecraft.version")
val minecraftVersionMajorMinor = minecraftVersion.split(".")[0] + "." + minecraftVersion.split(".")[1]
val fabricLoader = project.propertyStr("fabric.loader")
val fabricApi = project.propertyStr("fabric.api")
val forgeVersion = project.propertyStr("forge")
val currentYear = Integer.parseInt(project.propertyStr("year"))

subprojects ModProject@ {
    val modName = project.propertyStr("mod.name")
    val modId = project.propertyStr("mod.id")
    val modGroup = project.propertyStr("mod.group")
    val modAuthors = project.propertyStr("mod.authors").split(",")
    val modVersion = project.propertyStr("mod.version")
    val modDescription = project.propertyStr("mod.description")
    val modStartYear = Integer.parseInt(project.propertyStr("mod.start_year"))
    val modEnvironment = Environment.valueOf(project.propertyOrDefault("mod.environment", "both").toUpperCase())
    val multiplatform = project.propertyOrDefault("mod.multiplatform", "true").toBoolean()
    val library = project.propertyOrDefault("mod.library", "false").toBoolean()
    val discord = project.propertyOrBlank("mod.discord")

    val decoratedModVersion = "${modVersion}+${minecraftVersion}"

    version = decoratedModVersion
    group = modGroup

    if (multiplatform) {
        subprojects SubProject@{
            apply(plugin = "java")
            apply(plugin = "maven-publish")
            apply(plugin = "org.cadixdev.licenser")

            val sourceSets = this@SubProject.extensions.getByType(JavaPluginExtension::class).sourceSets
            val commonMainSourceSet =
                this@ModProject.project("Common").extensions.getByType(JavaPluginExtension::class).sourceSets["main"]
            val loaderName = this@SubProject.name
            val loaderId = loaderName.toLowerCase()

            val modrinthId = this@ModProject.findProperty("modrinth.id.${loaderId}")?.toString()
                ?: this@ModProject.propertyOrBlank("modrinth.id")
            val curseforgeId = this@ModProject.findProperty("curseforge.id.${loaderId}")?.toString()
                ?: this@ModProject.propertyOrBlank("curseforge.id")

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
                archivesName.set("${modName}-${name.toLowerCase()}")
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
                        "Implementation-Timestamp" to java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ISO_DATE_TIME),
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
                    val fabricModJson = parseJson(file("src/main/resources/fabric.mod.json")) // NOT EXPANDED
                    val datagenEnabled = fabricModJson.obj("entrypoints").containsKey("fabric-datagen")

                    configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
                        if (project.file("src/main/resources/${modId}.accesswidener").exists()) {
                            accessWidenerPath.set(project.file("src/main/resources/${modId}.accesswidener"))
                        }

                        shareCaches()

//                        mods {
//                            register(modId) {
//                                sourceSet(sourceSets["main"])
//                                sourceSet(commonMainSourceSet)
//                            }
//                        }

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
                        "implementation"("com.google.code.findbugs:jsr305:3.0.1")
                        "implementation"(this@ModProject.project("Common"))

                        if (fabricModules.size == 1 && fabricModules[0] == "*") {
                            "modImplementation"("net.fabricmc.fabric-api:fabric-api:${fabricApi}")
                        } else {
                            if (fabricModules.isNotEmpty() && fabricModules[0].isNotBlank()) {
                                val apiExt =
                                    this@SubProject.extensions.getByType(net.fabricmc.loom.configuration.FabricApiExtension::class)
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
                                "modRuntimeOnly"("net.fabricmc.fabric-api:fabric-api:${fabricApi}")
                            }
                        }
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
                                    contact["homepage"] = "https://github.com/marcus8448/MinecraftMods/tree/${minecraftVersionMajorMinor}/${this@ModProject.name}"
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

                                if (library || discord.isNotBlank()) {
                                    val modmenu = (json.getOrPut("custom", defaultValue = {
                                        HashMap<String, Any>()
                                    }) as HashMap<String, Any>).getOrPut("modmenu", defaultValue = {
                                        HashMap<String, Any>()
                                    }) as HashMap<String, Any>
                                    if (library) modmenu["badges"] = arrayOf("library")
                                    if (discord.isNotBlank()) {
                                        val links = HashMap<String, Any>(1)
                                        modmenu["links"] = links
                                        links["modmenu.discord"] = "https://discord.gg/${discord}"
                                    }
                                }

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
                            if (modEnvironment.matches(Environment.CLIENT)) {
                                create("client") {
                                    workingDirectory(project.file("run"))
                                    ideaModule("${rootProject.name}.${this@ModProject.name}.${project.name}.main")
                                    property("mixin.env.remapRefMap", "true")
                                    property(
                                        "mixin.env.refMapRemappingFile",
                                        "${projectDir}/build/createSrgToMcp/output.srg"
                                    )
                                    mods {
                                        create(modId) {
                                            source(sourceSets["main"])
                                            source(commonMainSourceSet)
                                        }
                                    }
                                }
                            }

                            if (modEnvironment.matches(Environment.SERVER)) {
                                create("server") {
                                    workingDirectory(project.file("run"))
                                    ideaModule("${rootProject.name}.${this@ModProject.name}.${project.name}.main")
                                    property("mixin.env.remapRefMap", "true")
                                    property(
                                        "mixin.env.refMapRemappingFile",
                                        "${projectDir}/build/createSrgToMcp/output.srg"
                                    )
                                    mods {
                                        create(modId) {
                                            source(sourceSets["main"])
                                            source(commonMainSourceSet)
                                        }
                                    }
                                }
                            }

                            create("data") {
                                workingDirectory(project.file("run"))
                                ideaModule("${rootProject.name}.${this@ModProject.name}.${project.name}.main")
                                args(
                                    "--mod",
                                    modId,
                                    "--all",
                                    "--output",
                                    file("src/main/generated/"),
                                    "--existing",
                                    file("src/main/resources/")
                                )
                                property("mixin.env.remapRefMap", "true")
                                property(
                                    "mixin.env.refMapRemappingFile",
                                    "${projectDir}/build/createSrgToMcp/output.srg"
                                )
                                mods {
                                    create(modId) {
                                        source(sourceSets["main"])
                                        source(commonMainSourceSet)
                                    }
                                }
                            }
                        }
                    }
                    sourceSets["main"].resources.srcDir("src/main/generated")

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
                                "forge_major" to forgeVersion.split(".")[0],
                                "mc_major_minor" to minecraftVersionMajorMinor
                            )
                        }
                    }

                    tasks["jar"].finalizedBy("reobfJar")

                    configure<PublishingExtension> {
                        publications {
                            register("mavenJava", MavenPublication::class) {
                                groupId = this@SubProject.group.toString()
                                artifactId =
                                    this@SubProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                                version = this@SubProject.version.toString()
                                artifact(tasks["jar"])
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
    } else {
        apply(plugin = "org.spongepowered.gradle.vanilla")

        version = decoratedModVersion

        configure<BasePluginExtension> {
            archivesName.set(modName)
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
                    "Implementation-Timestamp" to java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME),
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

        configure<org.spongepowered.gradle.vanilla.MinecraftExtension> {
            version(minecraftVersion)
        }

        dependencies {
            "compileOnly"("org.spongepowered:mixin:0.8.5")
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
                    groupId = this@ModProject.group.toString()
                    artifactId = this@ModProject.extensions.getByType(BasePluginExtension::class).archivesName.get()
                    version = this@ModProject.version.toString()
                    from(components["java"])
                }
            }

            repositories {
                mavenLocal()
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
