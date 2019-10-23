/*
 * #%~
 * VDM Gradle Plugin
 * %%
 * Copyright (C) 2018-9 Anaplan Inc
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #~%
 */
package com.anaplan.engineering.vdmgradleplugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.classloader.VisitableURLClassLoader
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.overture.ast.definitions.SOperationDefinition
import org.overture.interpreter.runtime.ContextException
import org.overture.interpreter.runtime.Interpreter
import org.overture.interpreter.runtime.ModuleInterpreter
import org.overture.interpreter.util.Delegate
import java.io.File
import java.net.InetAddress
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal const val test = "test"

internal fun Project.addTestTask() {
    createVdmTask(test, VdmTestRunTask::class.java)
    afterEvaluate {
        val testTask = tasks.getByName(test) ?: throw GradleException("Cannot find VDM test task")
        testTask.dependsOn(typeCheckTests)
        val checkTask = tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
                ?: throw GradleException("Cannot find check task")
        checkTask.dependsOn(test)
    }
}

internal val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

open class VdmTestRunTask() : DefaultTask() {

    val dialect: Dialect
        @Input
        get() = project.vdmConfig.dialect

    val recordCoverage: Boolean
        @Input
        get() = project.vdmConfig.recordCoverage

    val testLaunchGeneration: TestLaunchGeneration
        @Input
        get() = project.vdmConfig.testLaunchGeneration

    val generatedLibFile: File
        @InputFile
        get() = project.generatedLibFile

    val reportDir: File
        @OutputDirectory
        get() = File(project.vdmBuildDir, "junitreports")

    val coverageDir: File
        @OutputDirectory
        get() = File(project.vdmBuildDir, "coverage")

    val launchDir: File
        @OutputDirectory
        get() = File(project.vdmBuildDir, "testLaunch")

    @TaskAction
    fun runTests() {
        if (dialect != Dialect.vdmsl) {
            throw GradleException("Test running only defined for VDM-SL currently")
        }
        makeVdmLibsAvailableForDelegation()
        val interpreter = loadSpecification()
        val testSuites = collectTests(interpreter)
        val testRunner = TestRunner(interpreter, logger)
        val testResults = testRunner.run(testSuites)
        saveFormattedResults(testResults)
        logTestResults(testResults)
        if (recordCoverage) {
            CoverageRecorder(project.vdmSourceDir, coverageDir, logger).recordCoverage(interpreter)
        }
        generateLaunchFiles(testResults)
        if (!testResults.all { it.succeeded }) {
            throw GradleException("There were test failures")
        }
    }

    private fun makeVdmLibsAvailableForDelegation() {
        // Requires a dependence on Gradle internal, but alternative is completely separate JVM which is far
        // more work
        locateFilesWithExtension(project.vdmLibDir, "jar").map { it.toURI().toURL() }.forEach { url ->
            (Delegate::class.java.classLoader as VisitableURLClassLoader).addURL(url)
        }
    }

    private fun generateLaunchFiles(testResults: List<TestSuiteResult>) {
        if (launchDir.exists()) {
            deleteDirectory(launchDir)
        }
        launchDir.mkdirs()
        if (testLaunchGeneration == TestLaunchGeneration.NONE) {
            return
        }
        testResults.forEach { suite ->
            suite.testResults.filter { test ->
                testLaunchGeneration == TestLaunchGeneration.ALL || test.state != TestResultState.PASS
            }.forEach { test ->
                val xml = testLaunch {
                    stringAttribute {
                        key = "vdm_launch_config_default"
                        value = suite.moduleName
                    }
                    booleanAttribute {
                        key = "vdm_launch_config_dtc_checks"
                        value = true
                    }
                    stringAttribute {
                        key = "vdm_launch_config_expression"
                        value = "${suite.moduleName}`${test.testName}()"
                    }
                    stringAttribute {
                        key = "vdm_launch_config_project"
                        value = project.name
                    }
                    stringAttribute {
                        key = "vdm_launch_config_method"
                        value = "${test.testName}()"
                    }
                    stringAttribute {
                        key = "vdm_launch_config_module"
                        value = suite.moduleName
                    }
                    booleanAttribute {
                        key = "vdm_launch_config_inv_checks"
                        value = true
                    }
                    booleanAttribute {
                        key = "vdm_launch_config_pre_checks"
                        value = true
                    }
                    booleanAttribute {
                        key = "vdm_launch_config_post_checks"
                        value = true
                    }
                    booleanAttribute {
                        key = "vdm_launch_config_measure_checks"
                        value = true
                    }
                }
                File(launchDir, "${suite.moduleName}`${test.testName}.launch").writeText(xml)
            }
        }
    }

