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

import com.anaplan.engineering.vdmgradleplugin.TestRunner.executeCompositeBuild
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CompositeBuildTest(
    private val testName: String,
    private val expectSuccess: Boolean
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
            test(testName = "compositeWithDependencyPassingTest")
        )

        private fun test(
            testName: String,
            expectSuccess: Boolean = true
        ): Array<Any> = arrayOf(testName, expectSuccess)
    }

    @Test
    fun compositeBuildTest() {
        val dir = File(javaClass.getResource("/$testName").toURI())
        executeCompositeBuild(
            projectDir = dir,
            tasks = arrayOf("build"),
            fail = !expectSuccess
        )
    }

}
