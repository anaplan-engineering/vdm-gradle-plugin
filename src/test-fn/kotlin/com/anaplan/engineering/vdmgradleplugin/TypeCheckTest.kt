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
import org.overture.interpreter.VDMSL
import org.overture.interpreter.util.ModuleListInterpreter
import java.io.File

@RunWith(Parameterized::class)
class TypeCheckTest(
        private val testName: String,
        private val includeTests: Boolean,
        private val expectSuccess: Boolean,
        private val checkModules: (ModuleListInterpreter) -> Unit
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
                test(testName = "parseError", expectSuccess = false),
                test(testName = "typeCheckError", expectSuccess = false),
                test(testName = "parseAndTypeCheckOk", checkModules = { Assert.assertEquals(1, it.size) }),
                test(testName = "parseErrorInTests", includeTests = true, expectSuccess = false),
                test(testName = "typeCheckErrorInTests", includeTests = true, expectSuccess = false),
                test(testName = "parseAndTypeCheckTestsOk", includeTests = true, checkModules = { Assert.assertEquals(2, it.size) }),
                // the same tests should pass if we only run 'typeCheck'
                test(testName = "parseErrorInTests"),
                test(testName = "typeCheckErrorInTests"),
                test(testName = "parseAndTypeCheckTestsOk", checkModules = { Assert.assertEquals(1, it.size) }),
                test(testName = "wrongFileExtensionIgnored", checkModules = { Assert.assertEquals(1, it.size) }),
                test(testName = "wrongDialect", expectSuccess = false),
                test(testName = "customSourceFolders", includeTests = true, checkModules = { Assert.assertEquals(2, it.size) })
        )

        private fun test(
                testName: String,
                includeTests: Boolean = false,
                expectSuccess: Boolean = true,
                checkModules: (ModuleListInterpreter) -> Unit = {}
        ): Array<Any> = arrayOf(testName, includeTests, expectSuccess, checkModules)
    }

    @Test
    fun typeCheckTest() {
        val dir = File(javaClass.getResource("/$testName").toURI())
        val task = if (includeTests) "typeCheckTests" else "typeCheck"
        executeBuild(
                projectDir = dir,
                tasks = arrayOf(task),
                fail = !expectSuccess)
        val libFile = File(dir, "build/vdm/generated.lib")
        Assert.assertEquals(expectSuccess, libFile.exists())

        if (expectSuccess) {
            val vdmsl = VDMSL()
            vdmsl.parse(listOf(libFile))
            checkModules(vdmsl.interpreter.modules)
        }
    }

}