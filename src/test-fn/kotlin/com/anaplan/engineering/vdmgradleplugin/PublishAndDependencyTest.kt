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

import com.anaplan.engineering.vdmgradleplugin.TestRunner.executeBuild
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class PublishAndDependencyTest(
        private val testName: String,
        private val projects: Array<Project>
) {

    data class Project(
            val projectName: String,
            val expectSuccess: Boolean = true,
            val onBuild: (File, File) -> Unit = { _, _ -> }
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = arrayOf(
                test("publishMain",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        })),
                test("publishMainAndTest",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        })),
                test("publishMainAndDocSource",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        })),
                test("noAutoDocGen",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = false, projectName = "a", projectDir = projectDir, repository = repository)
                        })),
                test("publishAndConsumeMain",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        })),
                test("publishAndConsumeLib",
                        Project(projectName = "javalib", onBuild = { _, _ -> }),
                        Project(projectName = "vdmlib", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "vdmlib", projectDir = projectDir, repository = repository)
                            checkLibInstalled(dependencyName = "javalib", projectDir = projectDir)
                        })),
                test("publishAndConsumeTransitiveLib",
                        Project(projectName = "javalib", onBuild = { _, _ -> }),
                        Project(projectName = "vdmlib", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "vdmlib", projectDir = projectDir, repository = repository)
                            checkLibInstalled(dependencyName = "javalib", projectDir = projectDir)
                        }),
                        Project(projectName = "consumer", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "consumer", projectDir = projectDir, repository = repository)
                            checkLibInstalled(dependencyName = "javalib", projectDir = projectDir)
                        })),
                test("publishAndConsumeTransitiveMain",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "c", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "c", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "b", projectDir = projectDir)
                        })),
                test("publishAndConsumeTest",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "a", dependencyType = "test", projectDir = projectDir)
                        })),
                test("publishButDontConsumeTest",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "a", present = false, dependencyType = "test", projectDir = projectDir)
                        })),
                test("publishAndConsumeTransitiveTest",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "a", dependencyType = "test", projectDir = projectDir)
                        }),
                        Project(projectName = "c", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = true, md = false, doc = true, projectName = "c", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "b", projectDir = projectDir)
                            checkDependency(dependencyName = "a", dependencyType = "test", projectDir = projectDir)
                            checkDependency(dependencyName = "b", dependencyType = "test", projectDir = projectDir)
                        })),
                test("publishAndConsumeMd",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "a", dependencyType = "md", projectDir = projectDir)
                        })),
                test("publishButDontConsumeMd",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "a", present = false, dependencyType = "md", projectDir = projectDir)
                        })),
                test("publishAndConsumeTransitiveMd",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "a", dependencyType = "md", projectDir = projectDir)
                        }),
                        Project(projectName = "c", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = true, doc = true, projectName = "c", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "b", projectDir = projectDir)
                            checkDependency(dependencyName = "a", dependencyType = "md", projectDir = projectDir)
                            checkDependency(dependencyName = "b", dependencyType = "md", projectDir = projectDir)
                        })),
                test("diamondDependencyClash",
                        Project(projectName = "a1", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "a2", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository, version = "2.0.0")
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "c", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "c", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "d", expectSuccess = false)),
                test("diamondDependencyClashWithExclusion",
                        Project(projectName = "a1", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "a2", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository, version = "2.0.0")
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "c", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "c", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "d", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "d", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "b", projectDir = projectDir)
                            checkDependency(dependencyName = "c", projectDir = projectDir)
                        })),
                test("diamondDependencyOk",
                        Project(projectName = "a", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "a", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "b", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "b", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "c", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "c", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                        }),
                        Project(projectName = "d", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "d", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "a", projectDir = projectDir)
                            checkDependency(dependencyName = "b", projectDir = projectDir)
                            checkDependency(dependencyName = "c", projectDir = projectDir)
                        })),
                test("splittingISO8601",
                        Project(projectName = "numeric", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "numeric", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "ord", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "ord", projectDir = projectDir, repository = repository)
                        }),
                        Project(projectName = "seq", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "seq", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "ord", projectDir = projectDir)
                            checkDependency(dependencyName = "numeric", projectDir = projectDir)
                        }),
                        Project(projectName = "set", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "set", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "ord", projectDir = projectDir)
                            checkDependency(dependencyName = "numeric", projectDir = projectDir)
                            checkDependency(dependencyName = "seq", projectDir = projectDir)
                        }),
                        Project(projectName = "char", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "char", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "ord", projectDir = projectDir)
                            checkDependency(dependencyName = "numeric", projectDir = projectDir)
                            checkDependency(dependencyName = "seq", projectDir = projectDir)
                            checkDependency(dependencyName = "set", projectDir = projectDir)
                        }),
                        Project(projectName = "iso8601", onBuild = { projectDir, repository ->
                            checkPackagedAndPublished(main = true, test = false, md = false, doc = true, projectName = "iso8601", projectDir = projectDir, repository = repository)
                            checkDependency(dependencyName = "ord", projectDir = projectDir)
                            checkDependency(dependencyName = "numeric", projectDir = projectDir)
                            checkDependency(dependencyName = "seq", projectDir = projectDir)
                            checkDependency(dependencyName = "set", projectDir = projectDir)
                        }))
        )

        private fun checkLibInstalled(dependencyName: String, dependencyVersion: String = "1.0.0", projectDir: File) {
            val libFile = File(projectDir, "lib/$dependencyName-$dependencyVersion.jar")
            Assert.assertTrue(libFile.exists())
        }

        private fun checkDependency(dependencyName: String, dependencyType: String = "", present: Boolean = true, projectDir: File) {
            val dependencyPrefix = if (dependencyType.isEmpty()) "" else "$dependencyType-"
            val dependencyDir = File(projectDir, "build/vdm/${dependencyPrefix}dependencies/testing/$dependencyName")
            Assert.assertEquals(present, dependencyDir.exists())
            if (present) {
                Assert.assertTrue(dependencyDir.isDirectory)
            }
        }

        private fun checkPackagedAndPublished(main: Boolean, test: Boolean, md: Boolean, doc: Boolean, projectName: String, projectDir: File, repository: File, version: String = "1.0.0") {
            checkArtifactPackageAndPublishState(projectName, "", projectDir, repository, main, version)
            checkArtifactPackageAndPublishState(projectName, "test", projectDir, repository, test, version)
            checkArtifactPackageAndPublishState(projectName, "md", projectDir, repository, md, version)
            checkArtifactPackageAndPublishState(projectName, "doc", projectDir, repository, doc, version)
        }

        private fun checkArtifactPackageAndPublishState(projectName: String, classifier: String, projectDir: File, repository: File, packagedAndPublished: Boolean, version: String) {
            val classifierSuffix = if (classifier.isEmpty()) "" else "-$classifier"
            val packageFile = File(projectDir, "build/libs/$projectName-$version$classifierSuffix.zip")
            Assert.assertEquals(packagedAndPublished, packageFile.exists())
            val publishedFile = File(repository, "testing/$projectName/$version/$projectName-$version$classifierSuffix.zip")
            Assert.assertEquals(packagedAndPublished, publishedFile.exists())
            if (packagedAndPublished) {
                Assert.assertEquals(publishedFile.readBytes().toList(), packageFile.readBytes().toList())
            }
        }

        private fun test(
                testName: String,
                vararg projects: Project
        ): Array<Any> = arrayOf(testName, projects)
    }

    @Test
    fun publishAndDependencyTest() {
        val parentDir = File(javaClass.getResource("/$testName").toURI())
        val repository = File(parentDir, ".m2/repository")
        projects.forEach { project ->
            val projectDir = File(parentDir, project.projectName)
            executeBuild(
                    projectDir = projectDir,
                    tasks = arrayOf("test", "publish"),
                    fail = !project.expectSuccess)
            project.onBuild(projectDir, repository)
        }
    }

}