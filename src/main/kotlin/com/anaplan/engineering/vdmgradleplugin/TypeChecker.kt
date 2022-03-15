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
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

const val typeCheck = "typeCheck"
const val typeCheckTests = "typeCheckTests"

internal fun Project.addTypeCheckTasks() {
    createVdmTask(typeCheck, VdmTypeCheckMainTask::class.java)
    createVdmTask(typeCheckTests, VdmTypeCheckTestTask::class.java)
    afterEvaluate {
        it.tasks.matching { it.name == typeCheck || it.name == typeCheckTests }.forEach { it.dependsOn(dependencyUnpack) }
    }
}

@CacheableTask
open class VdmTypeCheckMainTask() : VdmTypeCheckTask(false)

@CacheableTask
open class VdmTypeCheckTestTask() : VdmTypeCheckTask(true)

open class VdmTypeCheckTask(private val includeTests: Boolean) : OvertureTask() {

    private val specificationFiles: FileCollection
        @PathSensitive(PathSensitivity.RELATIVE)
        @InputFiles
        get() = project.locateAllSpecifications(dialect, includeTests)

    // Only needed to ensure caching of the Gradle task
    private val logFile: File
        @OutputFile
        get() = File(project.vdmBuildDir, "typeCheckTask.log")

    override fun exec() {
        logger.info("VDM dialect: $dialect")
        if (specificationFiles.isEmpty) {
            logger.info("No files found")
        } else {
            standardOutput = ByteArrayOutputStream()
            jvmArgs = project.vdmConfig.overtureJvmArgs
            setArgs(constructArgs())
            classpath = project.files(createClassPathJar())
            super.exec()
            writeOutput(standardOutput)
        }
    }

    private fun writeOutput(os: OutputStream) {
        val output = os.toString()
        logFile.writeText(output)
        output.lineSequence().forEach {
            logger.log(getLogLevel(it), it)
        }
    }

    private fun getLogLevel(line: String) = when {
        line.startsWith("Error") -> LogLevel.ERROR
        line.startsWith("Warning") -> LogLevel.WARN
        line.startsWith("Parsed") || line.startsWith("Type checked") -> LogLevel.INFO
        else -> LogLevel.LIFECYCLE
    }

    private fun constructArgs() =
            listOf(
                    "--dialect", dialect.name,
                    "--log-level", project.gradle.startParameter.logLevel,
                    "--run-tests", false,
                    "--monitor-memory", project.vdmConfig.monitorOvertureMemory
            ) + specificationFiles.map { it.absolutePath }

}

