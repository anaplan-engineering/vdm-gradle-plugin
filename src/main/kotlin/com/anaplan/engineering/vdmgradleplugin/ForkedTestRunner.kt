package com.anaplan.engineering.vdmgradleplugin

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import org.overture.ast.definitions.SOperationDefinition
import org.overture.interpreter.runtime.ContextException
import org.overture.interpreter.runtime.Interpreter
import org.overture.interpreter.runtime.ModuleInterpreter
import java.io.File
import java.io.PrintStream
import java.net.InetAddress
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

internal val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun main(args: Array<String>) {
    try {
        ForkedTestRunner(ArgParser(args)).runTests()
    } catch (e: SystemExitException) {
        e.printAndExit()
    }
    exitProcess(0)
}

/**
 * There are some issues with logging in a way that is consistent with the build. If we use a logger here, we have
 * difficulty maintaining consistency with parent process. For this reason we use the gradle log level but simply output
 * to stdout/stderr according to that level.
 */
internal class Logger(private val logLevel: GradleLogLevel) {

    fun error(msg: String) = log(msg, GradleLogLevel.ERROR, System.err)
    fun info(msg: String) = log(msg, GradleLogLevel.INFO, System.out)
    fun debug(msg: String) = log(msg, GradleLogLevel.DEBUG, System.out)

    private fun log(msg: String, at: GradleLogLevel, printStream: PrintStream) {
        if (at.level >= logLevel.level) {
            printStream.println(msg)
        }
    }

}

internal enum class GradleLogLevel(val level: Int) {
    DEBUG(1),
    INFO(2),
    LIFECYCLE(3),
    WARN(4),
    QUIET(5),
    ERROR(6)
}

/**
 * This class is used to run VDM tests in a separate JVM and is invoked using main method above.
 */
// TODO - generic test runner could be extracted to a separate project
class ForkedTestRunner(parser: ArgParser) {

    private val logLevel by parser.storing("The logging level") {
        GradleLogLevel.valueOf(this)
    }.default(GradleLogLevel.ERROR)

    private val dialect by parser.storing("The VDM dialect") {
        Dialect.valueOf(this)
    }.default(Dialect.vdmsl)

    private val coverageTargetDir by parser.storing("Directory to write coverage results") {
        File(this)
    }.default { null }

    private val reportTargetDir by parser.storing("Directory to write test reports") {
        File(this)
    }

    private val launchTargetDir by parser.storing("Directory to write generated test launch files") {
        File(this)
    }.default { null }

    private val testLaunchGeneration by parser.storing("Generate test launch files for all, failing or no tests") {
        TestLaunchGeneration.valueOf(this)
    }.default { TestLaunchGeneration.FAILING }

    private val testLaunchProjectName by parser.storing("Project name required for test launch").default { null }

    private val coverageSourceDir by parser.storing("Limit coverage to files in the specified folder") {
        File(this)
    }.default { null }

    private val testSourceDir by parser.storing("Limit tests to those found in files in the specified folder") {
        File(this)
    }.default { null }

    private val specificationFiles by parser.positionalList("Specification files") {
        File(this)
    }

    private val logger = Logger(logLevel)

    fun runTests() {
        if (dialect != Dialect.vdmsl) {
            logger.error("Test running only defined for VDM-SL currently")
            exitProcess(1)
        }
        val interpreter = loadSpecification()
        val testSuites = collectTests(interpreter)
        val testResults = TestRunner(interpreter, logger).run(testSuites)
        saveFormattedResults(testResults)
        logTestResults(testResults)
        if (coverageTargetDir != null) {
            // TODO -- coverage need to be able to distinguish between code under tests and dependencies
            CoverageRecorder(coverageSourceDir, coverageTargetDir!!, logger).recordCoverage(interpreter)
        }
        generateLaunchFiles(testResults)

        if (!testResults.all { it.succeeded }) {
            logger.error("There were failing tests")
            exitProcess(1)
        }
    }

    private fun generateLaunchFiles(testResults: List<TestSuiteResult>) {
        if (launchTargetDir == null) {
            if (testLaunchGeneration != TestLaunchGeneration.NONE) {
                logger.error("Asked to generate launch files, but no launch target directory specified")
                exitProcess(1)
            }
            return
        }
        if (launchTargetDir!!.exists()) {
            deleteDirectory(launchTargetDir!!)
        }
        launchTargetDir!!.mkdirs()
        if (testLaunchGeneration == TestLaunchGeneration.NONE) {
            return
        }
        if (testLaunchProjectName == null) {
            logger.error("Asked to generate launch files, but no test launch project name specified")
            exitProcess(1)
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
                        value = testLaunchProjectName
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
                File(launchTargetDir, "${suite.moduleName}`${test.testName}.launch").writeText(xml)
            }
        }
    }

    private fun logTestResults(testResults: List<TestSuiteResult>) {
        if (testResults.all { it.succeeded }) {
            logger.info("SUCCESS -- ${testResults.sumBy { it.testCount }} tests passed")
        } else {
            logger.info("FAILURE -- ${testResults.sumBy { it.failCount }} tests failed, ${testResults.sumBy { it.errorCount }} tests had errors [${testResults.sumBy { it.testCount }} tests were run]")
        }
    }

    private fun loadSpecification(): ModuleInterpreter {
        // For coverage we need to reparse to correctly identify lex locations in files
        val controller = dialect.createController()
        controller.parse(specificationFiles)
        controller.typeCheck()
        return controller.getInterpreter() as? ModuleInterpreter
        // this should never happen as we have limited dialect to VDM-SL
                ?: exitProcess(2)
    }

    private fun collectTests(interpreter: ModuleInterpreter): List<TestSuite> {
        val testModules = interpreter.modules.filter { module ->
            module.files.all { testSourceDir == null || it.startsWith(testSourceDir!!) } &&
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
        if (reportTargetDir.exists()) {
            deleteDirectory(reportTargetDir)
        }
        reportTargetDir.mkdirs()
        testSuiteResults.forEach { saveFormattedResults(it, reportTargetDir) }
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