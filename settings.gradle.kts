pluginManagement {
	repositories {
		mavenLocal()
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenLocal()
		google()
		mavenCentral()
		maven { url = uri("https://jitpack.io") }
	}
}

fun getLocalProperty(key: String, file: String = "local.properties"): String? {
	val properties = java.util.Properties()
	val localProperties = File(file)
	if (localProperties.isFile) {
		java.io.InputStreamReader(java.io.FileInputStream(localProperties), Charsets.UTF_8)
			.use { reader ->
				properties.load(reader)
			}
	} else {
		return null
	}
	return properties.getProperty(key)
}

rootProject.name = "WG Tunnel"

include(":app")
include(":logcatter")
include(":networkmonitor")
