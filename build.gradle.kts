plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.android) apply false
	alias(libs.plugins.hilt.android) apply false
	alias(libs.plugins.kotlinxSerialization) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.androidLibrary) apply false
	alias(libs.plugins.compose.compiler) apply false
	alias(libs.plugins.ktlint)
}

subprojects {
	apply {
		plugin(rootProject.libs.plugins.ktlint.get().pluginId)
	}

	ktlint {
		debug.set(false)
		verbose.set(true)
		android.set(true)
		outputToConsole.set(true)
		ignoreFailures.set(false)
		enableExperimentalRules.set(true)
	}
}
