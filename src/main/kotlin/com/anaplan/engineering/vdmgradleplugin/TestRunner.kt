/*
 * #%~
 * VDM Gradle Plugin
 * %%
 * Copyright (C) 2018 Anaplan Inc
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
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.overture.ast.definitions.SOperationDefinition
import org.overture.interpreter.runtime.ContextException
import org.overture.interpreter.runtime.Interpreter
import org.overture.interpreter.runtime.ModuleInterpreter
import java.io.File
import java.net.InetAddress
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal const val test = "test"

internal fun Project.addTestTask() {
    createVdmTask(test, VdmTestRunTask::class.java)
    afterEvaluate {
        val testTask = tasks.getByName(test) ?: throw GradleException("Cannot find VDM package task")
        testTask.dependsOn(typeCheckTests)
        val checkTask = tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
                ?: throw GradleException("Cannot find check task")
        checkTask.dependsOn(test)
    }
}

internal val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

open class VdmTestRunTask() : DefaultTask() {

    @TaskAction
    fun runTests() {
        val dialect = project.vdmConfig.dialect
        if (dialect != Dialect.vdmsl) {
            throw GradleException("Test running only defined for VDM-SL currently")
        }
        val interpreter = project.loadBinarySpecification()
        val testSuites = collectTests(interpreter)
        val testRunner = TestRunner(interpreter, logger)
        val testResults = testRunner.run(testSuites)
        saveFormattedResults(testResults)
        if (logger.isInfoEnabled) {
            if (testResults.all { it.succeeded }) {
                logger.info("SUCCESS -- ${testResults.sumBy { it.testCount }} tests passed")
            } else {
                logger.info("FAILURE -- ${testResults.sumBy { it.failCount }} tests failed, ${testResults.sumBy { it.errorCount }} tests had errors [${testResults.sumBy { it.testCount }} tests were run]")
            }
        }
        if (!testResults.all { it.succeeded }) {
            throw GradleException("There were test failures")
        }
    }

    private fun collectTests(interpreter: Interpreter): List<TestSuite> {
        if (!(interpreter is ModuleInterpreter)) {
            // this should never happen as we have limited dialect to VDM-SL
            throw GradleException("Interpreter is not a container interpreter!")
        }
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
        val reportDir = File(project.vdmBuildDir, "junitreports")
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
        val start = System.currentTimeMillis()
        return try {
            interpreter.execute("$testName()", null)
            logger.debug("PASS .. $moduleName`$testName")
            TestResult(testName, System.currentTimeMillis() - start, TestResultState.PASS)
        } catch (e: ContextException) {
            val duration = System.currentTimeMillis() - start
            // Error #4072 is invalid post-condition -- this is considered a test failure, any other exception (pre/inv) is an error
            if (e.number == 4072) {
                logger.info("FAIL .. $moduleName`$testName")
                TestResult(testName, duration, TestResultState.FAIL, e.message)
            } else {
                logger.info("ERRR .. $moduleName`$testName")
                TestResult(testName, duration, TestResultState.ERROR, e.message)
            }
        }
    }
}