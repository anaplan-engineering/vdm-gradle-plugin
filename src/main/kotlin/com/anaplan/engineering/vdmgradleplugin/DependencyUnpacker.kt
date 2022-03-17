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
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ComponentSelectionReasons
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime

const val dependencyUnpack = "dependencyUnpack"

internal fun Project.addDependencyUnpackTask() {
    val localTask = createVdmTask(dependencyUnpack, DependencyUnpackTask::class.java)
    afterEvaluate {
        val vdmConfiguration = project.configurations.getByName(vdmConfigurationName)
        vdmConfiguration.dependencies.filterIsInstance<ProjectDependency>().forEach {
            val plugins = it.dependencyProject.plugins
            if (plugins.findPlugin(pluginId) != null) {
                val dependencyTask = it.dependencyProject.tasks.getByName(dependencyUnpack)
                    ?: throw GradleException("Cannot find unpack task in project dependency")
                localTask.dependsOn(dependencyTask)
            } else if (plugins.findPlugin("java") != null) {
                val assembleTask = it.dependencyProject.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
                    ?: throw GradleException("Cannot find unpack task in project dependency")
                localTask.dependsOn(assembleTask)
            } else {
                throw GradleException("Don't know what to do with project $it")
            }
        }
    }
}

open class DependencyUnpackTask : DefaultTask() {

    private val vdmConfiguration = project.configurations.getByName(vdmConfigurationName)

