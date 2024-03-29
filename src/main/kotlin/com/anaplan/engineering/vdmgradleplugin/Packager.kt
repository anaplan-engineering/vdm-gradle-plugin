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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

const val vdmPackage = "package"

internal fun Project.addPackageTask() {
    createVdmTask(vdmPackage, VdmPackageTask::class.java)
    afterEvaluate { project ->
        val vdmPackageTask = project.tasks.getByName(vdmPackage)
            ?: throw GradleException("Cannot find VDM package task")
        vdmPackageTask.dependsOn(typeCheckTests)
        val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
            ?: throw GradleException("Cannot find assemble task")
        assembleTask.dependsOn(vdmPackage)
    }
}

// TODO -- separate tasks to minimize repackaging
open class VdmPackageTask() : DefaultTask() {

    val sourceFiles: FileCollection
        @InputFiles
        get() = project.files(locateSpecifications(project.vdmSourceDir, project.vdmConfig.dialect))

    val vdmPackageFile: File
        @OutputFile
        get() = project.vdmPackageFile

    val testSourceFiles: FileCollection
        @InputFiles
        get() = project.files(locateSpecifications(project.vdmTestSourceDir, project.vdmConfig.dialect))

    val vdmTestPackageFile: File
        @OutputFile
        get() = project.vdmTestPackageFile

    val mdSourceFiles: FileCollection
        @InputFiles
        get() {
            val resourceTypes = project.vdmConfig.resourceFileTypes
            return project.files(locateFilesWithExtension(project.vdmMdDir, "md", *resourceTypes))
        }

    val vdmMdPackageFile: File
        @OutputFile
        get() = project.vdmMdPackageFile

    val packageMdSource: Boolean
        @Input
        get() = project.vdmConfig.packaging.mdSource

    val packageTestSource: Boolean
        @Input
        get() = project.vdmConfig.packaging.testSource

    @TaskAction
    fun createPackages() {
        createSourcePackage()
        createTestSourcePackage()
        createDocSourcePackage()
    }

    private fun createSourcePackage() {
        createZip(
            vdmPackageFile,
            ZipContents(sourceFiles, baseDir = project.vdmSourceDir),
            project.createManifestForZip("main")
        )
    }

    private fun createDocSourcePackage() {
        if (!packageMdSource) {
            return
        }
        if (mdSourceFiles.isEmpty) {
            return
        }
        createZip(
            vdmMdPackageFile,
            ZipContents(mdSourceFiles, baseDir = project.vdmMdDir),
            project.createManifestForZip("md")

        )
    }

    private fun createTestSourcePackage() {
        if (!packageTestSource) {
            return
        }
        if (testSourceFiles.isEmpty) {
            return
        }
        createZip(
            vdmTestPackageFile,
            ZipContents(testSourceFiles, baseDir = project.vdmTestSourceDir),
            project.createManifestForZip("test")
        )
    }
}


