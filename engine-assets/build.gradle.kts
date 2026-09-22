plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(libs.jackson.databind)
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.assimp)
    implementation(libs.lwjgl.stb)

    runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-assimp:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-stb:${libs.versions.lwjgl.get()}:natives-windows")
}

tasks.register<JavaExec>("runAssetCooker") {
    group = "application"
    description = "Cooks one source asset directory into one fresh output cache."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.samo.engine.assets.internal.AssetCookerMain")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
