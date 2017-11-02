package de.mannodermaus.gradle.plugins.android_junit5

import de.mannodermaus.gradle.plugins.android_junit5.util.FileLanguage
import de.mannodermaus.gradle.plugins.android_junit5.util.TestEnvironment
import org.apache.commons.lang.StringUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

import static de.mannodermaus.gradle.plugins.android_junit5.util.StringUtils.splitClasspath

abstract class BaseFunctionalSpec extends Specification {

  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()
  private final TestEnvironment environment = new TestEnvironment()

  private File buildFile
  private List<File> pluginClasspath
  private List<File> testCompileClasspath

  abstract String pluginClasspathResource()

  abstract String functionalTestCompileClasspathResource()

  abstract String concatProductFlavorsToString(List<String> flavorNames)

  def setup() {
    pluginClasspath = loadClassPathManifestResource(pluginClasspathResource())
    testCompileClasspath = loadClassPathManifestResource(functionalTestCompileClasspathResource())
    buildFile = testProjectDir.newFile("build.gradle")
    buildFile << """
buildscript {
	dependencies {
		classpath files(${splitClasspath(pluginClasspath)})
	}
}
"""
  }

  /*
   * ===============================================================================================
   * Tests
   * ===============================================================================================
   */

  def "Executes Java tests in default source set"() {
    given:
    androidPlugin()
    junit5Plugin()
    javaFile()
    javaTest()

    when:
    BuildResult result = runGradle()
        .withArguments("build")
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestDebug").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestRelease").outcome == TaskOutcome.SUCCESS

