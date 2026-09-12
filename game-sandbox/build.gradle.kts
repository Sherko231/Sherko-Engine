plugins {
    `java-library`
}

val engineDemoRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(project(":engine-core"))
    compileOnly(project(":engine-platform-lwjgl"))
    engineDemoRuntime(project(":engine-platform-lwjgl"))
    implementation(project(":engine-world"))
    implementation(project(":engine-physics-jolt"))
    implementation(project(":engine-network-api"))
    implementation(project(":engine-ui"))
    testImplementation(project(":test-support"))
}

tasks.register<JavaExec>("runEngineDemo") {
    group = "application"
    description = "Runs the owner-facing Sherko Engine sandbox demo."
    classpath = sourceSets.main.get().runtimeClasspath + engineDemoRuntime
    mainClass = "com.samo.game.sandbox.demo.EngineDemoMain"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
