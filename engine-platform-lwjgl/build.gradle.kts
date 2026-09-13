plugins {
    `java-library`
}

dependencies {
    api(project(":engine-core"))

    implementation(libs.jackson.databind)
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.glfw)
    implementation(libs.lwjgl.opengl)

    runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-windows")
}
