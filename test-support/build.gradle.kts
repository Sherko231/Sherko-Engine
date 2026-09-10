plugins {
    `java-library`
}

dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    runtimeOnly(libs.junit.platform.launcher)
}
