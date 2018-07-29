# android-junit5 [![CircleCI](https://circleci.com/gh/mannodermaus/android-junit5/tree/master.svg?style=svg)][circleci]

![Logo](.images/logo.png)

A Gradle plugin that allows for the execution of [JUnit 5][junit5gh] tests in Android environments using **Android Gradle Plugin 3.2.0 or later.**

## How?

This plugin configures the unit test tasks for each build variant of a project to run on the JUnit Platform. Furthermore, it provides additional configuration options for these tests [through a DSL][wiki-dsl] attached to `android.testOptions`.

Instructions on how to write JUnit 5 tests can be found [in their User Guide][junit5ug].
Furthermore, this repository provides a small showcase of the functionality provided by JUnit 5 [here][sampletests].

## Download

```groovy
buildscript {
  dependencies {
    classpath "de.mannodermaus.gradle.plugins:android-junit5:1.2.0.0"
  }
}
```

Snapshots of the development version are available through [Sonatype's `snapshots` repository][sonatyperepo].

## Setup

```groovy
apply plugin: "de.mannodermaus.android-junit5"

dependencies {
  // (Required) Writing and executing Unit Tests on the JUnit Platform
  testImplementation "org.junit.jupiter:junit-jupiter-api:5.2.0"
  testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.2.0"

  // (Optional) If you need "Parameterized Tests"
  testImplementation "org.junit.jupiter:junit-jupiter-params:5.2.0"

  // (Optional) If you also have JUnit 4-based tests
  testImplementation "junit:junit:4.12"
  testRuntimeOnly "org.junit.vintage:junit-vintage-engine:5.2.0"
}
```

More information on Getting Started can be found [on the wiki][wiki-gettingstarted].

## Requirements

The latest version of this plugin requires:
* Android Gradle Plugin `3.2.0` or above
* Gradle `4.7` or above

## Instrumentation Test Support

There is experimental support for Android instrumentation tests, which requires some additional configuration & dependencies. Note that since JUnit 5 is built on Java 8 from the ground up, these libraries require you to have a `minSdkVersion` of at least `26`. It's recommended to use a product flavor for experimentation if you want to keep your global requirements below `26`. Check the sample for how this can be done!

To start writing instrumentation tests with JUnit Jupiter, add the following to your `build.gradle`:

```groovy
android {
  deaultConfig {
    // (Required) Make sure to use the AndroidJUnitRunner, of a subclass of it
    testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    // (Required) Connect JUnit 5 to the runner
    testInstrumentationRunnerArgument "runnerBuilder", "de.mannodermaus.junit5.AndroidJUnit5Builder"
  }
}
dependencies {
  // (Required) Writing tests for JUnit Jupiter
  androidTestImplementation "org.junit.jupiter:junit-jupiter-api:5.2.0"
  // (Required) The instrumentation test companion library
  androidTestImplementation "de.mannodermaus.junit5:android-instrumentation-test:0.2.2"

  // (Required) Runtime dependencies to orchestrate the execution on-device
  androidTestRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.2.0"
  androidTestRuntimeOnly "org.junit.platform:junit-platform-runner:1.2.0"
  androidTestRuntimeOnly "de.mannodermaus.junit5:android-instrumentation-test-runner:0.2.2"
}
```

# Migration from 1.0.x

Since the move to utilize the native JUnit 5 support built into the build system, a lot has changed from the previous version of the plugin. Users seeking to migrate to the new version are encouraged to check out the migration guide located [on the Wiki][wiki-migration] of this repository.

## License

```
Copyright 2017-2018 Marcel Schnelle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See also the [full License text](LICENSE).

 [junit5gh]: https://github.com/junit-team/junit5
 [junit5ug]: https://junit.org/junit5/docs/current/user-guide
 [circleci]: https://circleci.com/gh/mannodermaus/android-junit5
 [sonatyperepo]: https://oss.sonatype.org/content/repositories/snapshots
 [sampletests]: sample/src/test
 [wiki-dsl]: https://github.com/mannodermaus/android-junit5/wiki/Configuration-DSL
 [wiki-migration]: https://github.com/mannodermaus/android-junit5/wiki/Migrating-from-1.0.x
 [wiki-gettingstarted]: https://github.com/mannodermaus/android-junit5/wiki/Getting-Started