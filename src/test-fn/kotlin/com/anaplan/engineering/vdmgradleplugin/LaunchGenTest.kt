package com.anaplan.engineering.vdmgradleplugin

import org.junit.Assert
import org.junit.Test
import java.io.File

class LaunchGenTest {

    private val testLaunchDir = "build/vdm/testLaunch/"

    @Test
    fun generateForAll() {
        val dir = File(javaClass.getResource("/generateLaunchGenForAllTest").toURI())
        executeBuild(
                projectDir = dir,
                tasks = arrayOf("test"),
                fail = true)
        val failedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd1.launch")
        val passedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd2.launch")
        Assert.assertTrue(failedTestLaunchFile.exists())
        Assert.assertTrue(passedTestLaunchFile.exists())
        Assert.assertEquals(File(javaClass.getResource("/generateLaunchGenForAllTest/TestTest`TestAdd1.launch").toURI()).readText(), failedTestLaunchFile.readText())
        Assert.assertEquals(File(javaClass.getResource("/generateLaunchGenForAllTest/TestTest`TestAdd2.launch").toURI()).readText(), passedTestLaunchFile.readText())
    }

    @Test
    fun generateForFailed() {
        val dir = File(javaClass.getResource("/generateLaunchGenForFailedTest").toURI())
        executeBuild(
                projectDir = dir,
                tasks = arrayOf("test"),
                fail = true)
        val failedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd1.launch")
        val passedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd2.launch")
        Assert.assertTrue(failedTestLaunchFile.exists())
        Assert.assertFalse(passedTestLaunchFile.exists())
        Assert.assertEquals(File(javaClass.getResource("/generateLaunchGenForFailedTest/TestTest`TestAdd1.launch").toURI()).readText(), failedTestLaunchFile.readText())
    }

    @Test
    fun generateForNone() {
        val dir = File(javaClass.getResource("/generateLaunchGenForNoneTest").toURI())
        executeBuild(
                projectDir = dir,
                tasks = arrayOf("test"),
                fail = true)
        val failedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd1.launch")
        val passedTestLaunchFile = File(dir, "$testLaunchDir/TestTest`TestAdd2.launch")
        Assert.assertFalse(failedTestLaunchFile.exists())
        Assert.assertFalse(passedTestLaunchFile.exists())
    }


}