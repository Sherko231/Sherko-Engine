plugins {
    id("java")
}

group = "com.samo"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.4.3"
val joltJniVersion = "6.0.0"
val steamworks4jVersion = "1.10.0"

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

    implementation("com.code-disaster.steamworks4j:steamworks4j-lwjgl3:$steamworks4jVersion")

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

fun JavaExec.configureNetworkImpairmentHarness(mode: String) {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.network.NetworkImpairmentHarness"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    args(mode)
    systemProperty(
        "spike.impairmentPort",
        providers.gradleProperty("impairmentPort").orElse("42100").get()
    )
    systemProperty(
        "spike.impairmentPacketCount",
        providers.gradleProperty("impairmentPacketCount").orElse("8").get()
    )
    systemProperty(
        "spike.impairmentTimeoutMillis",
        providers.gradleProperty("impairmentTimeoutMillis").orElse("10000").get()
    )
}

tasks.register<JavaExec>("runNetworkImpairmentHarness") {
    description = "Runs all P0-T11 localhost network impairment modes."
    configureNetworkImpairmentHarness("all")
}

listOf("latency", "jitter", "loss", "duplication", "reordering").forEach { mode ->
    val taskSuffix = mode.replaceFirstChar { it.uppercase() }
    tasks.register<JavaExec>("runNetworkImpairment$taskSuffix") {
        description = "Runs the P0-T11 $mode-only localhost network impairment check."
        configureNetworkImpairmentHarness(mode)
    }
}

val steamSpikeWorkingDir = layout.buildDirectory.dir("spikes/steam")
val prepareSteamSpike by tasks.registering {
    val appIdFile = steamSpikeWorkingDir.map { it.file("steam_appid.txt") }
    outputs.file(appIdFile)
    doLast {
        val file = appIdFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText("480\n")
    }
}

tasks.register<JavaExec>("runSteamInitSpike") {
    group = "verification"
    description = "Runs the P0-T07 Steam initialization/callback feasibility spike."
    dependsOn(prepareSteamSpike)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.steam.SteamInitSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    workingDir(steamSpikeWorkingDir.get().asFile)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty(
        "spike.timeoutSeconds",
        providers.gradleProperty("steamSpikeTimeoutSeconds").orElse("10").get()
    )
}

tasks.register<JavaExec>("runSteamFlatApiFfmSpike") {
    group = "verification"
    description = "Runs the P0-T09 Java 25 FFM -> Steam flat API feasibility spike."
    dependsOn(prepareSteamSpike)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.steam.SteamFlatApiFfmSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    workingDir(steamSpikeWorkingDir.get().asFile)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    providers.gradleProperty("steamApi64Path").orNull?.let {
        systemProperty("spike.steamApi64Path", it)
    }
}

val nativeSoakDir = layout.buildDirectory.dir("spikes/native-soak")

tasks.register<JavaExec>("runIntegratedNativeSoak") {
    group = "verification"
    description = "Runs the P0-T12 15-second GLFW/OpenGL/Jolt/OpenAL/UDP soak under JFR."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.integration.IntegratedNativeSoakSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-XX:StartFlightRecording=filename=${nativeSoakDir.get().file("p0-t12.jfr").asFile.absolutePath},settings=profile,dumponexit=true"
    )
    systemProperty(
        "spike.durationSeconds",
        providers.gradleProperty("nativeSoakDurationSeconds").orElse("15").get()
    )
    doFirst {
        nativeSoakDir.get().asFile.mkdirs()
    }
}
