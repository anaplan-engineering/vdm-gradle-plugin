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

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class DocGenTest(
        private val testName: String,
        private val expectSuccess: Boolean,
        private val modules: List<String>,
        private val testModules: List<String>,
        private val pages: List<String>,
        private val resources: List<String>
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
                test(testName = "renderModules", modules = listOf("A", "B")),
                test(testName = "renderDefaultModule", modules = listOf("DEFAULT")),
                test(testName = "renderTestModules", modules = listOf("A", "B"), testModules = listOf("TestA", "TestB")),
                test(testName = "generatePlainPage", modules = listOf("A", "B"), pages = listOf("A")),
                test(testName = "generateSubDir", modules = listOf("A", "B"), pages = listOf("A", "subDir/A")),
                test(testName = "generateWithResources", modules = listOf("A", "B"), pages = listOf("A"), resources = listOf("images/b.png")),
                test(testName = "generateModuleList", modules = listOf("A", "B"), pages = listOf("A")),
                test(testName = "generateTestModuleList", modules = listOf("A", "B"), testModules = listOf("TestA", "TestB"), pages = listOf("A")),
                test(testName = "generateModuleLink", modules = listOf("A", "B"), pages = listOf("A")),
                test(testName = "generateModuleRef", modules = listOf("A", "B"), pages = listOf("A")),
                // test(testName = "generateMultipleModuleRef", modules = listOf("A", "B"), pages = listOf("A")),
                test(testName = "generateMultipleModuleRefEscaped", modules = listOf("A", "B"), pages = listOf("A")),
                test(testName = "generateAnchor", modules = listOf("A", "B"), pages = listOf("A")),
                // TODO - test for links to dependencies
                test(testName = "generateLocalPageLink", modules = listOf("A", "B"), pages = listOf("A", "B"))
        )

        private fun test(
                testName: String,
                expectSuccess: Boolean = true,
                modules: List<String> = listOf(),
                testModules: List<String> = listOf(),
                pages: List<String> = listOf(),
                resources: List<String> = listOf()
        ): Array<Any> = arrayOf(testName, expectSuccess, modules, testModules, pages, resources)
    }

    @Test
    fun docGenTest() {
        val testDir = File(javaClass.getResource("/$testName").toURI())
        val projectDir = File(testDir, "project")
        val expectedDir = File(testDir, "expected")
        executeBuild(
                projectDir = projectDir,
                tasks = arrayOf("docGen"),
                fail = !expectSuccess)
        val docDir = File(projectDir, "build/vdm/docs")
        checkModules(docDir, expectedDir)
        checkPages(docDir, expectedDir)
        checkResources(docDir, expectedDir)
    }

    private fun checkModules(docDir: File, expectedDir: File) {
        fun check(moduleDir: String) = { module: String ->
            val moduleRender = File(docDir, "$moduleDir/$module.html")
            Assert.assertTrue(moduleRender.exists())
            val expectedRender = File(expectedDir, "$moduleDir/$module.html")
            if (expectedRender.exists()) {
                Assert.assertEquals(expectedRender.readText(), moduleRender.readText())
            }
        }
        modules.forEach(check("modules"))
        testModules.forEach(check("testModules"))
    }

    private fun checkPages(docDir: File, expectedDir: File) =
        pages.forEach { page: String ->
            val pageRender = File(docDir, "$page.html")
            Assert.assertTrue(pageRender.exists())
            val expectedRender = File(expectedDir, "$page.html")
            if (expectedRender.exists()) {
                Assert.assertEquals(expectedRender.readText(), pageRender.readText())
            }
        }

    private fun checkResources(docDir: File, expectedDir: File) =
        resources.forEach { resource: String ->
            val pageRender = File(docDir, resource)
            Assert.assertTrue(pageRender.exists())
            val expectedResource = File(expectedDir, resource)
            if (expectedResource.exists()) {
                Assert.assertEquals(expectedResource.readBytes().toList(), pageRender.readBytes().toList())
            }
        }



}