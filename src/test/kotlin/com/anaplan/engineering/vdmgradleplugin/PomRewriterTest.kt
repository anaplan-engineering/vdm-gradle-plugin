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
import java.nio.file.Files
import java.nio.file.Paths

class PomRewriterTest {

    @Test
    fun addDependencies_noExistingDependencies() = check("pom-no-dependencies") { pomRewriter ->
        pomRewriter.addDependencies(
            listOf(
                PomRewriter.Dependency("group", "artifact", "1.0.0", null, "zip", "compile")
            )
        )
    }

    @Test
    fun addDependencies_withExistingDependencies() = check("pom-with-dependencies") { pomRewriter ->
        pomRewriter.addDependencies(
            listOf(
                PomRewriter.Dependency("group2", "artifact2", "2.0.0", null, "zip", "compile")
            )
        )
    }

    private fun check(resourceSuffix: String, makeChanges: (PomRewriter) -> Unit) {
        val tempDir = Files.createTempDirectory(resourceSuffix)
        val sourceFileOrig = Paths.get(javaClass.getResource("$resourceSuffix-source.xml").toURI())
        val sourceFileCopy = tempDir.resolve("$resourceSuffix-source.xml")
        Files.copy(sourceFileOrig, sourceFileCopy)
        makeChanges(PomRewriter(sourceFileCopy.toFile()))
        val expectedFile = Paths.get(javaClass.getResource("$resourceSuffix-expected.xml").toURI())
        Assert.assertEquals(
            PomRewriter(expectedFile.toFile()).readDocument().toString(),
            PomRewriter(sourceFileCopy.toFile()).readDocument().toString()
        )
    }


}
