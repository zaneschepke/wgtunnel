plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.grgit)
}

val versionFile = file("$rootDir/versionCode.txt")

val versionCodeIncrement =
    with(getBuildTaskName().lowercase()) {
        when {
            this.contains(Constants.NIGHTLY) || this.contains(Constants.PRERELEASE) -> {
                if (versionFile.exists()) {
                    versionFile.readText().trim().toInt() + 1
                } else {
                    1
                }
            }
            else -> 0
        }
    }

android {
    namespace = Constants.APP_ID
    compileSdk = Constants.TARGET_SDK

    androidResources { generateLocaleConfig = true }

    // reproducibility
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    defaultConfig {
        applicationId = Constants.APP_ID
        minSdk = Constants.MIN_SDK
        targetSdk = Constants.TARGET_SDK
        versionCode = Constants.VERSION_CODE + versionCodeIncrement
        versionName = determineVersionName()

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }

        sourceSets {
            getByName("debug").assets.srcDirs(files("$projectDir/schemas")) // Room
        }

        buildConfigField(
            "String[]",
            "LANGUAGES",
            "new String[]{ ${languageList().joinToString(separator = ", ") { "\"$it\"" }} }",
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create(Constants.RELEASE) {
            storeFile = getStoreFile()
            storePassword = getSigningProperty(Constants.STORE_PASS_VAR)
            keyAlias = getSigningProperty(Constants.KEY_ALIAS_VAR)
            keyPassword = getSigningProperty(Constants.KEY_PASS_VAR)
        }
    }

    buildTypes {
        // don't strip
        packaging.jniLibs.keepDebugSymbols.addAll(
            listOf("libwg-go.so", "libwg-quick.so", "libwg.so")
        )

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "WG Tunnel - Debug")
            isDebuggable = true
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider.debug\"")
        }

        create(Constants.PRERELEASE) {
            initWith(buildTypes.getByName(Constants.RELEASE))
            applicationIdSuffix = ".prerelease"
            resValue("string", "app_name", "WG Tunnel - Pre")
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider.pre\"")
        }

        create(Constants.NIGHTLY) {
            initWith(buildTypes.getByName(Constants.RELEASE))
            applicationIdSuffix = ".nightly"
            resValue("string", "app_name", "WG Tunnel - Nightly")
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider.nightly\"")
        }

        applicationVariants.all {
            val variant = this
            variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    val outputFileName =
                        "${Constants.APP_NAME}-${variant.flavorName}-" +
                            "${variant.buildType.name}-${variant.versionName}.apk"
                    output.outputFileName = outputFileName
                }
        }
    }
    flavorDimensions.add(Constants.TYPE)
    productFlavors {
        create("fdroid") {
            dimension = Constants.TYPE
            proguardFile("fdroid-rules.pro")
        }
        create("general") { dimension = Constants.TYPE }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = Constants.JVM_TARGET }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":logcatter"))
    implementation(project(":networkmonitor"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // helpers for implementing LifecycleOwner in a Service
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.storage)

    // test
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.manifest)

    // tunnel
    implementation(libs.tunnel)
    implementation(libs.amneziawg.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // logging
    implementation(libs.timber)

    // compose navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)

    // accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.drawablepainter)

    // storage
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)

    // lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    // serialization
    implementation(libs.kotlinx.serialization.json)

    // ui
    implementation(libs.zxing.android.embedded)
    implementation(libs.material.icons.extended)

    // bio
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.pin.lock.compose)

    // shortcuts
    implementation(libs.androidx.core)

    // splash
    implementation(libs.androidx.core.splashscreen)

    // worker
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
}

fun determineVersionName(): String {
    return with(getBuildTaskName().lowercase()) {
        when {
            contains(Constants.NIGHTLY) || contains(Constants.PRERELEASE) ->
                Constants.VERSION_NAME + "-${grgitService.service.get().grgit.head().abbreviatedId}"
            else -> Constants.VERSION_NAME
        }
    }
}

val incrementVersionCode by
    tasks.registering {
        doLast {
            val versionFile = file("$rootDir/versionCode.txt")
            if (versionFile.exists()) {
                versionFile.writeText(versionCodeIncrement.toString())
                println("Incremented versionCode to $versionCodeIncrement")
            }
        }
    }

tasks.whenTaskAdded {
    if (name.startsWith("assemble") && !name.lowercase().contains("debug")) {
        dependsOn(incrementVersionCode)
    }
}
