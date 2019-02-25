package com.anaplan.engineering.vdmgradleplugin

import org.junit.Assert
import org.junit.Test
import java.io.File

class CoverageTest {

    @Test
    fun coverageTest() {
        val dir = File(javaClass.getResource("/coverageTest").toURI())
        executeBuild(
                projectDir = dir,
                tasks = arrayOf("test"),
                fail = false)
        val coverageFile = File(dir, "build/vdm/coverage/Main.html")
        Assert.assertTrue(coverageFile.exists())
        val expectedHtml = File(javaClass.getResource("/coverageTest/expected.html").toURI())
        Assert.assertEquals(expectedHtml.readText(), coverageFile.readText())
    }

    @Test
    fun noCoverageTest() {
        val dir = File(javaClass.getResource("/noCoverageTest").toURI())
        executeBuild(
                projectDir = dir,
                tasks = arrayOf("test"),
                fail = false)
        val coverageFile = File(dir, "build/vdm/coverage/Main.html")
        Assert.assertFalse(coverageFile.exists())
    }
}