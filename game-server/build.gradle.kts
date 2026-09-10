import java.util.Properties

plugins {
    java
}

dependencies {
    implementation(project(":game-sandbox"))
    implementation(project(":engine-network-ip"))
    implementation(project(":engine-steam"))
}

val forbiddenHeadlessProjects = setOf(
    "engine-platform-lwjgl",
    "engine-render-opengl",
    "engine-audio-openal"
)
val forbiddenHeadlessArtifacts = listOf(
    "lwjgl-glfw",
    "lwjgl-opengl",
    "lwjgl-openal"
)

val compatibilityVersions = Properties().apply {
    rootProject.file("config/version.properties").inputStream().use(::load)
}
val engineCommit = providers.exec {
    workingDir(rootDir)
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }
val runtimeClasspath = configurations.runtimeClasspath.get()
val generatedVersionResources = layout.buildDirectory.dir("generated/version-resources")

val generateVersionMetadata by tasks.registering {
    group = "build setup"
    description = "Generates reproducible version metadata for game-server."
    inputs.file(rootProject.file("config/version.properties"))
    inputs.property("engineCommit", engineCommit)
    inputs.files(runtimeClasspath)
    outputs.dir(generatedVersionResources)

    doLast {
        val nativeLibraries = runtimeClasspath.resolvedConfiguration.resolvedArtifacts
            .map { it.moduleVersion.id }
            .filter { id ->
                id.group == "org.lwjgl" ||
                    id.name.contains("jolt-jni", ignoreCase = true) ||
                    id.name.contains("steamworks4j", ignoreCase = true)
            }
            .map { "${it.group}:${it.name}:${it.version}" }
            .distinct()
            .sorted()

        val output = generatedVersionResources.get().file("META-INF/sherko-version.properties").asFile
        output.parentFile.mkdirs()
        output.writeText(
            buildString {
                appendLine("engineCommit=${engineCommit.get()}")
                appendLine("protocolVersion=${compatibilityVersions.getProperty("protocolVersion")}")
                appendLine("assetVersion=${compatibilityVersions.getProperty("assetVersion")}")
                appendLine("nativeLibraries=${nativeLibraries.joinToString(",").ifEmpty { "none" }}")
            }
        )
    }
}

sourceSets.main {
    resources.srcDir(generatedVersionResources)
}

tasks.named("processResources") {
    dependsOn(generateVersionMetadata)
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Runs the headless game-server foundation entry point."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.game.server.ServerMain"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val verifyHeadlessServerRuntime by tasks.registering {
    group = "verification"
    description = "Verifies the game-server runtime excludes window, renderer, and audio dependencies."

    inputs.files(runtimeClasspath)

    doLast {
        val forbiddenProjects = runtimeClasspath.incoming.resolutionResult.allComponents
            .mapNotNull { it.id as? org.gradle.api.artifacts.component.ProjectComponentIdentifier }
            .map { it.projectName }
            .filter { it in forbiddenHeadlessProjects }
            .sorted()

        check(forbiddenProjects.isEmpty()) {
            "Headless server runtime contains forbidden engine projects: ${forbiddenProjects.joinToString()}"
        }

        val forbiddenArtifacts = runtimeClasspath.files
            .map { it.name.lowercase() }
            .filter { artifactName ->
                forbiddenHeadlessArtifacts.any { forbidden -> artifactName.contains(forbidden) }
            }
            .sorted()

        check(forbiddenArtifacts.isEmpty()) {
            "Headless server runtime contains forbidden graphics/audio artifacts: ${forbiddenArtifacts.joinToString()}"
        }
    }
}

tasks.named("check") {
    dependsOn(verifyHeadlessServerRuntime)
}
