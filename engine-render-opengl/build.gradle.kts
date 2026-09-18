plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-platform-lwjgl"))
    implementation(libs.lwjgl.opengl)
    implementation(project(":engine-assets"))
    implementation(project(":engine-ui"))
}
