import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import java.io.File
import java.util.Properties

private fun getCurrentFlavor(gradle: Gradle): String {
    val taskRequestsStr = gradle.startParameter.taskRequests.toString()
    val pattern: java.util.regex.Pattern =
        if (taskRequestsStr.contains("assemble")) {
            java.util.regex.Pattern.compile("assemble(\\w+)(Release|Debug)")
        } else {
            java.util.regex.Pattern.compile("bundle(\\w+)(Release|Debug)")
        }

    val matcher = pattern.matcher(taskRequestsStr)
    val flavor =
        if (matcher.find()) {
            matcher.group(1).lowercase()
        } else {
            print("NO FLAVOR FOUND")
            ""
        }
    return flavor
}

fun getLocalProperty(key: String, file: String = "local.properties"): String? {
    val properties = java.util.Properties()
    val localProperties = File(file)
    if (localProperties.isFile) {
        java.io.InputStreamReader(java.io.FileInputStream(localProperties), Charsets.UTF_8)
            .use { reader ->
                properties.load(reader)
            }
    } else return null
    return properties.getProperty(key)
}

fun Project.isGeneralFlavor(gradle: Gradle): Boolean {
    return getCurrentFlavor(gradle) == "general"
}

fun Project.isNightlyBuild(): Boolean {
    return gradle.startParameter.taskRequests.toString().contains("Nightly")
}

fun Project.getSigningProperties() : Properties {
    return Properties().apply {
        // created local file for signing details
        try {
            load(file("signing.properties").reader())
        } catch (_: Exception) {
            load(file("signing_template.properties").reader())
        }
    }
}

fun Project.getStoreFile() : File {
    return file(
        System.getenv()
            .getOrDefault(
                Constants.KEY_STORE_PATH_VAR,
                getSigningProperties().getProperty(Constants.KEY_STORE_PATH_VAR),
            ),
    )
}

fun Project.getSigningProperty(property: String) : String {
    // try to get secrets from env first for pipeline build, then properties file for local
    return System.getenv()
        .getOrDefault(
            property,
            getSigningProperties().getProperty(property),
        )
}

fun Project.signingConfigName() : String {
    return if(getSigningProperty(Constants.KEY_PASS_VAR) == "") Constants.DEBUG else Constants.RELEASE
}




