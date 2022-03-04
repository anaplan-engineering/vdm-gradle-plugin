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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object TestRunner {

    val functionalTestClasspathJar by lazy {
        System.getProperty("functionalTestClasspathJar")
    }

    internal fun executeBuild(
        projectDir: File,
        clean: Boolean = true,
        tasks: Array<String> = arrayOf("build"),
        fail: Boolean = false
    ): BuildResult {
        println("Executing build in ${projectDir.absolutePath}")
        setClasspath(File(projectDir, "build.gradle"))
        return runBuild(clean, projectDir, tasks, fail)
    }

    internal fun executeCompositeBuild(
        projectDir: File,
        clean: Boolean = true,
        tasks: Array<String> = arrayOf("build"),
        fail: Boolean = false
    ): BuildResult {
        println("Executing composite build in ${projectDir.absolutePath}")
        Files.walkFileTree(projectDir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if (Files.isRegularFile(file) and file.fileName.toString().endsWith(".gradle")) {
                    setClasspath(file.toFile())
                }
                return FileVisitResult.CONTINUE
            }
        })
        return runBuild(clean, projectDir, tasks, fail)
    }

    private fun runBuild(clean: Boolean, projectDir: File, tasks: Array<String>, fail: Boolean): BuildResult {
        val cleanTasks = if (clean) arrayOf("clean") else arrayOf()
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*cleanTasks, *tasks, "--info", "--stacktrace")
            .forwardStdOutput(File(projectDir, "out.log").printWriter())
            .forwardStdError(File(projectDir, "err.log").printWriter())
        return if (fail) {
            runner.buildAndFail()
        } else {
            runner.build()
        }
    }

    private fun setClasspath(gradleFile: File) {
        gradleFile.writeText(gradleFile.readText().replace("%functionalTestClasspath.jar%", functionalTestClasspathJar))
    }

}