    private fun logTestResults(testResults: List<TestSuiteResult>) {
        if (logger.isInfoEnabled) {
            if (testResults.all { it.succeeded }) {
                logger.info("SUCCESS -- ${testResults.sumBy { it.testCount }} tests passed")
            } else {
                logger.info("FAILURE -- ${testResults.sumBy { it.failCount }} tests failed, ${testResults.sumBy { it.errorCount }} tests had errors [${testResults.sumBy { it.testCount }} tests were run]")
            }
        }
    }

    private fun loadSpecification() =
            if (recordCoverage) {
                // For coverage we need to reparse to correctly identify lex locations in files
                val controller = dialect.createController()
                controller.parse(project.locateAllSpecifications(dialect, true).toList())
                controller.typeCheck()
                controller.getInterpreter()
            } else {
                project.loadBinarySpecification(generatedLibFile)
            } as? ModuleInterpreter
                    ?: // this should never happen as we have limited dialect to VDM-SL
                    throw GradleException("Interpreter is not a container interpreter!")


    private fun collectTests(interpreter: ModuleInterpreter): List<TestSuite> {
        val testModules = interpreter.modules.filter { module ->
            module.files.all { it.startsWith(project.vdmTestSourceDir) } &&
                    module.name.name.startsWith("Test")
        }
        return testModules.map { module ->
            val operationDefs = module.defs.filter { it is SOperationDefinition }.map { it as SOperationDefinition }
            // TODO - should check that operation has zero params
            val testNames = operationDefs.filter { it.name.name.startsWith("Test") }.map { it.name.name }
            TestSuite(module.name.name, testNames)
        }
    }

    private fun saveFormattedResults(testSuiteResults: List<TestSuiteResult>) {
        if (reportDir.exists()) {
            deleteDirectory(reportDir)
        }
        reportDir.mkdirs()
        testSuiteResults.forEach { saveFormattedResults(it, reportDir) }
    }

    private fun saveFormattedResults(testSuiteResult: TestSuiteResult, reportDir: File) {
        val reportFile = File(reportDir, "TEST-${testSuiteResult.moduleName}.xml")
        val xml = testSuite {
            setTime(testSuiteResult.duration)
            name = testSuiteResult.moduleName
            tests = testSuiteResult.testCount
            failures = testSuiteResult.failCount
            errors = testSuiteResult.errorCount
            timestamp = timestampFormatter.format(testSuiteResult.timestamp)
            hostname = InetAddress.getLocalHost().hostName
            testSuiteResult.testResults.forEach { testResult ->
                testCase {
                    setTime(testResult.duration)
                    name = testResult.testName
                    classname = testSuiteResult.moduleName
                    if (testResult.state == TestResultState.FAIL) {
                        failure {
                            message = testResult.message
                        }
                    }
                    if (testResult.state == TestResultState.ERROR) {
                        error {
                            message = testResult.message
                        }
                    }
                }
            }
        }
        Files.write(reportFile.toPath(), xml.toByteArray())
    }


}

private data class TestSuite(
        val moduleName: String,
        val testNames: List<String>
)

private enum class TestResultState {
    PASS,
    FAIL,
    ERROR
}

private data class TestResult(
        val testName: String,
        val duration: Long,
        val state: TestResultState,
        val message: String? = null
)

