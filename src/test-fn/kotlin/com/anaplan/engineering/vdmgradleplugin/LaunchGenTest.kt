package com.anaplan.engineering.vdmgradleplugin

import com.anaplan.engineering.vdmgradleplugin.TestRunner.executeBuild
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaunchGenTest {

    private val testLaunchDir = "build/vdm/testLaunch/"

    @Test
    fun generateForAll() {
        val dir = File(javaClass.getResource("/generateLaunchGenForAllTest").toURI())
        executeBuild(
            projectDir = dir,
            tasks = arrayOf("test"),
            fail = true
        )
        val failedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd1.launch")
        val passedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd2.launch")
        assertTrue(failedTestLaunchFile.exists())
        assertTrue(passedTestLaunchFile.exists())
        assertEquals(
            File(
                javaClass.getResource("/generateLaunchGenForAllTest/TestTest`TestAdd1.launch").toURI()
            ).readText(), failedTestLaunchFile.readText()
        )
        assertEquals(
            File(
                javaClass.getResource("/generateLaunchGenForAllTest/TestTest`TestAdd2.launch").toURI()
            ).readText(), passedTestLaunchFile.readText()
        )
    }

    @Test
    fun generateForFailed() {
        val dir = File(javaClass.getResource("/generateLaunchGenForFailedTest").toURI())
        executeBuild(
            projectDir = dir,
            tasks = arrayOf("test"),
            fail = true
        )
        val failedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd1.launch")
        val passedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd2.launch")
        assertTrue(failedTestLaunchFile.exists())
        assertFalse(passedTestLaunchFile.exists())
        assertEquals(
            File(
                javaClass.getResource("/generateLaunchGenForFailedTest/TestTest`TestAdd1.launch").toURI()
            ).readText(), failedTestLaunchFile.readText()
        )
    }

    @Test
    fun generateForNone() {
        val dir = File(javaClass.getResource("/generateLaunchGenForNoneTest").toURI())
        executeBuild(
            projectDir = dir,
            tasks = arrayOf("test"),
            fail = true
        )
        val failedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd1.launch")
        val passedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd2.launch")
        assertFalse(failedTestLaunchFile.exists())
        assertFalse(passedTestLaunchFile.exists())
    }


}
