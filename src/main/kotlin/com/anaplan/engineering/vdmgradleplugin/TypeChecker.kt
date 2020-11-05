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

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import java.io.File

const val typeCheck = "typeCheck"
const val typeCheckTests = "typeCheckTests"

internal fun Project.addTypeCheckTasks() {
    createVdmTask(typeCheck, VdmTypeCheckMainTask::class.java)
    createVdmTask(typeCheckTests, VdmTypeCheckTestTask::class.java)
    afterEvaluate {
        it.tasks.matching { it.name == typeCheck || it.name == typeCheckTests }.forEach { it.dependsOn(dependencyUnpack) }
    }
}

open class VdmTypeCheckMainTask() : VdmTypeCheckTask(false)

open class VdmTypeCheckTestTask() : VdmTypeCheckTask(true)

open class VdmTypeCheckTask(private val includeTests: Boolean) : OvertureTask() {

    val specificationFiles: FileCollection
        @InputFiles
        get() = project.locateAllSpecifications(dialect, includeTests)

    val generatedLibFile: File
        @OutputFile
        get() = project.generatedLibFile


    override fun exec() {
        logger.info("VDM dialect: $dialect")
        jvmArgs = project.vdmConfig.overtureJvmArgs
        println("111JVM ARGS: ${project.vdmConfig.overtureJvmArgs}")
        println("222JVM ARGS: ${jvmArgs}")
        println(maxHeapSize)
        super.setArgs(constructArgs())
        super.setClasspath(project.files(createClassPathJar()))
        super.exec()
    }

    private fun constructArgs() =
            listOf(
                    "--dialect", dialect.name,
                    "--log-level", project.gradle.startParameter.logLevel,
                    "--run-tests", false,
                    "--output-lib", generatedLibFile.absolutePath
            ) + specificationFiles.map { it.absolutePath }

}

