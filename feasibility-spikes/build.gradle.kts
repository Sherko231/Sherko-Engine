plugins {
    java
}

dependencies {
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.openal)
    implementation(libs.lwjgl.opengl)

    runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-openal:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-windows")

    implementation(libs.jolt.jni.windows64)
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:${libs.versions.jolt.jni.get()}:DebugSp")
    implementation(libs.snaploader)
    runtimeOnly(libs.oshi.core)

    implementation(libs.steamworks4j.lwjgl3)
}

fun JavaExec.useSpikeRuntime(mainClassName: String) {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = mainClassName
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.register<JavaExec>("runOpenGL46Spike") {
    description = "Runs the P0-T03 LWJGL/GLFW/OpenGL 4.6 feasibility spike."
    useSpikeRuntime("com.samo.spike.opengl.OpenGL46Spike")
    systemProperty(
        "spike.durationSeconds",
        providers.gradleProperty("spikeDurationSeconds").orElse("600").get()
    )
}

tasks.register<JavaExec>("runJoltLifecycleSpike") {
    description = "Runs the P0-T04 Jolt JNI lifecycle/cleanup feasibility spike."
    useSpikeRuntime("com.samo.spike.physics.JoltLifecycleSpike")
    systemProperty(
        "spike.cycles",
        providers.gradleProperty("joltSpikeCycles").orElse("4").get()
    )
}

tasks.register<JavaExec>("runOpenAL3DAudioSpike") {
    description = "Runs the P0-T05 OpenAL 3D audio lifecycle feasibility spike."
    useSpikeRuntime("com.samo.spike.audio.OpenAL3DAudioSpike")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty(
        "spike.durationSeconds",
        providers.gradleProperty("audioSpikeDurationSeconds").orElse("10").get()
    )
}

tasks.register<JavaExec>("runWindowsNativeCiSmoke") {
    description = "Runs the headless-safe GLFW/OpenAL native lifecycle smoke used by Windows hosted CI."
    useSpikeRuntime("com.samo.spike.ci.WindowsNativeCiSmoke")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun JavaExec.configureUdpSpike(role: String) {
    useSpikeRuntime("com.samo.spike.network.LocalhostUdpSpike")
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
    useSpikeRuntime("com.samo.spike.network.NetworkImpairmentHarness")
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

val steamSpikeWorkingDir = rootProject.layout.buildDirectory.dir("spikes/steam")
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
    description = "Runs the P0-T07 Steam initialization/callback feasibility spike."
    dependsOn(prepareSteamSpike)
    useSpikeRuntime("com.samo.spike.steam.SteamInitSpike")
    workingDir(steamSpikeWorkingDir.get().asFile)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty(
        "spike.timeoutSeconds",
        providers.gradleProperty("steamSpikeTimeoutSeconds").orElse("10").get()
    )
}

tasks.register<JavaExec>("runSteamFlatApiFfmSpike") {
    description = "Runs the P0-T09 Java 25 FFM -> Steam flat API feasibility spike."
    dependsOn(prepareSteamSpike)
    useSpikeRuntime("com.samo.spike.steam.SteamFlatApiFfmSpike")
    workingDir(steamSpikeWorkingDir.get().asFile)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    providers.gradleProperty("steamApi64Path").orNull?.let {
        systemProperty("spike.steamApi64Path", it)
    }
}

val nativeEvidenceDir = rootProject.layout.buildDirectory.dir("spikes/native-evidence")

fun JavaExec.configureIntegratedNativeEvidence(
    evidenceTask: String,
    defaultDurationSeconds: String,
    recordingName: String
) {
    useSpikeRuntime("com.samo.spike.integration.IntegratedNativeEvidenceHarness")
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-XX:StartFlightRecording=filename=${nativeEvidenceDir.get().file(recordingName).asFile.absolutePath},settings=profile,dumponexit=true"
    )
    systemProperty("spike.evidenceTask", evidenceTask)
    systemProperty(
        "spike.durationSeconds",
        providers.gradleProperty("nativeEvidenceDurationSeconds")
            .orElse(defaultDurationSeconds)
            .get()
    )
    doFirst {
        nativeEvidenceDir.get().asFile.mkdirs()
    }
}

tasks.register<JavaExec>("runIntegratedNativeSmoke") {
    description = "Runs the P0-T12 15-second GLFW/OpenGL/Jolt/OpenAL/UDP smoke test under JFR."
    configureIntegratedNativeEvidence("P0-T12", "15", "p0-t12-smoke.jfr")
}

tasks.register<JavaExec>("runIntegratedNativeSoak") {
    description = "Runs the P0-T13 15-minute GLFW/OpenGL/Jolt/OpenAL/UDP sustained test under JFR."
    configureIntegratedNativeEvidence("P0-T13", "900", "p0-t13-soak.jfr")
}


val verifyFeasibilitySpikeIsolation by tasks.registering {
    group = "verification"
    description = "Verifies that no other subproject depends on the experimental feasibility-spikes module."

    doLast {
        val dependentConfigurations = rootProject.subprojects
            .filter { candidate -> candidate.path != project.path }
            .flatMap { candidate ->
                candidate.configurations.flatMap { configuration ->
                    configuration.dependencies
                        .withType(org.gradle.api.artifacts.ProjectDependency::class.java)
                        .filter { dependency -> dependency.path == project.path }
                        .map { "${candidate.path}:${configuration.name}" }
                }
            }
            .sorted()

        check(dependentConfigurations.isEmpty()) {
            "Production/support/game projects must not depend on :feasibility-spikes. " +
                "Found: ${dependentConfigurations.joinToString()}"
        }
    }
}

tasks.named("check") {
    dependsOn(verifyFeasibilitySpikeIsolation)
}
