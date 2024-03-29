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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class VdmGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("vdm", VdmConfigExtension::class.java, project.objects)
        project.configurations.create(vdmConfigurationName)
        project.addBase()
        project.addCleanLibsTask()
        project.addDependencyUnpackTask()
        project.addTypeCheckTasks()
        project.addTestTask()
        project.addPackageTask()
        project.addDocGenTask()
        project.addDocPackageTask()
        project.addVdmMavenPublishHook()
    }

}

internal fun Project.addBase() {
    apply { it.plugin("base") }
}

internal const val vdmTaskGroup = "vdm"
internal const val vdmConfigurationName = "vdm"
internal const val vdmMarkdownConfiguration = "vdm-md"
internal const val vdmTestConfiguration = "vdm-test"
internal const val pluginId = "com.anaplan.engineering.vdm"


internal val Project.vdmConfig
    get() = extensions.getByType(VdmConfigExtension::class.java)

internal val Project.vdmSourceDir
    get() = File(projectDir, vdmConfig.sourcesDir)

internal val Project.vdmTestSourceDir
    get() = File(projectDir, vdmConfig.testSourcesDir)

internal val Project.vdmMdDir
    get() = File(projectDir, vdmConfig.docsDir)

internal val Project.vdmGenDocsDir
    get() = File(vdmBuildDir, "docs")

internal val Project.vdmBuildDir
    get() = File(buildDir, "vdm")

internal val Project.vdmLibDir
    get() = File(projectDir, "lib")

internal val Project.vdmDependencyDir
    get() = File(vdmBuildDir, "dependencies")

internal val Project.vdmMdDependencyDir
    get() = File(vdmBuildDir, "md-dependencies")

internal val Project.vdmTestDependencyDir
    get() = File(vdmBuildDir, "test-dependencies")

internal val Project.vdmLibDependencyDir
    get() = File(vdmBuildDir, "lib-dependencies")

internal val Project.vdmPackageFile: File
    get() = File(buildDir, "libs/$name-$version.zip")

internal val Project.vdmMdPackageFile: File
    get() = File(buildDir, "libs/$name-$version-md.zip")

internal val Project.vdmTestPackageFile: File
    get() = File(buildDir, "libs/$name-$version-test.zip")

internal fun Project.createVdmTask(name: String, type: Class<out Task>) =
    tasks.create(
        mapOf<String, Any>(
            "name" to name,
            "type" to type,
            "group" to vdmTaskGroup
        )
    )

internal fun Project.locateAllSpecifications(dialect: Dialect, includeTests: Boolean) =
    project.files(
        locateSpecifications(project.vdmDependencyDir, dialect) +
            locateSpecifications(project.vdmSourceDir, dialect) +
            if (includeTests) {
                locateSpecifications(project.vdmTestDependencyDir, dialect) +
                    locateSpecifications(project.vdmTestSourceDir, dialect)
            } else {
                listOf()
            }
    )

internal fun locateSpecifications(directory: File, dialect: Dialect): List<File> {
    return locateFilesWithExtension(directory, dialect.fileExtension)
}

internal fun locateFilesWithExtension(directory: File, vararg extensions: String): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    val files = ArrayList<Path>()
    val fileVisitor = object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
            if (extensions.any { file.fileName.toString().endsWith(it) }) {
                files.add(file)
            }
            return FileVisitResult.CONTINUE
        }
    }
    val opts = EnumSet.of<FileVisitOption>(FileVisitOption.FOLLOW_LINKS)
    Files.walkFileTree(directory.toPath(), opts, Int.MAX_VALUE, fileVisitor)
    return files.map { it.toFile() }
}

internal val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)

internal fun deleteDirectory(directory: File) {
    if (!directory.exists()) {
        return
    }
    val fileVisitor = object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            delete(file)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            delete(dir)
            return FileVisitResult.CONTINUE
        }

        private fun delete(path: Path) {
            if (isWindows) {
                Files.setAttribute(path, "dos:readonly", false)
            }

            Files.delete(path)
        }
    }
    Files.walkFileTree(directory.toPath(), fileVisitor)
}
