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

    val runtimeClasspath = configurations.runtimeClasspath.get()
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
