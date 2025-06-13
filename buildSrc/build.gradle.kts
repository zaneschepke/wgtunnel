plugins {
    `kotlin-dsl` // enable the Kotlin-DSL
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.semver4j:semver4j:5.7.1")
    implementation("org.ajoberstar.grgit:grgit-core:5.3.0")
}