    // 1 per build type (Debug & Release)
    StringUtils.countMatches(result.output, "1 tests successful") == 2
  }

  def "Executes Kotlin tests in default source set"() {
    given:
    androidPlugin()
    kotlinPlugin()
    junit5Plugin()
    javaFile()
    kotlinTest()

    when:
    BuildResult result = runGradle()
        .withArguments("build")
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestDebug").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestRelease").outcome == TaskOutcome.SUCCESS

    // 1 per build type (Debug & Release)
    StringUtils.countMatches(result.output, "1 tests successful") == 2
  }

  def "Executes Java unit tests in build-type-specific source set"() {
    given:
    androidPlugin()
    junit5Plugin()
    javaFile()
    javaTest()
    javaTest(null, "debug")

    when:
    BuildResult result = runGradle()
        .withArguments("junitPlatformTestDebug")
        .build()

    then:
    result.task(":junitPlatformTestDebug").outcome == TaskOutcome.SUCCESS
    result.output.contains("2 tests successful")
    result.output.contains("JavaDebugAdderTest")
    result.output.contains("JavaAdderTest")

    when:
    result = runGradle()
        .withArguments("junitPlatformTestRelease")
        .build()

    then:
    result.task(":junitPlatformTestRelease").outcome == TaskOutcome.SUCCESS
    result.output.contains("1 tests successful")
    result.output.contains("JavaAdderTest")
  }

  def "Executes Kotlin unit tests in build-type-specific source set"() {
    given:
    androidPlugin()
    kotlinPlugin()
    junit5Plugin()
    javaFile()
    kotlinTest()
    kotlinTest(null, "debug")

    when:
    BuildResult result = runGradle()
        .withArguments("junitPlatformTestDebug")
        .build()

    then:
    result.task(":junitPlatformTestDebug").outcome == TaskOutcome.SUCCESS
    result.output.contains("2 tests successful")
    result.output.contains("KotlinDebugAdderTest")
    result.output.contains("KotlinAdderTest")

    when:
    result = runGradle()
        .withArguments("junitPlatformTestRelease")
        .build()

    then:
    result.task(":junitPlatformTestRelease").outcome == TaskOutcome.SUCCESS
    result.output.contains("1 tests successful")
    result.output.contains("KotlinAdderTest")
  }

  def "Executes Java unit tests in flavor-specific source set"() {
    given:
    androidPlugin(flavorNames: ["free"])
    junit5Plugin()
    javaFile()
    javaTest()
    javaTest("free", null)

    when:
    BuildResult result = runGradle()
        .withArguments("build")
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestFreeDebug").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestFreeRelease").outcome == TaskOutcome.SUCCESS

    // 1 per build type (Debug & Release)
    StringUtils.countMatches(result.output, "2 tests successful") == 2
  }

  def "Executes Kotlin unit tests in flavor-specific source set"() {
    given:
    androidPlugin(flavorNames: ["free"])
    kotlinPlugin()
    junit5Plugin()
    javaFile()
    kotlinTest()
    kotlinTest("free", null)

    when:
    BuildResult result = runGradle()
        .withArguments("build")
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestFreeDebug").outcome == TaskOutcome.SUCCESS
    result.task(":junitPlatformTestFreeRelease").outcome == TaskOutcome.SUCCESS

    // 1 per build type (Debug & Release)
    StringUtils.countMatches(result.output, "2 tests successful") == 2
  }

  def "Executes Java unit tests in build-type-and-flavor-specific source set"() {
    given:
    androidPlugin(flavorNames: ["free"])
    junit5Plugin()
    javaFile()
    javaTest()
    javaTest(null, "debug")
    javaTest("free", "debug")
    javaTest(null, "release")

    when:
    BuildResult result = runGradle()
        .withArguments("junitPlatformTestFreeDebug")
        .build()

    then:
    result.task(":junitPlatformTestFreeDebug").outcome == TaskOutcome.SUCCESS
    result.output.contains("3 tests successful")
    result.output.contains("JavaFreeDebugAdderTest")
    result.output.contains("JavaDebugAdderTest")
    result.output.contains("JavaAdderTest")

    when:
    result = runGradle()
        .withArguments("junitPlatformTestFreeRelease")
        .build()

    then:
    result.task(":junitPlatformTestFreeRelease").outcome == TaskOutcome.SUCCESS
    result.output.contains("2 tests successful")
    result.output.contains("JavaReleaseAdderTest")
    result.output.contains("JavaAdderTest")
  }

  def "Executes Kotlin unit tests in build-type-and-flavor-specific source set"() {
    given:
    androidPlugin(flavorNames: ["free"])
    kotlinPlugin()
    junit5Plugin()
    javaFile()
    kotlinTest()
    kotlinTest("free", "debug")
    kotlinTest(null, "debug")
    kotlinTest(null, "release")

    when:
    BuildResult result = runGradle()
        .withArguments("junitPlatformTestFreeDebug")
        .build()

    then:
    result.task(":junitPlatformTestFreeDebug").outcome == TaskOutcome.SUCCESS
    result.output.contains("3 tests successful")
    result.output.contains("KotlinFreeDebugAdderTest")
    result.output.contains("KotlinDebugAdderTest")
    result.output.contains("KotlinAdderTest")

    when:
    result = runGradle()
        .withArguments("junitPlatformTestFreeRelease")
        .build()

    then:
    result.task(":junitPlatformTestFreeRelease").outcome == TaskOutcome.SUCCESS
    result.output.contains("2 tests successful")
    result.output.contains("KotlinReleaseAdderTest")
    result.output.contains("KotlinAdderTest")
  }

  /*
   * ===============================================================================================
   * Helpers, Factories & Utilities
   * ===============================================================================================
   */

  protected final def androidPlugin(Map properties) {
    List<String> flavorNames = properties?.flavorNames

    // Require AndroidManifest.xml
    def manifestPath = Paths.get(testProjectDir.root.toString(), "src", "main",
        "AndroidManifest.xml")
    Files.createDirectories(manifestPath.parent)
    manifestPath.withWriter {
      it.write("""<manifest package="de.mannodermaus.app"/>""")
    }

    def productFlavors = ""
    if (flavorNames) {
      productFlavors = this.concatProductFlavorsToString(flavorNames)
    }

    buildFile << """
apply plugin: "com.android.application"

android {
  compileSdkVersion "${environment.compileSdkVersion}"
  buildToolsVersion "${environment.buildToolsVersion}"

  defaultConfig {
    applicationId "de.mannodermaus.app"
    minSdkVersion ${environment.minSdkVersion}
    targetSdkVersion ${environment.targetSdkVersion}
    versionCode 1
    versionName "1.0"
  }

  $productFlavors

  lintOptions {
    abortOnError false
  }
}

dependencies {
  testCompile files(${splitClasspath(testCompileClasspath)})
}
"""
  }

  protected final def kotlinPlugin() {
    buildFile << """
apply plugin: "kotlin-android"

android {
  sourceSets {
    main.java.srcDir "src/main/kotlin"
    test.java.srcDir "src/test/kotlin"
  }
}
"""
  }

  protected final def junit5Plugin() {
    buildFile << """
apply plugin: "de.mannodermaus.android-junit5"

junitPlatform {
  details "flat"
}

dependencies {
  // Use local dependencies so that defaultDependencies are not used
  junitPlatform files(${splitClasspath(testCompileClasspath)})
}
"""
  }

  protected final def javaFile() {
    def path = Paths.get(testProjectDir.root.toString(), "src", "main", "java", "de",
        "mannodermaus", "app", "Adder.java")
    Files.createDirectories(path.parent)
    path.withWriter {
      it.write("""
package de.mannodermaus.app;

public class Adder {
  public int add(int a, int b) {
    return a + b;
  }
}
""")
    }
  }

  // Generic factory to write out a test file to the temp project.
  // Don't use this directly: Instead, use the abstractions "javaTest()" and "kotlinTest()".
  private final def test(Map properties) {
    FileLanguage language = properties.language
    String flavorName = properties.flavorName
    String buildType = properties.buildType
    String content = properties.content

    if (!flavorName) {
      flavorName = ""
    }
    if (!buildType) {
      buildType = ""
    }

    def variant = "${flavorName.capitalize()}${buildType.capitalize()}"
    def testName = "${language.name()}${variant}AdderTest"
    def sourceSet = "test${variant}"
    def fileName = language.appendExtension(testName)

    def filePath = Paths.get(testProjectDir.root.toString(),
        // e.g. "src/test/java" or "src/testFreeDebug/kotlin"
        "src", sourceSet, language.sourceDirectoryName,
        // Package name of test file
        "de", "mannodermaus", "app", fileName)
    Files.createDirectories(filePath.parent)

    filePath.withWriter { it.write(content.replace("__NAME__", testName)) }
  }

  protected final def javaTest(String flavorName = null, String buildType = null) {
    this.test(language: FileLanguage.Java,
        flavorName: flavorName,
        buildType: buildType,
        content: """
package de.mannodermaus.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class __NAME__ {
  @Test
  void test() {
    Adder adder = new Adder();
    assertEquals(4, adder.add(2, 2), "This should succeed!");
  }
}
""")
  }

  protected final def kotlinTest(String flavorName = null, String buildType = null) {
    this.test(language: FileLanguage.Kotlin,
        flavorName: flavorName,
        buildType: buildType,
        content: """
package de.mannodermaus.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class __NAME__ {
  @Test
  fun myTest() {
    val adder = Adder()
    assertEquals(4, adder.add(2, 2), "This should succeed!")
  }
}
""")
  }

  private GradleRunner runGradle() {
    return GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath(pluginClasspath)
  }

  private List<File> loadClassPathManifestResource(String name) {
    InputStream classpathResource = getClass().classLoader.getResourceAsStream(name)
    if (classpathResource == null) {
      throw new IllegalStateException("Did not find required resource with name ${name}")
    }
    return classpathResource.readLines().collect { new File(it) }
  }
}
