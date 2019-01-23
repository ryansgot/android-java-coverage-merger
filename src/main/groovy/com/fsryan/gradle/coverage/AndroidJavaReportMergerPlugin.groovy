package com.fsryan.gradle.coverage

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.slf4j.LoggerFactory

class AndroidJavaReportMergerPlugin implements Plugin<Project> {

    private static final LOGGER = LoggerFactory.getLogger(AndroidJavaReportMergerPlugin.class)

    void apply(Project project) {
        final NamedDomainObjectContainer<ClassFilter> classFilters = project.container(ClassFilter)
        project.extensions.create("mergedReportConfig", MergedReportConfigExtension, classFilters)

        project.afterEvaluate {

            Task mergedTestReportsTask = project.tasks.create(name: "createMergedReports", description: "create merged jacoco test report for each build variant", group: "reporting")

            def variants = project.plugins.hasPlugin("com.android.application") ? project.android.applicationVariants : project.android.libraryVariants 

            variants.each { variant ->
                def mergeTask
                def configureTask
                if (variant.buildType.testCoverageEnabled) {
                    mergeTask = mergeTaskForVariant(project, variant)
                    configureTask = configureTaskForVariant(project, variant)
                    configureTask.dependsOn(mergeTask)
                    mergedTestReportsTask.dependsOn(configureTask)
                }

            }
        }
    }

    private static Task mergeTaskForVariant(Project project, variant) {
        Task mergeTask = project.tasks.create("merge${variant.name.capitalize()}Report", JacocoMerge)
        mergeTask.dependsOn("create${variant.name.capitalize()}CoverageReport")
        mergeTask.dependsOn("test${variant.name.capitalize()}UnitTest")
        mergeTask.group = 'Reporting'
        mergeTask.description = "merge android and java jacoco test reports for $variant.name variant"
        mergeTask.destinationFile = mergedExecFileForVariant(project, variant)
        mergeTask.executionData = project.files(javaTestExecutionDataForVariant(project, variant))
        mergeTask.doFirst {
            List<String> androidExecFiles = new FileNameByRegexFinder().getFileNames(project.buildDir.absolutePath + File.separator + '', /.*\.ec/)
            executionData = executionData + project.files(androidExecFiles)
        }
        return mergeTask
    }

    private static Task configureTaskForVariant(Project project, variant) {
        Task configureTask = project.tasks.create("createMerged${variant.name.capitalize()}Report", JacocoReport)
        configureTask.group = 'Reporting'
        configureTask.description = "merged jacoco for $variant.name variant"

        MergedReportConfigExtension config = project.extensions.getByType(MergedReportConfigExtension)

        LOGGER.debug("${variant.name} includes: ${config.includesFor(variant)}")
        LOGGER.debug("${variant.name} excludes: ${config.excludesFor(variant)}")

        configureTask.executionData = project.files(mergedExecFileForVariant(project, variant))
        configureTask.classDirectories = project.fileTree(
                dir: project.tasks.findByName("compile${variant.name.capitalize()}JavaWithJavac").destinationDir,
                includes: config.includesFor(variant),
                excludes: config.excludesFor(variant)
        )
        if (isKotlin(project)) {
            configureTask.classDirectories += project.fileTree(
                    dir: project.tasks.findByName("compile${variant.name.capitalize()}Kotlin").destinationDir,
                    includes: config.includesFor(variant),
                    excludes: config.excludesFor(variant)
            )
        }
        configureTask.reports {
            xml.enabled = true
            html.enabled = true
            csv.enabled = false
        }

        List<File> sourceDirectories = new ArrayList<>()
        variant.sourceSets.each { ss ->
            sourceDirectories.addAll(ss.javaDirectories)
        }
        LOGGER.debug("${variant.name} source directories: ${sourceDirectories}")
        configureTask.sourceDirectories = project.files(sourceDirectories)

        return configureTask
    }
    
    private static File javaTestExecutionDataForVariant(Project project, variant) {
        return new File(project.buildDir.absolutePath + File.separator + 'jacoco' + File.separator + "test${variant.name.capitalize()}UnitTest.exec")
    }

    private static File mergedExecFileForVariant(Project project, variant) {
        if (variant.flavorName == null || variant.flavorName.isEmpty()) {
            return new File(project.buildDir.absolutePath + File.separator + 'jacoco' + File.separator + variant.buildType.name + File.separator + 'jacoco_merged.exec')
        }
        return new File(project.buildDir.absolutePath + File.separator + 'jacoco' + File.separator + variant.flavorName + File.separator + variant.buildType.name + File.separator + 'jacoco_merged.exec')
    }

    private static boolean isKotlin(Project project) {
        return project.plugins.findPlugin('kotlin-android') != null
    }
}
