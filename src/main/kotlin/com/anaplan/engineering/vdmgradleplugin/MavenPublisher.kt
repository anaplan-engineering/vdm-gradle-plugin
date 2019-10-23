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
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

private const val publicationName = "vdm"

internal fun Project.addVdmMavenPublishHook() {
    val mavenPublishPlugin = plugins.findPlugin(MavenPublishPlugin::class.java)
    if (mavenPublishPlugin != null) {
        addVdmMavenPublish()
    }
    plugins.whenPluginAdded { plugin ->
        if (plugin is MavenPublishPlugin) {
            addVdmMavenPublish()
        }
    }
}

private fun Project.addVdmMavenPublish() {
    logger.info("Hooking VDM artifact publish into Maven publish")

    afterEvaluate { project ->
        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
        val mavenPublication = if (publishingExtension.publications.findByName(publicationName) == null) {
            publishingExtension.publications.create<MavenPublication>(publicationName, MavenPublication::class.java)
        } else {
            publishingExtension.publications.getByName(publicationName) as MavenPublication
        }
        if (mavenPublication.artifacts.none { it.file == project.vdmPackageFile }) {
            logger.info("Marking main artifact for publication")
            mavenPublication.artifact(project.vdmPackageFile)
        }
    }

    val addAttachedArtifactsTask = createVdmTask("AddVdmArtifacts", AddVdmAttachedArtifactsTask::class.java)
    val addVdmDependenciesToPomTask = createVdmTask("AddVdmDependenciesToPom", AddVdmDependenciesToPomTask::class.java)
    addAttachedArtifactsTask.dependsOn(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

    val addTaskDependencies = { task: Task ->
        if (task.name == "generatePomFileForVdmPublication") {
            task.dependsOn(addAttachedArtifactsTask)
            addVdmDependenciesToPomTask.dependsOn(task)
        }
        if (task.name.startsWith("publishVdmPublication")) {
            task.dependsOn(addVdmDependenciesToPomTask)
        }
        if (task.name == "publish" || task.name.startsWith("publishToMavenLocal")) {
            task.dependsOn(addAttachedArtifactsTask)
        }
    }
    project.tasks.forEach(addTaskDependencies)
    project.tasks.whenTaskAdded(addTaskDependencies)
}

private class MavenArtifactAdapter(
        val f: File,
        val e: String,
        val c: String
) : MavenArtifact {
    override fun getExtension() = e
    override fun getFile() = f
    override fun setExtension(p0: String?) {}
    override fun setClassifier(p0: String?) {}
    override fun getBuildDependencies() = null
    override fun getClassifier() = c
    override fun builtBy(vararg p0: Any?) {}
}

open class AddVdmAttachedArtifactsTask : DefaultTask() {
    @TaskAction
    fun addAttachedArtifacts() {
        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
        val mavenPublication = publishingExtension.publications.getByName(publicationName) as MavenPublication
        if (project.vdmTestPackageFile.exists() && mavenPublication.artifacts.none { it.file == project.vdmTestPackageFile }) {
            logger.info("Marking test artifact for publication")
            mavenPublication.artifact(MavenArtifactAdapter(project.vdmTestPackageFile, "zip", "test"))
        }
        if (project.vdmMdPackageFile.exists() && mavenPublication.artifacts.none { it.file == project.vdmMdPackageFile }) {
            logger.info("Marking md artifact for publication")
            mavenPublication.artifact(MavenArtifactAdapter(project.vdmMdPackageFile, "zip", "md"))
        }
        if (project.docPackageFile.exists() && mavenPublication.artifacts.none { it.file == project.docPackageFile }) {
            logger.info("Marking doc artifact for publication")
            mavenPublication.artifact(MavenArtifactAdapter(project.docPackageFile, "zip", "doc"))
        }
    }
}


open class AddVdmDependenciesToPomTask() : DefaultTask() {

    @TaskAction
    fun appendDependencies() {
        val pomRewriter = PomRewriter(File(project.buildDir, "publications/${publicationName}/pom-default.xml"))
        val vdmConfiguration = project.configurations.getByName(vdmConfigurationName)
        val dependencies = vdmConfiguration.dependencies.map { d ->
            PomRewriter.Dependency(
                    groupId = d.group ?: throw IllegalStateException("Dependency has null group id"),
                    artifactId = d.name,
                    version = d.version ?: throw IllegalStateException("Dependency has null version"),
                    scope = "compile"
            )
        }
        pomRewriter.addDependencies(dependencies)
    }

}