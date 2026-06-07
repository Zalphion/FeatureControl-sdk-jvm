plugins {
    alias(libs.plugins.kotlin.serialization)
}

pluginManager.apply("kotlin")

dependencies {
    implementation(libs.http4k.client.okhttp)
    implementation(libs.http4k.format.kotlinx.serialization)
    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.slf4j.simple)
}