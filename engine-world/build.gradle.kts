plugins {
    `java-library`
}

dependencies {
    api(project(":engine-core"))
    api(project(":engine-assets"))
    implementation(libs.jackson.databind)
}
