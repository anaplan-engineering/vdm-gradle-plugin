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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.api.tasks.options.Option
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File


internal const val test = "test"

internal fun Project.addTestTask() {
    createVdmTask(test, VdmTestRunTask::class.java)
    afterEvaluate {
        val testTask = tasks.getByName(test) ?: throw TaskInstantiationException("Cannot find VDM test task")
        testTask.dependsOn(typeCheckTests)
        val checkTask = tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
                ?: throw TaskInstantiationException("Cannot find check task")
        checkTask.dependsOn(test)
    }
}

@CacheableTask
open class VdmTestRunTask() : OvertureTask() {

    val recordCoverage: Boolean
        @Input
        get() = project.vdmConfig.recordCoverage

    val testLaunchGeneration: TestLaunchGeneration
        @Input
        get() = project.vdmConfig.testLaunchGeneration

    val reportDir: File
        @OutputDirectory
        get() = File(project.vdmBuildDir, "junitreports")

    val coverageDir: File
        @OutputDirectory
        get() = File(project.vdmBuildDir, "coverage")

    val launchDir: File
        @OutputDirectory
        get() = File(project.vdmBuildDir, "testLaunch")

    private var testFilter: String = "Test.*"
        @Option(option = "tests", description = "Filter the tests to be run")
        set(value) { field = value }

    override fun exec() {
        if (dialect != Dialect.vdmsl) {
            // Would prefer to throw TaskExecutionException, but it requires a task as argument
            throw IllegalStateException("Test running only defined for VDM-SL currently")
        }
        jvmArgs = project.vdmConfig.overtureJvmArgs
        super.setArgs(constructArgs())
        super.setClasspath(project.files(createClassPathJar()))
        super.exec()
    }

    private fun constructArgs() =
            if (recordCoverage) {
                listOf("--coverage-target-dir", coverageDir.absolutePath)
            } else {
                emptyList()
            } +
                    listOf(
                            "--log-level", project.gradle.startParameter.logLevel,
                            "--run-tests", true,
                            "--test-filter", testFilter,
                            "--report-target-dir", reportDir.absolutePath,
                            "--launch-target-dir", launchDir.absolutePath,
                            "--test-launch-generation", testLaunchGeneration.name,
                            "--test-launch-project-name", project.name,
                            "--coverage-source-dir", project.vdmSourceDir.absolutePath,
                            "--test-source-dir", project.vdmTestSourceDir.absolutePath,
                            "--monitor-memory", project.vdmConfig.monitorOvertureMemory
                    ) + project.locateAllSpecifications(dialect, true).map { it.absolutePath }

}