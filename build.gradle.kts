plugins {
    java
    alias(libs.plugins.kotlin.jvm) apply false
    `java-test-fixtures`
    alias(libs.plugins.lombok) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    pluginManager.apply("java-test-fixtures")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(8)
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

subprojects {
    pluginManager.apply("java-library")
    pluginManager.apply("com.vanniktech.maven.publish")
    pluginManager.apply("io.freefair.lombok")
}

dependencies {
    implementation(project(":sdk-java"))
    testImplementation(testFixtures(project(":sdk-java")))
}
