plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(libs.jackson.databind)
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
