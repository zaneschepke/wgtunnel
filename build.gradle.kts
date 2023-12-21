buildscript {
    dependencies {
        if (BuildHelper.isReleaseBuild(gradle) && BuildHelper.isGeneralFlavor(gradle)) {
            classpath(libs.google.services)
            classpath(libs.firebase.crashlytics.gradle)
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    kotlin("plugin.serialization").version(libs.versions.kotlin).apply(false)
    alias(libs.plugins.ksp) apply false
}
