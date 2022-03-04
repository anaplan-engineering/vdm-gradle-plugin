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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibCleanerTest {

    @Test
    fun publishAndDependencyTest() {
        val parentDir = File(javaClass.getResource("/libCleanerTest").toURI())

        val javaLibDir = File(parentDir, "javalib")
        executeBuild(projectDir = javaLibDir, tasks = arrayOf("publish"), fail = false)

        val vdmLibDir = File(parentDir, "vdmlib")
        val installedLibFile = File(vdmLibDir, "lib/javalib-1.0.0.jar")
        assertFalse(installedLibFile.exists())

        executeBuild(projectDir = vdmLibDir, tasks = arrayOf("test"), fail = false)
        assertTrue(installedLibFile.exists())

        executeBuild(projectDir = vdmLibDir, tasks = arrayOf("clean"), fail = false)
        assertFalse(installedLibFile.exists())

    }
}
