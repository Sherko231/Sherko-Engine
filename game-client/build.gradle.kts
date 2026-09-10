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

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Runs the game-client foundation entry point."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.game.client.ClientMain"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
