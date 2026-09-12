plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-platform-lwjgl"))
    implementation(project(":engine-world"))
    implementation(project(":engine-physics-jolt"))
    implementation(project(":engine-network-api"))
    implementation(project(":engine-ui"))
}

tasks.register<JavaExec>("runEngineDemo") {
    group = "application"
    description = "Runs the owner-facing Sherko Engine sandbox demo."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.game.sandbox.demo.EngineDemoMain"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
