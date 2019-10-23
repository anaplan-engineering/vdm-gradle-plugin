/*
 * #%~
 * VDM Gradle Plugin
 * %%
 * Copyright (C) 2018-9 Anaplan Inc
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

import org.gradle.api.Project
import java.io.File
import java.time.LocalDateTime

private val Project.vdmManifestDir
    get() = File(vdmBuildDir, "manifests")

private fun Project.createManifestFile(type: String) : File {
    val manifestFile = File(vdmManifestDir, "manifest-$type.mf")
    if (!manifestFile.parentFile.exists()) {
        manifestFile.parentFile.mkdirs()
    }
    manifestFile.writeText(createManifest(type))
    return manifestFile
}

private fun Project.createManifest(type: String): String {
    val sb = StringBuilder()
    sb.append("Group: $group\n")
    sb.append("Name: $name\n")
    sb.append("Version: $version\n")
    sb.append("Type: $type\n")
    sb.append("Built: ${LocalDateTime.now()}\n")
    return sb.toString()
}

internal fun Project.createManifestForZip(type: String) =
    ZipContents(listOf(project.createManifestFile(type)), listOf(File("manifest.mf")))
