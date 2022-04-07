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

import com.anaplan.engineering.vdmgradleplugin.TestRunner.executeBuild
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class TypeCheckTest(
    private val testName: String,
    private val includeTests: Boolean,
    private val expectSuccess: Boolean,
    private val expected: Int?
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
            test(testName = "parseError", expectSuccess = false),
            test(testName = "typeCheckError", expectSuccess = false),
            test(testName = "parseAndTypeCheckOk", expected = 1),
            test(testName = "parseErrorInTests", includeTests = true, expectSuccess = false),
            test(testName = "typeCheckErrorInTests", includeTests = true, expectSuccess = false),
            test(testName = "parseAndTypeCheckTestsOk", includeTests = true, expected = 2),
            // the same tests should pass if we only run 'typeCheck'
            test(testName = "parseErrorInTests", expected = 1),
            test(testName = "typeCheckErrorInTests", expected = 1),
            test(testName = "parseAndTypeCheckTestsOk", expected = 1),
            test(testName = "wrongFileExtensionIgnored", expected = 1),
            test(testName = "wrongDialect", expectSuccess = false),
            test(testName = "customSourceFolders", includeTests = true, expected = 2)
        )

        private fun test(
            testName: String,
            includeTests: Boolean = false,
            expectSuccess: Boolean = true,
            expected: Int? = null
        ) = arrayOf(testName, includeTests, expectSuccess, expected)
    }

    @Test
    fun typeCheckTest() {
        val dir = File(javaClass.getResource("/$testName")!!.toURI())
        val task = if (includeTests) "typeCheckTests" else "typeCheck"
        executeBuild(
            projectDir = dir,
            tasks = arrayOf(task),
            fail = !expectSuccess
        )

        val logResource = javaClass.getResource("/$testName/build/vdm/typeCheckTask.log")
        Assert.assertNotNull("Log not found", logResource)

        val logText = File(logResource!!.toURI()).readText()
        Assert.assertTrue("Log was not saved", logText.isNotEmpty())

        if (expectSuccess) {
            val re = """Type checked (?<num>\d+) module""".toRegex()
            val typeCheckedModules = re.find(logText)?.groups?.get("num")?.value?.toInt()
            Assert.assertEquals(expected, typeCheckedModules)
        }
    }
}
