package com.anaplan.engineering.vdmgradleplugin

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

class generateLaunchGenForAllTestUpToDateTest {

    val mavenRepo = Files.createTempDirectory("maven")

    @Test
    fun dependencyUnpack_firstSucceedsSecondUpToDate_noDependencies() {
        val project = createProject()
        runAndCheck(project, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun dependencyUnpack_firstSucceedsSecondUpToDate_withDependency() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun dependencyUnpack_dependencyUpdated() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun dependencyUnpack_OneOfManyDependenciesUpdated() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project2, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project3 = createProject(moduleName = "C", config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project3, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project4 = createProject(moduleName = "D", config = createDependencyConfig(
                dependencies = listOf(
                        Dependency(project1, "1.0.0"),
                        Dependency(project2, "1.0.0"),
                        Dependency(project3, "1.0.0")
                )))
        runAndCheck(project4, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        project4.buildGradle.writeText(createDependencyConfig(
                dependencies = listOf(
                        Dependency(project1, "1.0.1"),
                        Dependency(project2, "1.0.0"),
                        Dependency(project3, "1.0.0")
                )))
        runAndCheck(project4, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun dependencyUnpack_touchConfig_noChanges() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE
        ))
    }

    // We have no way of knowing whether our snapshot is the latest without resolving the dependency, so always unpack if
    // we have a snapshot
    @Test
    fun dependencyUnpack_snapshotNeverUptodate_oneOfMany() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0-SNAPSHOT"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project2, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project3 = createProject(moduleName = "C", config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project3, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project4 = createProject(moduleName = "D", config = createDependencyConfig(
                dependencies = listOf(
                        Dependency(project1, "1.0.0-SNAPSHOT"),
                        Dependency(project2, "1.0.0"),
                        Dependency(project3, "1.0.0")
                )))
        runAndCheck(project4, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        runAndCheck(project4, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun dependencyUnpack_snapshotNeverUptodate() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0-SNAPSHOT"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0-SNAPSHOT"))))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
        runAndCheck(project2, Task.dependencyUnpack, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheck_firstSucceedsSecondUpToDate() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_addSpecFile() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "b.vdmsl").writeText(createModule("B", "b"))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheck_addIrrelevantFile() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "c.text").writeText("hello, world")
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_addSpecDifferentDialect() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "c.vdmpp").writeText(createClass(className = "C", valueName = "c"))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_addSpecDifferentDialect_changeDialect() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "c.vdmpp").writeText(createClass(className = "C", valueName = "c"))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
        project.buildGradle.writeText("$useVdmPlugin\n\n$useVdmPp")
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheck_removeSpecFile() {
        val project = createProject()
        val b = File(project.mainVdmDir, "b.vdmsl")
        b.writeText(createModule("B", "b"))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        Files.delete(b.toPath())
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheck_removeIrrelevantFile() {
        val project = createProject()
        val c = File(project.mainVdmDir, "c.text")
        c.writeText("hello, world")
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        Files.delete(c.toPath())
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_changeSpecFile() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheck_touchSpecFileNoChanges() {
        val project = createProject()
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "a"))
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_changeIrrelevantFile() {
        val project = createProject()
        val c = File(project.mainVdmDir, "c.text")
        c.writeText("hello, world")
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        c.writeText("foo, bar")
        runAndCheck(project, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_dependencyChangesButContentDoesnt() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheck_dependencyChangesAndMainContentDoes() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheck_dependencyChangesAndOnlyTestContentDoes() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createTestModule = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "b"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createTestModule = true)
        runAndCheck(project2, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.typeCheck, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheck to TaskOutcome.UP_TO_DATE
        ))
    }


    @Test
    fun typeCheckTests_firstSucceedsSecondUpToDate() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_addSpecFile() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "b.vdmsl").writeText(createModule("B", "b"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_addTestFile() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "b.vdmsl").writeText(createModule("B", "b"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_addIrrelevantFile() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "c.text").writeText("hello, world")
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_addSpecDifferentDialect() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "C.vdmpp").writeText(createClass(className = "C", valueName = "c"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_addSpecDifferentDialect_changeDialect() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "c.vdmpp").writeText(createClass(className = "C", valueName = "c"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
        project.buildGradle.writeText("$useVdmPlugin\n\n$useVdmPp")
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_addTestDifferentDialect() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestC.vdmpp").writeText(createClass(className = "TestC", valueName = "c"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_addTestDifferentDialect_changeDialect() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestC.vdmpp").writeText(createClass(className = "TestC", valueName = "c"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
        project.buildGradle.writeText("$useVdmPlugin\n\n$useVdmPp")
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_removeSpecFile() {
        val project = createProject(createTestModule = true)
        val b = File(project.mainVdmDir, "b.vdmsl")
        b.writeText(createModule("B", "b"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        Files.delete(b.toPath())
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_removeTestFile() {
        val project = createProject(createTestModule = true)
        val b = File(project.testVdmDir, "b.vdmsl")
        b.writeText(createModule("B", "b"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        Files.delete(b.toPath())
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_removeIrrelevantFile() {
        val project = createProject(createTestModule = true)
        val c = File(project.mainVdmDir, "c.text")
        c.writeText("hello, world")
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        Files.delete(c.toPath())
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_changeSpecFile() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_changeTestFile() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "c"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_touchSpecFileNoChanges() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "a"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_touchTestFileNoChanges() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "a"))
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_changeIrrelevantFile() {
        val project = createProject(createTestModule = true)
        val c = File(project.mainVdmDir, "c.text")
        c.writeText("hello, world")
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        c.writeText("foo, bar")
        runAndCheck(project, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_dependencyChangesButContentDoesnt() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createTestModule = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createTestModule = true)
        runAndCheck(project2, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun typeCheckTests_dependencyChangesAndMainContentDoes() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createTestModule = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createTestModule = true)
        runAndCheck(project2, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun typeCheckTests_dependencyChangesAndOnlyTestContentDoes() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createTestModule = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "b"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createTestModule = true)
        runAndCheck(project2, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.typeCheckTests, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun test_firstSucceedsSecondUpToDate() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.test to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun test_serializedSpecsChange_main() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun test_serializedSpecsChange_test() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "b"))
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun test_serializedSpecsChange_dependencyChangesButNoLibChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.test to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun test_serializedSpecsChange_dependencyChangesWithLibChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun test_dialectChanges() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.SUCCESS
        ))
        project.buildGradle.writeText("$useVdmPlugin\n\n$useVdmPp")
        // test running not allowed for VDM++
        runAndCheck(project, Task.test, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.test to TaskOutcome.FAILED
        ), expectBuildFailure = true)
    }

    @Test
    fun docGen_firstSucceedsSecondUpToDate() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun docGen_markdownAdded() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        File(project.mdDir, "other.md").writeText("# other stuff!")
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_markdownChanged() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        File(project.mdDir, "index.md").writeText("# foo bar!")
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_markdownRemoved() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        Files.delete(File(project.mdDir, "index.md").toPath())
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_dependencyChangesButNoMdChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createMarkdown = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createMarkdown = true)
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun docGen_dependencyChangesMdChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createMarkdown = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.mdDir, "index.md").writeText("# foo bar!")
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createMarkdown = true)
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_dependencyChangesResourceChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"), createMarkdown = true)
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.mdDir, "other.png").writeText("this is an image, honest!")
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))), createMarkdown = true)
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_serializedSpecsChange_main() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_serializedSpecsChange_test() {
        val project = createProject(createTestModule = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "b"))
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_serializedSpecsChange_dependencyChangesButNoLibChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun docGen_serializedSpecsChange_dependencyChangesWithLibChange() {
        val project1 = createProject(config = createPublishConfig(version = "1.0.0"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        project1.buildGradle.writeText(createPublishConfig(version = "1.0.1"))
        File(project1.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project1, Task.publish, emptyMap(), failOnUnexpectedTask = false)
        val project2 = createProject(moduleName = "B", config = createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.0"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project2.buildGradle.writeText(createDependencyConfig(dependencies = listOf(Dependency(project1, "1.0.1"))))
        runAndCheck(project2, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_resourceAdded() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        File(project.mdDir, "other.png").writeText("this is an image, honest!")
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docGen_nonResourceAdded() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        File(project.mdDir, "other.doc").writeText("this is a doc!")
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun docGen_prettyPrinterConfigChanges_logUnhandledChanges() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project.buildGradle.writeText(useVdmPlugin + """
            |
            |vdm {
            |   prettyPrinter {
            |       logUnhandledCases true
            |   }
            |}
            """.trimMargin())
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }
    @Test
    fun docGen_prettyPrinterConfigChanges_minListLengthToUseNls() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        project.buildGradle.writeText(useVdmPlugin + """
            |
            |vdm {
            |   prettyPrinter {
            |       minListLengthToUseNls 5
            |   }
            |}
            """.trimMargin())
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS
        ))
        project.buildGradle.writeText(useVdmPlugin + """
            |
            |vdm {
            |   prettyPrinter {
            |       minListLengthToUseNls 10
            |   }
            |}
            """.trimMargin())
        runAndCheck(project, Task.docGen, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun docPackage_firstSucceedsSecondUpToDate() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docPackage, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.docPackage to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.docPackage, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE,
                Task.docPackage to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun docPackage_generatedDocChanges() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.docPackage, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.docPackage to TaskOutcome.SUCCESS
        ))
        File(project.mdDir, "index.md").writeText("# foo bar!")
        runAndCheck(project, Task.docPackage, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.docPackage to TaskOutcome.SUCCESS
        ))
    }


    @Test
    fun package_firstSucceedsSecondUpToDate() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE,
                Task.`package` to TaskOutcome.UP_TO_DATE
        ))
    }

    @Test
    fun package_configChanges_mdSource() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        project.buildGradle.writeText(useVdmPlugin + """
            |
            |vdm {
            |   packaging {
            |       mdSource false
            |   }
            |}
            """.trimMargin())
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
        project.buildGradle.writeText(useVdmPlugin + """
            |
            |vdm {
            |   packaging {
            |       mdSource true
            |   }
            |}
            """.trimMargin())
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE,
                Task.`package` to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun package_configChanges_testSource() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
        project.buildGradle.writeText(useVdmPlugin + """
            |
            |vdm {
            |   packaging {
            |       testSource false
            |   }
            |}
            """.trimMargin())
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.UP_TO_DATE,
                Task.`package` to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun package_changeSpecFile() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
        File(project.mainVdmDir, "A.vdmsl").writeText(createModule("A", "b"))
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun package_changeTestFile() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
        File(project.testVdmDir, "TestA.vdmsl").writeText(createModule("TestA", "b"))
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
    }

    @Test
    fun package_changeMdFile() {
        val project = createProject(createTestModule = true, createMarkdown = true)
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.SUCCESS,
                Task.typeCheckTests to TaskOutcome.SUCCESS,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
        File(project.mdDir, "index.md").writeText("# foo bar!")
        runAndCheck(project, Task.`package`, mapOf(
                Task.dependencyUnpack to TaskOutcome.UP_TO_DATE,
                Task.typeCheckTests to TaskOutcome.UP_TO_DATE,
                Task.docGen to TaskOutcome.SUCCESS,
                Task.`package` to TaskOutcome.SUCCESS
        ))
    }

    private fun runAndCheck(project: Project, taskToRun: Task, expected: Map<Task, TaskOutcome>, expectBuildFailure: Boolean = false, failOnUnexpectedTask: Boolean = true) {
        val result = executeBuild(projectDir = project.dir, clean = false, tasks = arrayOf(taskToRun.name), fail = expectBuildFailure)
        result.tasks.forEach { taskResult ->
            val task = Task.fromPath(taskResult.path)
            if (expected.containsKey(task)) {
                Assert.assertEquals("Unexpected outcome for ${task.name}", expected.get(task), taskResult.outcome)
            } else if (failOnUnexpectedTask) {
                Assert.fail("Task ${task.name} run but outcome not checked")
            }
        }
    }

    private fun createClass(className: String, valueName: String) = """
        |class $className
        |
        |values
        |
        |$valueName : nat = 1
        |
        |end $className
    """.trimMargin()

    private fun createModule(moduleName: String, valueName: String) = """
        |module $moduleName
        |
        |definitions
        |
        |values
        |
        |$valueName : nat = 1
        |
        |end $moduleName
    """.trimMargin()

    private val useVdmPlugin = """
        |plugins {
        |    id("vdm")
        |}
        |
        |apply plugin: 'vdm'
    """.trimMargin()

    private val useVdmPp = """
        |vdm {
        |    dialect 'vdmpp'
        |}
    """.trimMargin()

    private fun createPublishConfig(version: String) = """
        |$useVdmPlugin
        |
        |group = "testing"
        |version = "$version"
        |
        |apply plugin: 'maven-publish'
        |
        |publishing {
        |    repositories {
        |        maven {
        |            url 'file://${mavenRepo.toFile().absolutePath}'
        |        }
        |    }
        |}
        """.trimMargin()

    private fun createDependencyConfig(dependencies: List<Dependency>): String {
        val dependencyDeclarations = dependencies.map { dependency ->
            "\tvdm group: 'testing', name: '${dependency.project.name}', version: '${dependency.version}'"
        }.joinToString("\n")
        return """
            |$useVdmPlugin
            |
            |repositories {
            |    maven {
            |        url 'file://${mavenRepo.toFile().absolutePath}'
            |    }
            |}
            |
            |dependencies {
            |   $dependencyDeclarations
            |}
            """.trimMargin()
    }

    private fun createProject(
            moduleName: String = "A",
            valueName: String = moduleName.toLowerCase(),
            config: String = useVdmPlugin,
            createMainModule: Boolean = true,
            createTestModule: Boolean = false,
            createMarkdown: Boolean = false
    ): Project {
        val project = Project()
        project.buildGradle.writeText(config)
        if (createMainModule) {
            project.mainVdmDir.mkdirs()
            File(project.mainVdmDir, "$moduleName.vdmsl").writeText(createModule(moduleName, valueName))
        }
        if (createTestModule) {
            project.testVdmDir.mkdirs()
            File(project.testVdmDir, "Test$moduleName.vdmsl").writeText(createModule("Test$moduleName", valueName))
        }
        if (createMarkdown) {
            project.mdDir.mkdirs()
            File(project.mdDir, "index.md").writeText("# hello, world!")
        }
        return project
    }

    private data class Project(
            val dir: File = Files.createTempDirectory("uptodate").toFile(),
            val buildGradle: File = File(dir, "build.gradle"),
            val mainVdmDir: File = File(dir, "src/main/vdm"),
            val testVdmDir: File = File(dir, "src/test/vdm"),
            val mdDir: File = File(dir, "src/main/md"),
            val name: String = dir.name
    )

    private data class Dependency(
            val project: Project,
            val version: String
    )

    private enum class Task {
        dependencyUnpack,
        typeCheck,
        typeCheckTests,
        test,
        `package`,
        AddVdmArtifacts,
        generatePomFileForVdmPublication,
        AddVdmDependenciesToPom,
        publishVdmPublicationToMavenRepository,
        assemble,
        docGen,
        docPackage,
        publish;

        companion object {
            fun fromPath(path: String) = Task.valueOf(path.drop(1))
        }
    }
}

