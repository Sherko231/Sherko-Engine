import java.util.Properties

plugins {
    java
}

dependencies {
    implementation(project(":game-sandbox"))
    implementation(project(":engine-platform-lwjgl"))
    implementation(project(":engine-render-opengl"))
    implementation(project(":engine-audio-openal"))
    implementation(project(":engine-network-ip"))
    implementation(project(":engine-steam"))
}

val compatibilityVersions = Properties().apply {
    rootProject.file("config/version.properties").inputStream().use { load(it) }
}
val engineCommit = providers.exec {
    workingDir(rootDir)
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }
val runtimeClasspath = configurations.runtimeClasspath.get()
val generatedVersionResources = layout.buildDirectory.dir("generated/version-resources")

val generateVersionMetadata by tasks.registering {
    group = "build setup"
    description = "Generates reproducible version metadata for game-client."
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

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Runs the game-client foundation entry point."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.game.client.ClientMain"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
