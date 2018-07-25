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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin

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

open class VdmPackageTask() : DefaultTask() {

    @TaskAction
    fun createPackages() {
        createSourcePackage()
        createTestSourcePackage()
        createDocSourcePackage()
    }

    private fun createSourcePackage() {
        val sourceFiles = locateSpecifications(project.vdmSourceDir, project.vdmConfig.dialect)
        createZip(
                project.vdmPackageFile,
                ZipContents(sourceFiles, baseDir = project.vdmSourceDir),
                project.createManifestForZip("main")
        )
    }

    private fun createDocSourcePackage() {
        if (!project.vdmConfig.packaging.mdSource) {
            return
        }
        val resourceTypes = project.vdmConfig.resourceFileTypes
        val sourceFiles = locateFilesWithExtension(project.vdmDocsDir, "md", *resourceTypes)
        if (sourceFiles.isEmpty()) {
            return
        }
        createZip(
                project.vdmMdPackageFile,
                ZipContents(sourceFiles, baseDir = project.vdmDocsDir),
                project.createManifestForZip("md")

        )
    }

    private fun createTestSourcePackage() {
        if (!project.vdmConfig.packaging.testSource) {
            return
        }
        val sourceFiles = locateSpecifications(project.vdmTestSourceDir, project.vdmConfig.dialect)
        if (sourceFiles.isEmpty()) {
            return
        }
        createZip(
                project.vdmTestPackageFile,
                ZipContents(sourceFiles, baseDir = project.vdmTestSourceDir),
                project.createManifestForZip("test")
        )
    }


}

