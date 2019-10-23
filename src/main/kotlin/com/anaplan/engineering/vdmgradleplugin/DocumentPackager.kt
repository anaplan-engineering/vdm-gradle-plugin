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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File


const val docPackage = "docPackage"

internal fun Project.addDocPackageTask() {
    createVdmTask(docPackage, DocPackageTask::class.java)
    afterEvaluate { project ->
        val docPackageTask = project.tasks.getByName(docPackage)
                ?: throw GradleException("Cannot find document package task")
        docPackageTask.dependsOn(docGen)
        if (project.vdmConfig.autoDocGeneration) {
            val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
                    ?: throw GradleException("Cannot find assemble task")
            assembleTask.dependsOn(docPackage)
        }
    }
}


internal val Project.docPackageFile: File
    get() = File(buildDir, "libs/$name-$version-doc.zip")


open class DocPackageTask : DefaultTask() {
    val docFiles : FileCollection
        @InputFiles
        get() {
            val resourceTypes = project.vdmConfig.resourceFileTypes
            return project.files(locateFilesWithExtension(project.vdmGenDocsDir, "html", "css", *resourceTypes))
        }

    val docPackageFile: File
        @OutputFile
        get() = project.docPackageFile

    @TaskAction
    fun packageDocs() {
        if (!project.vdmGenDocsDir.exists()) {
            return
        }
        createZip(
                docPackageFile,
                ZipContents(docFiles, baseDir = project.vdmGenDocsDir)
        )
    }
}
