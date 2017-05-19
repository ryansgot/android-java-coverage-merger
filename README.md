# android-java-coverage-merger
Gradle plugin for adding tasks that merge jacoco execution data for tests running on the Android device and on the local JVM

[ ![Download](https://api.bintray.com/packages/ryansgot/maven/android-java-coverage-merger/images/download.svg) ](https://bintray.com/ryansgot/maven/android-java-coverage-merger/_latestVersion)

## What problem does this solve?
To adequately test an Android application, one must usually run tests both on the DVM (on an Android device) and on a
JVM. Without an external tool such as this one, you would get separate reports for both kinds of tests. This plugin
solves the two-reports problem by merging them

## How does it work?
For each build variant that has testCoverageEnabled, an associated task is added to run the tests, then generate the
appropriate merged reports from both the local java (JVM) and tests run on the device (DVM). The correct task 
dependencies are created such that you must only run one gradle task in order to test on the JVM and DVM and generate
the merged report.

## Sample
Add the following config to your projects base build.gradle
```groovy
buildscript {
    repositories {
        maven {
            url  "http://dl.bintray.com/ryansgot/maven"
        }
    }
    classpath {
        classpath 'com.android.tools.build:gradle:2.3.1'
        classpath 'com.fsryan.gradle.coverage:android-java-coverage-merger:x.y.z'
    }
}
```
You must first apply the jacoco plugin in any (sub)project for which you want to create combined reports.
```groovy
apply plugin: 'com.android.application' // or com.android.library
apply plugin: 'jacoco'
apply plugin: 'android-java-coverage-merger'

```
With the below in your build.gradle, you'll receive the following tasks:
- createMergedFreeDebugReport - the task you should run when you want to test the free debug variant
- mergeFreeDebugReport - the merging task for the free debug variant
- createMergedPaidDebugReport - the task you should run when you want to test the paid debug variant
- mergePaidDebugReport - the merging task for the paid debug variant
- createMergedReports - the task you should run when you want to test all variants

(Note that if your project does not have flavors, then the above tasks will be generated without the flavors) 
```groovy

android {
    buildTypes {
        debug {
            testCoverageEnabled true
        }
    }
    productFlavors {
        free {
        }
        paid {
        }
    }
}
```
In order to filter the classes on which Jacoco reports in the merged report, you can also filter by build type and/or product flavor.
```groovy
mergedReportConfig {
    classFilters {
        // buildType + flavor matches are additive rather than overriding
        debug {
            includes = ['**/com/my/app/base/package/**']
            excludes = [
                    '**/R$*.class',                                 // generated R subclasses
                    '**/R.class',                                   // generated R classes
                    '**/*Test.class',                               // filter test classes
                    '**/BuildConfig*',                              // generated BuildConfig classes

                    '**/*_*.class',                                 // Butterknife/AutoValue/Dagger-generated classes
                    '**/Dagger*.class',                             // Dagger-generated classes
            ]
        }
        free {
            excludes = ["**/App.class"]                             // free flavor debug report additionally filters any class called App
        }
    }
}
```
Note that the sources included in each report will be the same as the sources for the variant. Therefore, if you have different sources for the free and paid flavors, then make sure to run the appropriate test task.

Finally, in order to to a combined run of local JVM and connected android testing that generates the merged report for the free debug variant,
```
$ ./gradlew clean createMergedFreeDebugReport
```
And the same for the paid debug variant"
```
$ ./gradlew clean createMergedPaidDebugReport
```
