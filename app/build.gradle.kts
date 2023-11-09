import java.util.Properties

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
        versionCode = 32000
        versionName = "3.2.1"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        resourceConfigurations.addAll(listOf("en"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val properties = Properties().apply {
                //created local file for signing details
                try {
                    load(file("signing.properties").reader())
                } catch (_ : Exception) {
                    load(file("signing_template.properties").reader())
                }
            }
            val storePassVarName = "SIGNING_STORE_PASSWORD"
            val keyAliasVarName = "SIGNING_KEY_ALIAS"
            val keyPassVarName = "SIGNING_KEY_PASSWORD"
            val keyStorePathVarName = "KEY_STORE_PATH"

            //try to get secrets from env first for pipeline build, then properties file for local build
            storeFile = file(System.getenv().getOrDefault(keyStorePathVarName, properties.getProperty(keyStorePathVarName)))
            storePassword = System.getenv().getOrDefault(storePassVarName, properties.getProperty(storePassVarName))
            keyAlias = System.getenv().getOrDefault(keyAliasVarName, properties.getProperty(keyAliasVarName))
            keyPassword = System.getenv().getOrDefault(keyPassVarName, properties.getProperty(keyPassVarName))
        }
    }

    buildTypes {
        //don't strip
        packaging.jniLibs.keepDebugSymbols.addAll(listOf("libwg-go.so", "libwg-quick.so", "libwg.so"))

        applicationVariants.all {
            val variant = this
            variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    val outputFileName = "wgtunnel-${variant.flavorName}-${variant.buildType.name}-${variant.versionName}.apk"
                    output.outputFileName = outputFileName
                }
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        isCoreLibraryDesugaringEnabled = true
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)

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