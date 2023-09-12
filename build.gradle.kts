// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val objectBoxVersion by extra("3.7.0")
    val hiltVersion by extra("2.48")
    val accompanistVersion by extra("0.31.2-alpha")

    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:$objectBoxVersion")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}

plugins {
    id("com.android.application") version "8.2.0-beta03" apply false
    id("org.jetbrains.kotlin.android") version "1.8.22"  apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    kotlin("plugin.serialization") version "1.8.22" apply false
}
