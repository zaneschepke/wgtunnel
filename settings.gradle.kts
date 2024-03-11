pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val GITHUB_USER_VAR = "GH_USER"
val GITHUB_TOKEN_VAR = "GH_TOKEN"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/zaneschepke/wireguard-android")
            credentials {
                username = getLocalProperty(GITHUB_USER_VAR) ?: System.getenv(GITHUB_USER_VAR)
                password = getLocalProperty(GITHUB_TOKEN_VAR) ?: System.getenv(GITHUB_TOKEN_VAR)
            }
        }
        google()
        mavenCentral()
    }
}

fun getLocalProperty(key: String, file: String = "local.properties"): String? {
    val properties = java.util.Properties()
    val localProperties = File(file)
    if (localProperties.isFile) {
        java.io.InputStreamReader(java.io.FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
            properties.load(reader)
        }
    } else return null
    return properties.getProperty(key)
}

rootProject.name = "WG Tunnel"

include(":app")
