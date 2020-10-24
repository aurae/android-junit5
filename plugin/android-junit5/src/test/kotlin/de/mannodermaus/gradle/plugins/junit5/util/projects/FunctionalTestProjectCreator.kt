package de.mannodermaus.gradle.plugins.junit5.util.projects

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.toml
import de.mannodermaus.gradle.plugins.junit5.util.TestedAgp
import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.opentest4j.TestAbortedException
import java.io.File

private const val TEST_PROJECTS_RESOURCE = "/test-projects"
private const val BUILD_GRADLE_TEMPLATE_NAME = "build.gradle.kts.template"
private const val SETTINGS_GRADLE_TEMPLATE_NAME = "settings.gradle.kts.template"
private const val OUTPUT_BUILD_GRADLE_NAME = "build.gradle.kts"
private const val OUTPUT_SETTINGS_GRADLE_NAME = "settings.gradle.kts"
private const val PROJECT_CONFIG_FILE_NAME = "config.toml"
private const val SRC_FOLDER_NAME = "src"

class FunctionalTestProjectCreator(private val rootFolder: File,
                                   private val environment: TestEnvironment) {

  private val rawBuildGradle: String
  private val rawSettingsGradle: String

  val allSpecs: List<Spec>

  init {
    // Obtain access to the root folder for all test projects
    val rootUrl = FunctionalTestProjectCreator::class.java.getResource(TEST_PROJECTS_RESOURCE)
    val rootFolder = File(rootUrl.toURI())

    // Read in the raw template texts
    val buildTemplateFile = File(rootFolder, BUILD_GRADLE_TEMPLATE_NAME)
    rawBuildGradle = if (buildTemplateFile.exists()) {
      buildTemplateFile.bufferedReader().readText()
    } else {
      ""
    }
    val settingsTemplateFile = File(rootFolder, SETTINGS_GRADLE_TEMPLATE_NAME)
    rawSettingsGradle = if (settingsTemplateFile.exists()) {
      settingsTemplateFile.bufferedReader().readText()
    } else {
      ""
    }

    // Collect all eligible test folders
    allSpecs = rootFolder.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { folder -> Spec.tryCreate(folder) }
        ?: emptyList()
  }

  @Throws(TestAbortedException::class)
  fun createProject(spec: Spec, agp: TestedAgp): File {
    // Validate the spec requirement against the executing AGP version first
    validateSpec(spec, agp)

    // Construct the project folder, cleaning it if necessary.
    // If any Gradle or build caches already exist, we keep those around.
    // That's the reason for not doing "projectFolder.deleteRecursively()"
    // and nuking everything at once.
    val projectName = "${spec.name}_${agp.shortVersion}"
    val projectFolder = File(rootFolder, projectName)
    if (projectFolder.exists()) {
      File(projectFolder, SRC_FOLDER_NAME).deleteRecursively()
      File(projectFolder, OUTPUT_BUILD_GRADLE_NAME).delete()
      File(projectFolder, OUTPUT_SETTINGS_GRADLE_NAME).delete()
    }
    projectFolder.mkdirs()

    // Copy over the source folders
    val targetSrcFolder = File(projectFolder, "src").also { it.mkdir() }
    spec.srcFolder.copyRecursively(targetSrcFolder)

    // Construct the build script from its raw template, using the environment properties as placeholders
    val replacements = environment.envProps.mapKeys { it.key.toString() }.toMutableMap()
    replacements["AGP_VERSION"] = agp.version
    replacements["USE_KOTLIN"] = spec.useKotlin
    replacements["USE_FLAVORS"] = spec.useFlavors
    replacements["USE_CUSTOM_BUILD_TYPE"] = spec.useCustomBuildType
    replacements["RETURN_DEFAULT_VALUES"] = spec.returnDefaultValues
    replacements["INCLUDE_ANDROID_RESOURCES"] = spec.includeAndroidResources
    replacements["DISABLE_TESTS_FOR_BUILD_TYPES"] = spec.disableTestsForBuildTypes
    val processor = BuildScriptTemplateProcessor(agp.requiresGradle, replacements)

    val processedBuildGradle = processor.process(rawBuildGradle)
    File(projectFolder, OUTPUT_BUILD_GRADLE_NAME).writeText(processedBuildGradle)
    val processedSettingsGradle = processor.process(rawSettingsGradle)
    File(projectFolder, OUTPUT_SETTINGS_GRADLE_NAME).writeText(processedSettingsGradle)

    return projectFolder
  }

  private fun validateSpec(spec: Spec, agp: TestedAgp) {
    if (spec.minAgpVersion != null) {
      // If the spec dictates a minimum version of the AGP,
      // disable the test for plugin versions below that minimum requirement
      assumeTrue(
          SemanticVersion(agp.version) >= SemanticVersion(spec.minAgpVersion),
          "This project requires AGP ${spec.minAgpVersion} and was disabled on ths version.")
    }
  }

  /* Types */

  class Spec private constructor(val name: String,
                                 val srcFolder: File,
                                 config: Config) {
    val minAgpVersion = config[TomlSpec.Settings.minAgpVersion]
    val useKotlin = config[TomlSpec.Settings.useKotlin]
    val useFlavors = config[TomlSpec.Settings.useFlavors]
    val useCustomBuildType = config[TomlSpec.Settings.useCustomBuildType]
    val returnDefaultValues = config[TomlSpec.Settings.returnDefaultValues]
    val includeAndroidResources = config[TomlSpec.Settings.includeAndroidResources]
    val expectedTests = config[TomlSpec.expectations]

    val disableTestsForBuildTypes = config[TomlSpec.Settings.disableTestsForBuildTypes]
        ?.split(",")?.map(String::trim)
        ?: emptyList()

    companion object {
      fun tryCreate(folder: File): Spec? {
        if (folder.isFile) {
          return null
        }

        val srcFolder = File(folder, SRC_FOLDER_NAME)
        if (!srcFolder.exists()) {
          return null
        }

        val configFile = File(folder, PROJECT_CONFIG_FILE_NAME)
        if (!configFile.exists()) {
          return null
        }

        val config = Config { addSpec(TomlSpec) }.from.toml.file(configFile)
        return Spec(folder.name, srcFolder, config)
      }
    }
  }

  // Structure of the virtual project config file, used only internally
  private object TomlSpec : ConfigSpec(prefix = "") {
    val expectations by required<List<ExpectedTests>>()

    object Settings : ConfigSpec() {
      val minAgpVersion by optional<String?>(default = null)
      val useFlavors by optional(default = false)
      val useKotlin by optional(default = false)
      val useCustomBuildType by optional<String?>(default = null)
      val returnDefaultValues by optional(default = false)
      val includeAndroidResources by optional(default = false)
      val disableTestsForBuildTypes by optional<String?>(default = null)
    }
  }

  /**
   * Data holder for one set of expected tests within the virtual project
   */
  data class ExpectedTests(
      val buildType: String,
      val productFlavor: String?,
      private val tests: String
  ) {
    val testsList = tests.split(",").map(String::trim)
  }
}
