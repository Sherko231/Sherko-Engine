plugins {
    `java-library`
}

group = "com.samo"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
}
