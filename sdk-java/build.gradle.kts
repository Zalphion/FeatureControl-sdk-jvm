plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.jspecify)

    implementation(libs.moshi.legacy)
    implementation(libs.okhttp3)
    implementation(libs.slf4j.api.legacy)

    runtimeOnly(libs.slf4j.simple)

    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.assertj)

    testFixturesRuntimeOnly(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}

configurations {
    testFixturesApi {
        extendsFrom(configurations.compileOnly.get())
        extendsFrom(configurations.implementation.get())
    }
}

tasks.shadowJar {
    // replace original JAR for maven distribution
    archiveClassifier.set("")

    // relocate implementation dependencies
    relocate("com.squareup.moshi", "com.zalphion.featurecontrol.lib.moshi")
    relocate("okhttp", "com.zalphion.featurecontrol.lib.okhttp")
    relocate("okio", "com.zalphion.featurecontrol.lib.okio")

    // don't vendor slf4j-api
    dependencies {
        exclude(dependency("org.slf4j:slf4j-api:.*"))
    }

    // include project license
    from(rootProject.file("LICENSE")) {
        into("")
    }

    // remove unused vendored code
    minimize()
}