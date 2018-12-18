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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDateTime

const val dependencyUnpack = "dependencyUnpack"

internal fun Project.addDependencyUnpackTask() = createVdmTask(dependencyUnpack, DependencyUnpackTask::class.java)

open class DependencyUnpackTask : DefaultTask() {

    private val vdmConfiguration = project.configurations.getByName(vdmConfigurationName)

    // There is nothing serializable that we can depend on out-of-the-box. Create a string that we can use for comparison.
    val dependencies: String
        @Input
        get() = vdmConfiguration.dependencies.map { d ->
            // No way that we can determine whether snapshots are uptodate without resolution -- so take pessimistic view
            "${d.group}:${d.name}:${if (d.version?.endsWith("-SNAPSHOT") == true) LocalDateTime.now().toString() else d.version}"
        }.joinToString(";")

    val vdmMdDependencyDir: File
        @OutputDirectory
        get() = project.vdmMdDependencyDir

    val vdmTestDependencyDir: File
        @OutputDirectory
        get() = project.vdmTestDependencyDir

    val vdmDependencyDir: File
        @OutputDirectory
        get() = project.vdmDependencyDir

    val autoDependMd: Boolean
        @Input
        get() = project.vdmConfig.dependencies.autoDependMd

    val autoDependTest: Boolean
        @Input
        get() = project.vdmConfig.dependencies.autoDependTest

    @TaskAction
    fun unpack() {
        vdmConfiguration.resolutionStrategy.failOnVersionConflict()
        unpackSpecifications()
        unpackTests()
        unpackDocumentation()
    }

    // TODO - finer grained control
    private fun unpackDocumentation() {
        if (autoDependMd) {
            autoUnpackAttachedArtifacts(vdmMarkdownConfiguration, "md", vdmMdDependencyDir)
        }
    }

    // TODO - finer grained control
    private fun unpackTests() {
        if (autoDependTest) {
            autoUnpackAttachedArtifacts(vdmTestConfiguration, "test", vdmTestDependencyDir)
        }
    }

    private fun autoUnpackAttachedArtifacts(attachedConfiguration: String, classifier: String, unpackDirectory: File) {
        if (project.configurations.findByName(attachedConfiguration) == null) {
            project.configurations.create(attachedConfiguration)
        }
        vdmConfiguration.incoming.artifacts.map {
            it.id.componentIdentifier as ModuleComponentIdentifier
        }.forEach { id ->
            project.dependencies.add(attachedConfiguration, "${id.group}:${id.module}:${id.version}:$classifier@zip")
        }
        project.configurations.getByName(attachedConfiguration).resolvedConfiguration.lenientConfiguration.artifacts.forEach { artifact ->
            artifact.id.componentIdentifier as ModuleComponentIdentifier
            unpackArtifact(artifact.id, artifact.file, unpackDirectory)
        }
    }

    private fun unpackSpecifications() {
        vdmConfiguration.incoming.artifacts.forEach { artifact ->
            unpackArtifact(artifact.id, artifact.file, vdmDependencyDir)
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