    // There is nothing serializable that we can depend on out-of-the-box. Create a string that we can use for comparison.
    val dependencies: String
        @Input
        get() = vdmConfiguration.dependencies.map { d ->
            // No way that we can determine whether snapshots are uptodate without resolution -- so take pessimistic view
            "${d.group}:${d.name}:${
                if (d.version?.endsWith("-SNAPSHOT") == true) LocalDateTime.now().toString() else d.version
            }"
        }.joinToString(";")

    val vdmMdDependencyDir: File
        @OutputDirectory
        get() = project.vdmMdDependencyDir

    val vdmTestDependencyDir: File
        @OutputDirectory
        get() = project.vdmTestDependencyDir

    val vdmLibDependencyDir: File
        @OutputDirectory
        get() = project.vdmLibDependencyDir

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
        installLibs()
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
        vdmConfiguration.incoming.artifactsWithType("zip").map {
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
        vdmConfiguration.resolve()
        vdmConfiguration.incoming.resolutionResult.allComponents.forEach { component ->
            val reasons = component.selectionReason.descriptions
            when {
                ComponentSelectionReasons.COMPOSITE_BUILD in reasons -> {
                    throw GradleException("Composite builds requiring substitution are not supported")
                }
                ComponentSelectionReasons.REQUESTED in reasons -> {
                    logger.info("Unpacking artifacts for requested component: ${component.id}")
                    when (component.id) {
                        is ModuleComponentIdentifier -> {
                            vdmConfiguration.incoming.artifactsWithType("zip").filter {
                                it.id.componentIdentifier == component.id
                            }.forEach { artifact ->
                                unpackArtifact(artifact.id, artifact.file, vdmDependencyDir)
                            }
                        }
                        is ProjectComponentIdentifier -> {
                            val projectId = component.id as ProjectComponentIdentifier
                            logger.info("Link to project: $projectId")
                            val dependency = project.findProject(projectId.projectPath)
                                ?: throw GradleException("References unlocatable dependency $projectId")


                            val plugins = dependency.plugins
                            if (plugins.findPlugin(pluginId) != null) {
                                val dependencyLink = File(vdmDependencyDir, "${dependency.group}/${dependency.name}")
                                if (dependencyLink.exists()) {
                                    dependencyLink.delete()
                                }
                                createLink(dependencyLink, dependency.vdmSourceDir)

                                val groupDirs = dependency.vdmDependencyDir.listFiles()?.filter { it.isDirectory }
                                    ?: emptyList()
                                groupDirs.forEach { groupDir ->
                                    val moduleDirs = groupDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
                                    moduleDirs.forEach { moduleDir ->
                                        val transDependencyLink =
                                            File(vdmDependencyDir, "${groupDir.name}/${moduleDir.name}")
                                        if (transDependencyLink.exists()) {
                                            // check its the same?
                                        } else {
                                            createLink(transDependencyLink, moduleDir)
                                        }
                                    }
                                }


                                if (autoDependTest) {
                                    if (dependency.vdmTestSourceDir.exists()) {
                                        val testDependencyLink =
                                            File(vdmTestDependencyDir, "${dependency.group}/${dependency.name}")
                                        if (testDependencyLink.exists()) {
                                            testDependencyLink.delete()
                                        }
                                        createLink(testDependencyLink, dependency.vdmTestSourceDir)
                                    }

                                    val testGroupDirs =
                                        dependency.vdmTestDependencyDir.listFiles()?.filter { it.isDirectory }
                                            ?: emptyList()
                                    testGroupDirs.forEach { groupDir ->
                                        val moduleDirs = groupDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
                                        moduleDirs.forEach { moduleDir ->
                                            val transDependencyLink =
                                                File(vdmTestDependencyDir, "${groupDir.name}/${moduleDir.name}")
                                            if (transDependencyLink.exists()) {
                                                // check its the same?
                                            } else {
                                                createLink(transDependencyLink, moduleDir)
                                            }
                                        }
                                    }
                                }

                                if (autoDependMd) {
                                    if (dependency.vdmMdDir.exists()) {
                                        val mdDependencyLink =
                                            File(vdmMdDependencyDir, "${dependency.group}/${dependency.name}")
                                        if (mdDependencyLink.exists()) {
                                            mdDependencyLink.delete()
                                        }
                                        createLink(mdDependencyLink, dependency.vdmMdDir)
                                    }

                                    val mdGroupDirs =
                                        dependency.vdmMdDependencyDir.listFiles()?.filter { it.isDirectory }
                                            ?: emptyList()
                                    mdGroupDirs.forEach { groupDir ->
                                        val moduleDirs = groupDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
                                        moduleDirs.forEach { moduleDir ->
                                            val transDependencyLink =
                                                File(vdmMdDependencyDir, "${groupDir.name}/${moduleDir.name}")
                                            if (transDependencyLink.exists()) {
                                                // check its the same?
                                            } else {
                                                createLink(transDependencyLink, moduleDir)
                                            }
                                        }
                                    }
                                }

                                dependency.vdmLibDependencyDir.listFiles()?.filter {
                                    it.extension == "jar"
                                }?.forEach {
                                    installLib(it, vdmLibDependencyDir)
                                }
                            } else if (plugins.findPlugin("java") != null) {
                                File(dependency.buildDir, "libs").listFiles()?.filter {
                                    it.extension == "jar"
                                }?.forEach {
                                    installLib(it, vdmLibDependencyDir)
                                }
                            } else {
                                throw GradleException("Don't know what to do with project $projectId")
                            }
                        }
                    }
                }
                ComponentSelectionReasons.ROOT in reasons -> {
                }
                else -> throw GradleException("Unsupported dependency on ${component.id} reasons $reasons")
            }
        }
    }

    private fun createLink(from: File, to: File) {
        from.parentFile.mkdirs()
        if (from.exists()) {
            from.delete()
        }
        if (isWindows) {
            // Cannot create symbolic links on windows -- https://bugs.openjdk.java.net/browse/JDK-8221852
            Files.createLink(from.toPath(), to.toPath())
        } else {
            Files.createSymbolicLink(from.toPath(), to.toPath().toRealPath())
        }
    }

    private fun ResolvableDependencies.artifactsWithType(type: String) = artifacts.filter { it.file.extension == type }

    private fun installLibs() {
        cleanLibs(project)
        vdmConfiguration.incoming.artifactsWithType("jar").forEach { artifact ->
            installLib(artifact.file, vdmLibDependencyDir)
        }
    }

    private fun installLib(file: File, dependencyDirectory: File) {
        logger.info("Install lib: $file")
        if (!dependencyDirectory.exists()) {
            dependencyDirectory.mkdirs()
        }
        if (!project.vdmLibDir.exists()) {
            project.vdmLibDir.mkdirs()
        }
        // create a link to artifact within dependencies so we can track all libs installed via gradle
        createLink(File(dependencyDirectory, file.name), file)
        // install into lib directory for use in Overture and tests
        createLink(File(project.vdmLibDir, file.name), file)
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
        extractZip(file, dependencyUnpackDir)
    }
}
