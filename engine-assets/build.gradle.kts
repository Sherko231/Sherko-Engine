plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(libs.jackson.databind)
}
