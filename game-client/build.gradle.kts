plugins {
    java
}

dependencies {
    implementation(project(":game-sandbox"))
    implementation(project(":engine-platform-lwjgl"))
    implementation(project(":engine-render-opengl"))
    implementation(project(":engine-audio-openal"))
    implementation(project(":engine-network-ip"))
    implementation(project(":engine-steam"))
}
