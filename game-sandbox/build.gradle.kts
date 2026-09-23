plugins {
    `java-library`
}

val sandboxRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-assets"))
    compileOnly(project(":engine-platform-lwjgl"))
    compileOnly(project(path = ":engine-render-opengl", configuration = "runtimeElements")) {
        isTransitive = false
    }
    sandboxRuntime(project(":engine-platform-lwjgl"))
    sandboxRuntime(project(":engine-render-opengl"))
    implementation(project(":engine-world"))
    implementation(project(":engine-physics-jolt"))
    implementation(project(":engine-network-api"))
    implementation(project(":engine-ui"))
    testImplementation(project(":test-support"))
    testCompileOnly(project(":engine-render-opengl"))
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
