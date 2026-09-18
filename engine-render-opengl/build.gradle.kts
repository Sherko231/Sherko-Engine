plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-platform-lwjgl"))
    implementation(libs.lwjgl.opengl)
    testImplementation(libs.lwjgl.shaderc)
    testRuntimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
    testRuntimeOnly("org.lwjgl:lwjgl-shaderc:${libs.versions.lwjgl.get()}:natives-windows")
    implementation(project(":engine-assets"))
    implementation(project(":engine-ui"))
}


tasks.register<Test>("validateGlsl") {
    description = "Validates committed Phase 5 GLSL sources with locked LWJGL Shaderc."
    group = "verification"
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.samo.engine.render.opengl.internal.GlslOfflineValidationTest.validatesCommittedRuntimeShaders")
        includeTestsMatching("com.samo.engine.render.opengl.internal.GlslOfflineValidationTest.rejectsBrokenFixtureWithDiagnostics")
    }
    shouldRunAfter(tasks.test)
}

tasks.named("check") {
    dependsOn(tasks.named("validateGlsl"))
}