private data class TestSuiteResult(
        val moduleName: String,
        val timestamp: LocalDateTime,
        val testResults: List<TestResult>
) {
    val errorCount: Int by lazy {
        testResults.count { it.state == TestResultState.ERROR }
    }
    val failCount: Int by lazy {
        testResults.count { it.state == TestResultState.FAIL }
    }
    val testCount: Int by lazy {
        testResults.size
    }
    val duration: Long by lazy {
        testResults.sumByLong { it.duration }
    }
    val succeeded: Boolean by lazy {
        testResults.all { it.state == TestResultState.PASS }
    }
}

// copies _Collections.sumBy for long
private inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

private enum class ExpectedTestResult(val description: String) {
    success("success"),
    failedPrecondition("precondition failure"),
    failedPostcondition("postcondition failure"),
    failedInvariant("invariant failure")
}

// ideally this would be done through an annotation, but this is not feasible currently
private fun getExpectedResult(testName: String): ExpectedTestResult {
    val testNameLower = testName.toLowerCase()
    return if (testNameLower.toLowerCase().contains("expectpreconditionfailure")) {
        ExpectedTestResult.failedPrecondition
    } else if (testNameLower.contains("expectpostconditionfailure")) {
        ExpectedTestResult.failedPostcondition
    } else if (testNameLower.contains("expectinvariantfailure")) {
        ExpectedTestResult.failedInvariant
    } else {
        ExpectedTestResult.success
    }
}

private val preconditionFailureCodes = setOf(4055, 4071)
private val postconditionFailureCodes = setOf(4056, 4072)
private val invariantFailureCodes = setOf(4060, 4079, 4082)

private class TestRunner(private val interpreter: Interpreter, private val logger: Logger) {

    internal fun run(testSuites: List<TestSuite>): List<TestSuiteResult> {
        interpreter.init(null)
        val timestamp = LocalDateTime.now()
        return testSuites.map { run(it, timestamp) }
    }

    private fun run(testSuite: TestSuite, timestamp: LocalDateTime): TestSuiteResult {
        interpreter.defaultName = testSuite.moduleName
        val testResults = testSuite.testNames.map { run(testSuite.moduleName, it) }
        return TestSuiteResult(testSuite.moduleName, timestamp, testResults)
    }

    private fun run(moduleName: String, testName: String): TestResult {
        val expectedResult = getExpectedResult(testName)
        val start = System.currentTimeMillis()
        return try {
            interpreter.execute("$testName()", null)
            val duration = System.currentTimeMillis() - start
            if (expectedResult == ExpectedTestResult.success) {
                logger.debug("PASS .. $moduleName`$testName")
                TestResult(testName, duration, TestResultState.PASS)
            } else {
                val msg = "test passed, but expected ${expectedResult.description}"
                logger.info("FAIL .. $moduleName`$testName -- $msg")
                TestResult(testName, duration, TestResultState.FAIL, msg)
            }
        } catch (e: ContextException) {
            val duration = System.currentTimeMillis() - start
            // Post condition failure is considered a test failure, any other unexpected exception (pre/inv) is an error
            if (postconditionFailureCodes.contains(e.number)) {
                if (expectedResult == ExpectedTestResult.failedPostcondition) {
                    logger.debug("PASS .. $moduleName`$testName")
                    TestResult(testName, duration, TestResultState.PASS)
                } else {
                    logger.info("FAIL .. $moduleName`$testName")
                    TestResult(testName, duration, TestResultState.FAIL, e.message)
                }
            } else if (preconditionFailureCodes.contains(e.number) && expectedResult == ExpectedTestResult.failedPrecondition) {
                logger.debug("PASS .. $moduleName`$testName")
                TestResult(testName, duration, TestResultState.PASS)
            } else if (invariantFailureCodes.contains(e.number) && expectedResult == ExpectedTestResult.failedInvariant) {
                logger.debug("PASS .. $moduleName`$testName")
                TestResult(testName, duration, TestResultState.PASS)
            } else {
                logger.info("ERRR .. $moduleName`$testName")
                TestResult(testName, duration, TestResultState.ERROR, e.message)
            }
        }
    }
}