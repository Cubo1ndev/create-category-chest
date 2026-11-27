plugins {
    id("net.neoforged.moddev") version "2.0.148"
}

version = "${property("mod.version")}+${property("minecraft")}"
base.archivesName = "${property("mod.id")}-neoforge"

val createJar = file("../create-mod-pack/.cache/downloads/create-1.21.1-6.0.10.jar")

neoForge {
    version = property("neo") as String
    mods { register(property("mod.id") as String) { sourceSet(sourceSets.main.get()) } }
    runs {
        register("server") { server(); gameDirectory = file("run/server") }
        register("client") {
            client(); gameDirectory = file("run/client")
            programArguments.addAll("--quickPlayMultiplayer", "localhost:25599") // the dev server (run/server)
        }
    }
}

// Create's bundled libraries (Ponder, Flywheel, Registrate) appear in its class signatures.
val createLibs = tasks.register<Copy>("createLibs") {
    from(zipTree(createJar)) { include("META-INF/jarjar/*.jar"); eachFile { path = name } }
    into(layout.buildDirectory.dir("createLibs"))
    includeEmptyDirs = false
}
dependencies {
    compileOnly(files(createJar))
    compileOnly(files(createLibs.map { fileTree(it.destinationDir) }).builtBy(createLibs))
}

java { toolchain.languageVersion = JavaLanguageVersion.of(21) }

tasks.processResources {
    val props = listOf("mod.id", "mod.name", "mod.version", "mod.description", "mod.license", "mod.authors", "minecraft")
        .associate { it.removePrefix("mod.") to project.property(it).toString() } + ("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

// Create is only compiled against; the dev runs load it from their mods folders.
val copyCreate = tasks.register<Copy>("copyCreate") {
    from(createJar)
    into("run/server/mods")
    doLast { copy { from(createJar); into("run/client/mods") } }
}
tasks.matching { it.name == "runServer" || it.name == "runClient" }.configureEach { dependsOn(copyCreate) }

// The index remap is checked by a main() self-check, not JUnit.
val remapCheck = tasks.register<JavaExec>("remapCheck") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.cubo1ndev.categorychest.Remap"
}
tasks.named("check") { dependsOn(remapCheck) }
