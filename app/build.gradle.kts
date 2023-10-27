plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.zaneschepke.wireguardautotunnel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zaneschepke.wireguardautotunnel"
        minSdk = 26
        targetSdk = 34
        versionCode = 31900
        versionName = "3.1.9"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        resourceConfigurations.addAll(listOf("en"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    flavorDimensions.add("type")
    productFlavors {
        create("fdroid") {
            dimension = "type"
            proguardFile("fdroid-rules.pro")
        }
        create("general") {
            dimension = "type"
            if (BuildHelper.isReleaseBuild(gradle) && BuildHelper.isGeneralFlavor(gradle))
            {
                apply(plugin = "com.google.gms.google-services")
                apply(plugin = "com.google.firebase.crashlytics")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true

    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val generalImplementation by configurations
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // optional - helpers for implementing LifecycleOwner in a Service
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)

    //test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.manifest)

    //wg
    implementation(libs.tunnel)

    //logging
    implementation(libs.timber)

    // compose navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    //accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.drawablepainter)

    //room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    //lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)


    //icons
    implementation(libs.material.icons.extended)
    //serialization
    implementation(libs.kotlinx.serialization.json)

    //firebase crashlytics
    generalImplementation(platform(libs.firebase.bom))
    generalImplementation(libs.google.firebase.crashlytics.ktx)
    generalImplementation(libs.google.firebase.analytics.ktx)

    //barcode scanning
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)

    //bio
    implementation(libs.androidx.biometric.ktx)

    //shortcuts
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.google.shortcuts)
}