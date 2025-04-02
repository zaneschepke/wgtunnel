import org.gradle.api.Project
import java.io.File
import java.util.*

fun Project.getCurrentFlavor(): String {
    val taskRequestsStr = gradle.startParameter.taskRequests.toString()
    val pattern: java.util.regex.Pattern =
        if (taskRequestsStr.contains(":app:assemble")) {
            java.util.regex.Pattern.compile(":app:assemble(\\w+)(Release|Debug)")
        } else {
            java.util.regex.Pattern.compile(":app:bundle(\\w+)(Release|Debug)")
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

fun Project.getBuildTaskName(): String {
    val taskRequestsStr = gradle.startParameter.taskRequests[0].toString()
    return taskRequestsStr.also {
        project.logger.lifecycle("Build task: $it")
    }
}

fun getLocalProperty(key: String, file: String = "local.properties"): String? {
    val properties = Properties()
    val localProperties = File(file)
    if (localProperties.isFile) {
        java.io.InputStreamReader(java.io.FileInputStream(localProperties), Charsets.UTF_8)
            .use { reader ->
                properties.load(reader)
            }
    } else return null
    return properties.getProperty(key)
}


fun Project.getSigningProperties(): Properties {
    return Properties().apply {
        // created local file for signing details
        try {
            load(file("signing.properties").reader())
        } catch (_: Exception) {
            load(file("signing_template.properties").reader())
        }
    }
}

fun Project.getStoreFile(): File {
    return file(
        System.getenv()
            .getOrDefault(
                Constants.KEY_STORE_PATH_VAR,
                getSigningProperties().getProperty(Constants.KEY_STORE_PATH_VAR),
            ),
    )
}

fun Project.getSigningProperty(property: String): String {
    // try to get secrets from env first for pipeline build, then properties file for local
    return System.getenv()
        .getOrDefault(
            property,
            getSigningProperties().getProperty(property),
        )
}

fun Project.languageList(): List<String> {
	return fileTree("../app/src/main/res") { include("**/strings.xml") }
		.asSequence()
		.map { stringFile -> stringFile.parentFile.name }
		.map { valuesFolderName -> valuesFolderName.replace("values-", "") }
		.filter { valuesFolderName -> valuesFolderName != "values" }
		.map { languageCode -> languageCode.replace("-r", "_") }
		.distinct()
		.sorted()
		.toList() + "en"
}




