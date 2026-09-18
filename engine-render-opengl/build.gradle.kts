import java.util.zip.ZipFile

plugins {
    `java-library`
}

val publicApiTest by sourceSets.creating {
    java.srcDir("src/publicApiTest/java")
}

val publicApiConsumerClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.api.get())
}

dependencies {
    api(project(":engine-core"))
    api(project(":engine-platform-lwjgl"))
    implementation(libs.lwjgl.opengl)
    testImplementation(libs.lwjgl.shaderc)
    testRuntimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
    testRuntimeOnly("org.lwjgl:lwjgl-shaderc:${libs.versions.lwjgl.get()}:natives-windows")
    implementation(project(":engine-assets"))
    implementation(project(":engine-ui"))
}

val publicApiJar by tasks.registering(Jar::class) {
    description = "Builds the renderer compile artifact containing only the supported public API package."
    group = "build"
    archiveClassifier.set("api")
    from(sourceSets.main.get().output) {
        include("com/samo/engine/render/api/**")
    }
}

configurations.named("apiElements") {
    outgoing.artifacts.clear()
    outgoing.artifact(publicApiJar)
    outgoing.variants.removeIf { it.name == "classes" }
}

publicApiTest.compileClasspath = files(publicApiJar.flatMap { it.archiveFile }) + publicApiConsumerClasspath
publicApiTest.runtimeClasspath = publicApiTest.output + publicApiTest.compileClasspath

val verifyPublicApiArtifact by tasks.registering {
    description = "Verifies renderer compile/runtime artifacts preserve the supported public boundary."
    group = "verification"
    dependsOn(publicApiJar)
    dependsOn(tasks.named("jar"))

    doLast {
        val apiArtifact = publicApiJar.get().archiveFile.get().asFile
        val runtimeArtifact = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        val apiElements = configurations.getByName("apiElements")

        check(apiElements.outgoing.variants.none { it.name == "classes" }) {
            "Renderer apiElements must not expose the full main classes directory"
        }

        ZipFile(apiArtifact).use { archive ->
            val entries = archive.entries().asSequence().map { it.name }.toList()
            check("com/samo/engine/render/api/OpenGlRenderer.class" in entries) {
                "Renderer API artifact must contain OpenGlRenderer"
            }
            check(entries.none { it.startsWith("com/samo/engine/render/opengl/internal/") }) {
                "Renderer API artifact must not expose internal renderer implementation classes"
            }
        }

        ZipFile(runtimeArtifact).use { archive ->
            val entries = archive.entries().asSequence().map { it.name }.toList()
            check("com/samo/engine/render/api/OpenGlRenderer.class" in entries) {
                "Renderer runtime artifact must contain OpenGlRenderer"
            }
            check("com/samo/engine/render/opengl/internal/IndexedStaticMeshPipeline.class" in entries) {
                "Renderer runtime artifact must retain the indexed-mesh implementation"
            }
            check("shaders/p5/basic.vert" in entries && "shaders/p5/basic.frag" in entries) {
                "Renderer runtime artifact must retain committed shader resources"
            }
        }
    }
}

val verifyPublicApiBoundary by tasks.registering {
    description = "Compiles a renderer-only consumer and verifies the public compile artifact surface."
    group = "verification"
    dependsOn(tasks.named(publicApiTest.compileJavaTaskName))
    dependsOn(verifyPublicApiArtifact)
}

tasks.register<Test>("validateGlsl") {
    description = "Validates committed Phase 5 GLSL sources with locked LWJGL Shaderc."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.samo.engine.render.opengl.internal.GlslOfflineValidationTest.validatesCommittedRuntimeShaders")
        includeTestsMatching("com.samo.engine.render.opengl.internal.GlslOfflineValidationTest.rejectsBrokenFixtureWithDiagnostics")
    }
    shouldRunAfter(tasks.test)
}

tasks.named("check") {
    dependsOn(tasks.named("validateGlsl"))
    dependsOn(verifyPublicApiBoundary)
}
