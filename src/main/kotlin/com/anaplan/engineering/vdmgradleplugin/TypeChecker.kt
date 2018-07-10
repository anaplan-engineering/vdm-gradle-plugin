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
import org.overture.interpreter.util.ExitStatus

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

// TODO -- don't evaluate if unchanged --- https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:up_to_date_checks
open class VdmTypeCheckTask(private val includeTests: Boolean) : DefaultTask() {

    /*
    Various strategies have been attempted here to make use of binaries and to split out the parse phase from the type
    check phase, but Overture doesn't really help.

    We should look to implement the following in Overture and then revisit:
    - produce non-type checked binary format (for parse phase)
    - do not include loaded libs when writing out binary (otherwise we have transitive issues) so that we can:
        * store main and test in separate libs
        * publish and depend upon libs

     If we try and create a binary for the main and then load that in test we get false warnings of duplicate declarations.
     These become very distracting and obfuscate real issues, so we have reverted to starting from scratch in each task.
     */
    @TaskAction
    fun typeCheck() {
        val dialect = project.vdmConfig.dialect
        logger.info("VDM dialect: $dialect")

        val controller = dialect.createController()

        val files = locateSpecifications(project.vdmDependencyDir, dialect) +
                locateSpecifications(project.vdmSourceDir, dialect) +
                if (includeTests) {
                    locateSpecifications(project.vdmTestDependencyDir, dialect) +
                            locateSpecifications(project.vdmTestSourceDir, dialect)
                } else {
                    listOf()
                }

        logger.debug("Specification files found: ")
        files.forEach { logger.debug(" * $it") }

        if (!project.generatedLibFile.parentFile.exists()) {
            project.generatedLibFile.parentFile.mkdirs()
        }
        controller.setOutfile(project.generatedLibFile.absolutePath)

        val parseStatus = controller.parse(files)
        if (parseStatus != ExitStatus.EXIT_OK) {
            throw GradleException("VDM parse failed")
        }
        val typeCheckStatus = controller.typeCheck()
        if (typeCheckStatus != ExitStatus.EXIT_OK) {
            throw GradleException("VDM type check failed")
        }
    }
}

