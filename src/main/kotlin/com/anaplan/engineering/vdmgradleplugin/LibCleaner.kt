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

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskInstantiationException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

const val cleanLibs = "cleanLibs"

internal fun Project.addCleanLibsTask() {
    createVdmTask(cleanLibs, CleanLibTask::class.java)
    afterEvaluate {
        val cleanTask = tasks.getByName("clean") ?: throw TaskInstantiationException("Cannot find clean task")
        cleanTask.dependsOn(cleanLibs)
    }
}

open class CleanLibTask : DefaultTask() {

    private val vdmConfiguration = project.configurations.getByName(vdmConfigurationName)

    @TaskAction
    fun unpack() {
        cleanLibs(project)
    }

}

internal fun cleanLibs(project: Project) {
    val libDir = project.vdmLibDir
    if (libDir.exists() && libDir.isDirectory) {
        val buildDir = project.vdmBuildDir.toPath()
        Files.walkFileTree(libDir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if ((Files.isSymbolicLink(file) && Files.readSymbolicLink(file).startsWith(buildDir))
                        || Files.exists(project.vdmLibDependencyDir.toPath().resolve(file.fileName))) {
                    Files.delete(file)
                }
                return FileVisitResult.CONTINUE
            }
        })
    }
}