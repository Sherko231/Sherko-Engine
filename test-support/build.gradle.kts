plugins {
    `java-library`
}

dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    runtimeOnly(libs.junit.platform.launcher)
}

val repositoryModules = rootProject.subprojects
    .map { it.name }
    .sorted()
    .joinToString(",")

tasks.withType<Test>().configureEach {
    systemProperty("repository.root", rootProject.projectDir.absolutePath)
    systemProperty("repository.modules", repositoryModules)
}
