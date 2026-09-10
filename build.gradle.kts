import org.gradle.api.plugins.quality.Checkstyle

plugins {
    id("java")
    id("checkstyle")
}

allprojects {
    group = "com.samo"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxErrors = 0
}

val phaseZeroSpikeDir = rootProject.file("src/main/java/com/samo/spike")
val phaseZeroSpikeSources = fileTree(phaseZeroSpikeDir) {
    include("**/*.java")
}
val checkstyleMainSources = fileTree(rootDir) {
    include("src/main/java/**/*.java")
    include("*/src/main/java/**/*.java")
    exclude("src/main/java/com/samo/spike/**/*.java")
}
val checkstyleTestSources = fileTree(rootDir) {
    include("src/test/java/**/*.java")
    include("*/src/test/java/**/*.java")
}
val includeInvalidCheckstyleFixture = providers.gradleProperty("checkstyleIncludeInvalidFixture")
    .map { it.toBoolean() }
    .orElse(false)

tasks.named<Checkstyle>("checkstyleMain") {
    setSource(checkstyleMainSources)
    if (includeInvalidCheckstyleFixture.get()) {
        source(rootProject.file("config/checkstyle/fixtures/InvalidCheckstyleFixture.java"))
    }
    classpath = files()
}

tasks.named<Checkstyle>("checkstyleTest") {
    setSource(checkstyleTestSources)
    classpath = files()
}

val verifyCheckstyleSourceBoundary by tasks.registering {
    group = "verification"
    description = "Verifies that experimental Phase 0 spike sources remain outside the production Checkstyle scan."
    inputs.files(checkstyleMainSources)
    inputs.files(phaseZeroSpikeSources)

    doLast {
        check(phaseZeroSpikeSources.files.isNotEmpty()) {
            "Phase 0 spike source boundary is empty; revisit the deliberate Checkstyle exclusion."
        }

        val spikeRoot = phaseZeroSpikeDir.toPath().toAbsolutePath().normalize()
        val leakedSources = checkstyleMainSources.files
            .filter { it.toPath().toAbsolutePath().normalize().startsWith(spikeRoot) }
            .sortedBy { it.path }

        check(leakedSources.isEmpty()) {
            "Experimental Phase 0 sources leaked into the production Checkstyle scan: " +
                leakedSources.joinToString { it.relativeTo(rootDir).path }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyCheckstyleSourceBoundary)
}

val engineTestModules = listOf(
    "engine-core",
    "engine-platform-lwjgl",
    "engine-render-opengl",
    "engine-ui",
    "engine-assets",
    "engine-world",
    "engine-physics-jolt",
    "engine-audio-openal",
    "engine-network-api",
    "engine-network-ip",
    "engine-steam",
    "engine-editor"
)

engineTestModules.forEach { moduleName ->
    project(":$moduleName") {
        pluginManager.withPlugin("java") {
            dependencies.add("testImplementation", project(":test-support"))
        }
    }
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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("buildAllModules") {
    group = "build"
    description = "Runs the root quality gate and compiles/tests every declared engine/game module."
    dependsOn(tasks.named("check"))
    dependsOn(subprojects.map { "${it.path}:build" })
}

val projectsForLocking = rootProject.allprojects

tasks.register("resolveAndLockAllDependencies") {
    group = "build setup"
    description = "Resolves all resolvable configurations so dependency lock state can be written with --write-locks."

    doLast {
        projectsForLocking.forEach { project ->
            project.configurations
                .filter { it.isCanBeResolved }
                .sortedBy { it.name }
                .forEach { configuration ->
                    logger.lifecycle("Resolving ${project.path}:${configuration.name}")
                    configuration.resolve()
                }
        }
    }
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

val nativeEvidenceDir = layout.buildDirectory.dir("spikes/native-evidence")

fun JavaExec.configureIntegratedNativeEvidence(
    evidenceTask: String,
    defaultDurationSeconds: String,
    recordingName: String
) {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.samo.spike.integration.IntegratedNativeSoakSpike"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
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
