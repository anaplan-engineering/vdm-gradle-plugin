package com.anaplan.engineering.vdmgradleplugin

import com.anaplan.engineering.vdmgradleplugin.TestRunner.executeBuild
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverageTest {

    @Test
    fun coverageTest() {
        val dir = File(javaClass.getResource("/coverageTest").toURI())
        executeBuild(
            projectDir = dir,
            tasks = arrayOf("test"),
            fail = false
        )
        val coverageFile = File(dir, "build/vdm/coverage/Main.html")
        assertTrue(coverageFile.exists())
        val expectedHtml = File(javaClass.getResource("/coverageTest/expected.html").toURI())
        assertEquals(expectedHtml.readText(), coverageFile.readText())
    }

    @Test
    fun noCoverageTest() {
        val dir = File(javaClass.getResource("/noCoverageTest").toURI())
        executeBuild(
            projectDir = dir,
            tasks = arrayOf("test"),
            fail = false
        )
        val coverageFile = File(dir, "build/vdm/coverage/Main.html")
        assertFalse(coverageFile.exists())
    }
}
