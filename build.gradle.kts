import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

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

val phaseZeroSpikeDir = rootProject.file("feasibility-spikes/src/main/java/com/samo/spike")
val phaseZeroSpikeSources = fileTree(phaseZeroSpikeDir) {
    include("**/*.java")
}
val checkstyleMainSources = fileTree(rootDir) {
    include("*/src/main/java/**/*.java")
    exclude("feasibility-spikes/src/main/java/**/*.java")
}
val checkstyleTestSources = fileTree(rootDir) {
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
    dependsOn(":test-support:test")
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
            pluginManager.apply("jacoco")

            extensions.configure<JacocoPluginExtension> {
                toolVersion = libs.versions.jacoco.get()
            }

            tasks.named<JacocoReport>("jacocoTestReport") {
                dependsOn(tasks.named("test"))
                reports {
                    xml.required.set(true)
                    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
                    html.required.set(true)
                    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
                    csv.required.set(false)
                }
            }
        }
    }
}

val jacocoTestReportAllModules by tasks.registering {
    group = "verification"
    description = "Runs tests and generates JaCoCo XML/HTML reports for every test-bearing engine module."
    dependsOn(engineTestModules.map { ":$it:jacocoTestReport" })
}

val verifyJacocoReports by tasks.registering {
    group = "verification"
    description = "Verifies that every test-bearing engine module produced JaCoCo XML and HTML reports."
    dependsOn(jacocoTestReportAllModules)

    doLast {
        val missingReports = mutableListOf<String>()

        engineTestModules.forEach { moduleName ->
            val module = project(":$moduleName")
            val xmlReport = module.layout.buildDirectory
                .file("reports/jacoco/test/jacocoTestReport.xml")
                .get().asFile
            val htmlReport = module.layout.buildDirectory
                .file("reports/jacoco/test/html/index.html")
                .get().asFile

            if (!xmlReport.isFile) {
                missingReports += "$moduleName XML: ${xmlReport.relativeTo(rootDir).path}"
            }
            if (!htmlReport.isFile) {
                missingReports += "$moduleName HTML: ${htmlReport.relativeTo(rootDir).path}"
            }
        }

        check(missingReports.isEmpty()) {
            "Missing JaCoCo reports:\n" + missingReports.joinToString("\n")
        }
    }
}

tasks.register("buildAllModules") {
    group = "build"
    description = "Runs the root quality gate and compiles/tests every declared engine/game/support module."
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

val phaseZeroTaskAliases = listOf(
    "runOpenGL46Spike",
    "runJoltLifecycleSpike",
    "runOpenAL3DAudioSpike",
    "runWindowsNativeCiSmoke",
    "runUdpSpikeServer",
    "runUdpSpikeClient",
    "runNetworkImpairmentHarness",
    "runNetworkImpairmentLatency",
    "runNetworkImpairmentJitter",
    "runNetworkImpairmentLoss",
    "runNetworkImpairmentDuplication",
    "runNetworkImpairmentReordering",
    "runSteamInitSpike",
    "runSteamFlatApiFfmSpike",
    "runIntegratedNativeSmoke",
    "runIntegratedNativeSoak"
)

phaseZeroTaskAliases.forEach { taskName ->
    tasks.register(taskName) {
        group = "verification"
        description = "Compatibility alias for :feasibility-spikes:$taskName."
        dependsOn(":feasibility-spikes:$taskName")
    }
}
