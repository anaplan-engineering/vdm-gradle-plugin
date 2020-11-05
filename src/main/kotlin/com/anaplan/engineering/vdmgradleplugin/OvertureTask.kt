package com.anaplan.engineering.vdmgradleplugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

abstract class OvertureTask : JavaExec() {


    val dialect: Dialect
        @Input
        get() = project.vdmConfig.dialect


    init {
        mainClass.set("com.anaplan.engineering.vdmgradleplugin.OvertureWrapperKt")
        jvmArgs = project.vdmConfig.overtureJvmArgs
    }

    protected fun createClassPathJar(): File {
        val classpath = getAllClassPathConfig(project)
                .plus(project.files(project.vdmLibDependencyDir.listFiles()?.filter { it.extension == "jar"}))
                .filter { it.extension == "jar" }
        val manifestClassPath = classpath.map { it.toURI() }.joinToString(" ")
        val manifest = Manifest()
        val attributes = manifest.mainAttributes
        attributes[Attributes.Name.MANIFEST_VERSION] = "1.0.0"
        attributes[Attributes.Name("Class-Path")] = manifestClassPath

        val jarFile = File(project.vdmBuildDir, "overtureClassPath.jar")
        val os: OutputStream = FileOutputStream(jarFile)
        JarOutputStream(os, manifest).close()
        return jarFile
    }

}

private fun getAllClassPathConfig(project: Project): FileCollection {
    val config = project.buildscript.configurations.getByName("classpath")
    return if (project.parent == null) {
        config
    } else {
        config.plus(getAllClassPathConfig(project.parent!!))
    }
}