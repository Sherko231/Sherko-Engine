plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-assets"))
    implementation(project(":engine-world"))
    implementation(project(":engine-render-opengl"))
}
