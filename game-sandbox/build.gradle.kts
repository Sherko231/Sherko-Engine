plugins {
    `java-library`
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-world"))
    implementation(project(":engine-physics-jolt"))
    implementation(project(":engine-network-api"))
    implementation(project(":engine-ui"))
}
