import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    google()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }

  dependencies {
    val latest = Artifacts.Plugin.latestStableVersion
    classpath("de.mannodermaus.gradle.plugins:android-junit5:$latest")
  }
}

plugins {
  id("com.android.library")
  kotlin("android")
}

apply {
  plugin("de.mannodermaus.android-junit5")
}

android {
  compileSdkVersion(Android.compileSdkVersion)

  dexOptions {
    javaMaxHeapSize = Android.javaMaxHeapSize
  }

  defaultConfig {
    minSdkVersion(Android.testCoreMinSdkVersion)
    targetSdkVersion(Android.targetSdkVersion)
    multiDexEnabled = true

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArgument("runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder")
  }

  sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
    getByName("test").java.srcDir("src/test/kotlin")
    getByName("androidTest").java.srcDir("src/androidTest/kotlin")
  }

  compileOptions {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
  }

  lintOptions {
    // JUnit 4 refers to java.lang.management APIs, which are absent on Android.
    warning("InvalidPackage")
  }

  packagingOptions {
    exclude("META-INF/LICENSE.md")
    exclude("META-INF/LICENSE-notice.md")
  }

  testOptions {
    unitTests.apply {
      isReturnDefaultValues = true
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
  failFast = true
  testLogging {
    events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

val commonTestImplementation = configurations.create("commonTestImplementation")
configurations {
  getByName("androidTestImplementation").extendsFrom(commonTestImplementation)
  getByName("testImplementation").extendsFrom(commonTestImplementation)
}

dependencies {
  api(Libs.junit_jupiter_api)

  implementation(Libs.kotlin_stdlib)
  implementation(Libs.androidx_test_core)
  implementation(Libs.androidx_test_runner)

  // This is required by the "instrumentation-runner" companion library,
  // since it can't provide any JUnit 5 runtime libraries itself
  // due to fear of prematurely incrementing the minSdkVersion requirement.
  runtimeOnly(Libs.junit_platform_runner)

  commonTestImplementation(Libs.truth)
  commonTestImplementation(Libs.mockito_core)
  commonTestImplementation(Libs.junit_jupiter_api)
  commonTestImplementation(Libs.junit_jupiter_engine)

  androidTestImplementation(Libs.truth_android)
  androidTestImplementation(Libs.espresso_core)

  androidTestRuntimeOnly(project(":runner"))

  // Obviously, these dependencies should be mostly "runtimeOnly",
  // but we have to override bundled APIs from the IDE as much as possible for Android Studio.
  testImplementation(Libs.junit_platform_engine)
  testImplementation(Libs.junit_platform_launcher)
  testImplementation(Libs.junit_jupiter_api)
  testImplementation(Libs.junit_jupiter_engine)
  testImplementation(Libs.junit_vintage_engine)
}

// ------------------------------------------------------------------------------------------------
// Deployment Setup
//
// Releases are pushed to jcenter via Bintray, while snapshots are pushed to Sonatype OSS.
// This section defines the necessary tasks to push new releases and snapshots using Gradle tasks.
// ------------------------------------------------------------------------------------------------

val deployConfig by extra<Deployed> { Artifacts.Instrumentation.Core }
apply(from = "$rootDir/gradle/deployment.gradle")
