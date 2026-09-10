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
    implementation(project(":engine-core"))
    implementation(project(":engine-assets"))
    implementation(project(":engine-world"))
    implementation(project(":engine-render-opengl"))
}
