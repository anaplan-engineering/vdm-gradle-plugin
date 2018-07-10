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

internal fun executeBuild(
        projectDir: File,
        tasks: Array<String> = arrayOf("build"),
        fail: Boolean = false
): BuildResult {
    println("Executing build in ${projectDir.absolutePath}")
    val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("clean", *tasks, "--info", "--stacktrace")
            .forwardStdOutput(File(projectDir, "out.log").printWriter())
            .forwardStdError(File(projectDir, "err.log").printWriter())
            .withPluginClasspath()
    return if (fail) {
        runner.buildAndFail()
    } else {
        runner.build()
    }
}