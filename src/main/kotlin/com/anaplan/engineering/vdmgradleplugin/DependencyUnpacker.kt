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
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.TaskAction
import java.io.File

const val dependencyUnpack = "dependencyUnpack"

internal fun Project.addDependencyUnpackTask() = createVdmTask(dependencyUnpack, DependencyUnpackTask::class.java)

open class DependencyUnpackTask : DefaultTask() {

    @TaskAction
    fun unpack() {
        project.configurations.getByName(vdmConfiguration).resolutionStrategy.failOnVersionConflict()
        unpackSpecifications()
        unpackTests()
        unpackDocumentation()
    }

    // TODO - finer grained control
    private fun unpackDocumentation() {
        if (project.vdmConfig.dependencies.autoDependMd) {
            autoUnpackAttachedArtifacts(vdmMarkdownConfiguration, "md", project.vdmMdDependencyDir)
        }
    }

    // TODO - finer grained control
    private fun unpackTests() {
        if (project.vdmConfig.dependencies.autoDependTest) {
            autoUnpackAttachedArtifacts(vdmTestConfiguration, "test", project.vdmTestDependencyDir)
        }
    }

    private fun autoUnpackAttachedArtifacts(attachedConfiguration: String, classifier: String, unpackDirectory: File) {
        if (project.configurations.findByName(attachedConfiguration) == null) {
            project.configurations.create(attachedConfiguration)
        }
        project.configurations.getByName(vdmConfiguration).incoming.artifacts.map { it.id.componentIdentifier }.forEach { id ->
            project.dependencies.add(attachedConfiguration, "$id:$classifier@zip")
        }
        project.configurations.getByName(attachedConfiguration).resolvedConfiguration.lenientConfiguration.artifacts.forEach { artifact ->
            artifact.id.componentIdentifier as ModuleComponentIdentifier
            unpackArtifact(artifact.id, artifact.file, unpackDirectory)
        }
    }

    private fun unpackSpecifications() {
        project.configurations.getByName(vdmConfiguration).incoming.artifacts.forEach { artifact ->
            unpackArtifact(artifact.id, artifact.file, project.vdmDependencyDir)
        }
    }

    private fun unpackArtifact(id: ComponentArtifactIdentifier, file: File, dependencyBaseDirectory: File) {
        val moduleIdentifier = id.componentIdentifier as ModuleComponentIdentifier
        logger.info("Unpack artifact: $moduleIdentifier")
        val dependencyUnpackDir = File(dependencyBaseDirectory, "${moduleIdentifier.group}/${moduleIdentifier.module}")
        if (dependencyUnpackDir.exists()) {
            deleteDirectory(dependencyUnpackDir)
        }
        dependencyUnpackDir.mkdirs()

        logger.debug("Unpack file: $file")
        if (!file.name.endsWith(".zip")) {
            throw GradleException("Invalid dependency type for ${moduleIdentifier.group}:${moduleIdentifier.module}:${moduleIdentifier.version} -- can only be zip")
        }
        extractZip(file, dependencyUnpackDir)
    }

}