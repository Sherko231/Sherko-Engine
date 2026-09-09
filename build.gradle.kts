plugins {
    id("java")
}

group = "com.samo"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.4.3"
val joltJniVersion = "6.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")

    implementation("com.github.stephengold:jolt-jni-Windows64:$joltJniVersion")
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:$joltJniVersion:DebugSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
    runtimeOnly("com.github.oshi:oshi-core:7.4.2")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runOpenGL46Spike") {
    group = "verification"
    description = "Runs the P0-T03 LWJGL/GLFW/OpenGL 4.6 feasibility spike."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.opengl.OpenGL46Spike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    systemProperty(
        "spike.durationSeconds",
        providers.gradleProperty("spikeDurationSeconds").orElse("600").get()
    )
}

tasks.register<JavaExec>("runJoltLifecycleSpike") {
    group = "verification"
    description = "Runs the P0-T04 Jolt JNI lifecycle/cleanup feasibility spike."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.physics.JoltLifecycleSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    systemProperty(
        "spike.cycles",
        providers.gradleProperty("joltSpikeCycles").orElse("4").get()
    )
}

tasks.register<JavaExec>("runOpenAL3DAudioSpike") {
    group = "verification"
    description = "Runs the P0-T05 OpenAL 3D audio lifecycle feasibility spike."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.audio.OpenAL3DAudioSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty(
        "spike.durationSeconds",
        providers.gradleProperty("audioSpikeDurationSeconds").orElse("10").get()
    )
}

fun JavaExec.configureUdpSpike(role: String) {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.network.LocalhostUdpSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    args(role)
    systemProperty(
        "spike.udpPort",
        providers.gradleProperty("udpSpikePort").orElse("42060").get()
    )
    systemProperty(
        "spike.packetCount",
        providers.gradleProperty("udpSpikePacketCount").orElse("8").get()
    )
    systemProperty(
        "spike.timeoutMillis",
        providers.gradleProperty("udpSpikeTimeoutMillis").orElse("10000").get()
    )
}

tasks.register<JavaExec>("runUdpSpikeServer") {
    description = "Runs the P0-T06 localhost UDP echo server in its own JVM."
    configureUdpSpike("server")
}

tasks.register<JavaExec>("runUdpSpikeClient") {
    description = "Runs the P0-T06 localhost UDP RTT client in its own JVM."
    configureUdpSpike("client")
}
