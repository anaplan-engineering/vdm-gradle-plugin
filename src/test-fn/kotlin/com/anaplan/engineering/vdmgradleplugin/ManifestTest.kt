package com.anaplan.engineering.vdmgradleplugin

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.zip.ZipFile

@RunWith(Parameterized::class)
class ManifestTest(
        private val testName: String,
        private val expectedAttributes: Map<String, String>
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
                test(testName = "parseAndTypeCheckOk", expectedAttribues = mapOf(
                        "Group" to "testing",
                        "Name" to "parseAndTypeCheckOk",
                        "Version" to "1.0.0",
                        "Type" to "main"
                ))
        )

        private fun test(
                testName: String,
                expectedAttribues: Map<String, String>
        ): Array<Any> = arrayOf(testName, expectedAttribues)
    }

    @Test
    fun manifestTest() {
        val projectDir = File(javaClass.getResource("/$testName").toURI())
        executeBuild(
                projectDir = projectDir,
                tasks = arrayOf("package"),
                fail = false)
        val packageFile = File(projectDir, "build/libs/$testName-1.0.0.zip")
        Assert.assertTrue(packageFile.exists())
        val manifestText = getManifestText(packageFile)
        expectedAttributes.entries.forEach { (k, v) ->
            manifestText.contains("$k: $v")
        }
    }

    private fun getManifestText(file: File) =
            ZipFile(file).use { zip ->
                val manifestEntry = zip.entries().asSequence().find { it.name == "manifest.mf" }
                zip.getInputStream(manifestEntry).use { input ->
                    input.bufferedReader().use { it.readText() }
                }
            }

}