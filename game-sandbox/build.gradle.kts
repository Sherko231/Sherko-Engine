plugins {
    `java-library`
}

val sandboxRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(project(":engine-core"))
    compileOnly(project(":engine-platform-lwjgl"))
    compileOnly(project(":engine-render-opengl"))
    sandboxRuntime(project(":engine-platform-lwjgl"))
    sandboxRuntime(project(":engine-render-opengl"))
    implementation(project(":engine-world"))
    implementation(project(":engine-physics-jolt"))
    implementation(project(":engine-network-api"))
    implementation(project(":engine-ui"))
    testImplementation(project(":test-support"))
}

fun JavaExec.configureSandboxRun(mainClassName: String) {
    classpath = sourceSets.main.get().runtimeClasspath + sandboxRuntime
    mainClass = mainClassName
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.register<JavaExec>("runSandbox") {
    group = "application"
    description = "Runs the persistent owner-facing Sherko Engine sandbox playground."
    configureSandboxRun("com.samo.game.sandbox.SandboxMain")
}

tasks.register<JavaExec>("runEngineDemo") {
    group = "application"
    description = "Legacy alias for runSandbox."
    configureSandboxRun("com.samo.game.sandbox.demo.EngineDemoMain")
}
