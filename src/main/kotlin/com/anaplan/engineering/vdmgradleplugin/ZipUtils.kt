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

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal fun createZip(contents : List<File>, baseDir : File, outputFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { out ->
        contents.forEach { file ->
            FileInputStream(file).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    val relativeFileName = baseDir.toPath().relativize(file.toPath()).toString()
                    val entry = ZipEntry(relativeFileName)
                    out.putNextEntry(entry)
                    bufferedInputStream.copyTo(out, 1024)
                }
            }
        }
    }
}

internal fun extractZip(zipFile : File, outputDir: File) {
    ZipFile(zipFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            zip.getInputStream(entry).use { input ->
                val outputFile = File(outputDir, entry.name)
                outputFile.parentFile.mkdirs()
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}