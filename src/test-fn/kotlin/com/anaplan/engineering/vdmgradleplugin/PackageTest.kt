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
import java.util.zip.ZipFile

@RunWith(Parameterized::class)
class PackageTest(
        private val testName: String,
        private val expectedEntries: Map<String, Set<String>>
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
                test(testName = "parseAndTypeCheckOk", expectedEntries = mapOf(
                        "" to setOf("manifest.mf", "parse-and-type-check-ok.vdmsl")
                )),
                test(testName = "parseAndTypeCheckTestsOk", expectedEntries = mapOf(
                        "" to setOf("manifest.mf", "parse-and-type-check-ok.vdmsl"),
                        "test" to setOf("manifest.mf", "test-parse-and-type-check-ok.vdmsl")
                )),
                test(testName = "parseAndTypeCheckTestsWithMdOk", expectedEntries = mapOf(
                        "" to setOf("manifest.mf", "parse-and-type-check-ok.vdmsl"),
                        "test" to setOf("manifest.mf", "test-parse-and-type-check-ok.vdmsl"),
                        "md" to setOf("manifest.mf", "index.md")
                )),
                test(testName = "packageWithNesting", expectedEntries = mapOf(
                        "" to setOf("manifest.mf", "parse-and-type-check-ok.vdmsl", "subdir/other.vdmsl"),
                        "test" to setOf("manifest.mf", "test-parse-and-type-check-ok.vdmsl", "subdir/test-other.vdmsl"),
                        "md" to setOf("manifest.mf", "index.md", "subdir/other.md")
                )),
                test(testName = "packageWithResources", expectedEntries = mapOf(
                        "" to setOf("manifest.mf", "a.vdmsl", "b.vdmsl"),
                        "md" to setOf("manifest.mf", "A.md", "images/b.png")
                )),
                test(testName = "wrongFileExtensionIgnored", expectedEntries = mapOf(
                        "" to setOf("manifest.mf", "parse-and-type-check-ok.vdmsl")
                ))
        )

        private fun test(
                testName: String,
                expectedEntries: Map<String, Set<String>>
        ): Array<Any> = arrayOf(testName, expectedEntries)
    }

    @Test
    fun packageTest() {
        val projectDir = File(javaClass.getResource("/$testName").toURI())
        executeBuild(
                projectDir = projectDir,
                tasks = arrayOf("package"),
                fail = false)
        expectedEntries.keys.forEach {
            checkPackage(projectDir, it)
        }
    }

    private fun checkPackage(projectDir: File, classifier:String) {
        val classifierSuffix = if (classifier.isEmpty()) "" else "-$classifier"
        val packageFile = File(projectDir, "build/libs/$testName-1.0.0$classifierSuffix.zip")
        Assert.assertTrue(packageFile.exists())
        Assert.assertEquals(expectedEntries.get(classifier), getActualEntries(packageFile))
    }

    private fun getActualEntries(file: File) =
        ZipFile(file).use { it.entries().asSequence().map { it.name }.toSet() }

}