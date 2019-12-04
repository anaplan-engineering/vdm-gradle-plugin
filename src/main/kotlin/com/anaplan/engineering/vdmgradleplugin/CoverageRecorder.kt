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

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.overture.ast.lex.LexLocation
import org.overture.interpreter.runtime.ModuleInterpreter
import java.io.File

internal class CoverageRecorder(
        private val sourceDir: File?,
        private val coverageDir: File,
        private val logger: Logger
) {

    internal fun recordCoverage(interpreter: ModuleInterpreter) {
        val coverageByFile = generateCoverage(interpreter)
        val totalLocations = coverageByFile.sumBy { it.coverage.size }
        if (totalLocations > 0) {
            val coveredLocations = coverageByFile.sumBy { it.coverage.filter { (_, v) -> v > 0L }.count() }
            val coverage = if (totalLocations == 0) 100.0 else 100.0 * coveredLocations / totalLocations
            logger.info("COVERAGE -- %2.2f%%".format(coverage))

            coverageByFile.forEach {
                val reportFile = File(coverageDir, "${it.moduleName}.html")
                reportFile.writeText(generateCoverageHtml(it))

            }
            generateCoverageStats(coverageByFile, coveredLocations, totalLocations, coverage)
        }
    }

    private fun generateCoverageStats(coverageByFile: List<FileCoverage>, overallCoveredLocations: Int,
                                      overallTotalLocations: Int, overallCoverage: Double) {
        val reportFile = File(coverageDir, "report.html")
        val cellStyle = "border: 1px solid lightgray; padding: 10px"
        val html = buildString {
            appendHTML(xhtmlCompatible = true).html {
                attributes["data-theme"] = "vdm"
                head {
                    meta(charset = "UTF-8")
                    title(content = "Coverage statistics")
                }
                body {
                    h1 { +"Coverage statistics" }
                    h2 { +"Overall coverage" }
                    p {
                        +"Overall $overallCoveredLocations of $overallTotalLocations locations were hit: "
                        b { +"%2.2f%%".format(overallCoverage) }
                    }
                    h2 { +"Breakdown by module" }
                    table {
                        tr {
                            th {
                                style = cellStyle
                                +"File"
                            }
                            th {
                                style = cellStyle
                                +"Covered locations"
                            }
                            th {
                                style = cellStyle
                                +"Total locations"
                            }
                            th {
                                style = cellStyle
                                +"%"
                            }
                        }
                        coverageByFile.forEach { fileCoverage ->
                            val coveredLocations = fileCoverage.coverage.filter { (_, v) -> v > 0L }.count()
                            val totalLocations = fileCoverage.coverage.size
                            val coverage = if (totalLocations == 0) 100.0 else 100.0 * coveredLocations / totalLocations
                            tr {
                                td {
                                    style = cellStyle
                                    a(href = "${fileCoverage.moduleName}.html") {
                                        +fileCoverage.file.name
                                    }
                                } // TODO -- link to file
                                td {
                                    style = cellStyle
                                    +"$coveredLocations"
                                }
                                td {
                                    style = cellStyle
                                    +"$totalLocations"
                                }
                                td {
                                    style = cellStyle
                                    +"%2.2f%%".format(coverage)
                                }
                            }
                        }
                    }
                }
            }
        }
        reportFile.writeText(html)
    }

    private fun generateCoverage(interpreter: ModuleInterpreter) =
            interpreter.modules.filter { sourceDir == null || it.files.all { it.startsWith(sourceDir) } }.flatMap { module ->
                module.files.map { file ->
                    val locationCoverage = LexLocation.getSourceLocations(file).map { location ->
                        Location(
                                location.startLine,
                                location.startPos,
                                location.endLine,
                                location.endPos
                        ) to location.hits
                    }.toMap()
                    FileCoverage(file, module.name.name, file.readText(), locationCoverage)
                }
            }

    private fun escape(text: String) =
            text
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")

    private fun generateCoverageHtml(fileCoverage: FileCoverage): String {
        val lines = fileCoverage.text.lines()
        val locations = fileCoverage.coverage.keys.sortedWith(locationComparator)
        var currentLine = 1
        var currentPos = 1
        val htmlBuilder = StringBuilder("<html>\n<pre>\n")
        fun copyText(startLine: Int, startPos: Int, endLine: Int, endPos: Int) {
            (startLine - 1 until endLine).forEach { line ->
                val lastIndex = lines[line].length
                val startIndex = if (line == (startLine - 1)) maxOf(startPos - 1, 0) else 0
                val endIndex = if (line == (endLine - 1)) maxOf(endPos - 1, 0) else lastIndex
                if (startIndex == lastIndex) {
                    htmlBuilder.append("\n")
                } else if (endIndex == lastIndex) {
                    htmlBuilder.append(escape(lines[line].substring(startIndex)))
                    htmlBuilder.append("\n")
                } else {
                    htmlBuilder.append(escape(lines[line].substring(startIndex, endIndex)))
                }
            }
        }
        locations.forEach { location ->
            val hitCount = fileCoverage.coverage[location] as Long
            if (location.startLine < currentLine || (location.startLine == currentLine && location.startPos < currentPos)) {
                throw IllegalStateException("Overlapping locations!")
            }
            if (location.startLine != currentLine || location.startPos != currentPos) {
                copyText(currentLine, currentPos, location.startLine, location.startPos)
            }
            if (hitCount > 0) {
                htmlBuilder.append("<span title=\"Hit $hitCount times\" style=\"background-color: #c4ffc5;\">")
                copyText(location.startLine, location.startPos, location.endLine, location.endPos)
                htmlBuilder.append("</span>")
            } else {
                htmlBuilder.append("<span style=\"background-color: #ff9696;\">")
                copyText(location.startLine, location.startPos, location.endLine, location.endPos)
                htmlBuilder.append("</span>")
            }
            currentLine = location.endLine
            currentPos = location.endPos
        }
        if (currentLine < lines.size || currentPos < lines[lines.size - 1].length) {
            copyText(currentLine, currentPos, lines.size, lines[lines.size - 1].length + 1)
        }

        htmlBuilder.append("\n</pre>\n</html>")
        return htmlBuilder.toString()
    }

    private data class FileCoverage(
            val file: File,
            val moduleName: String,
            val text: String,
            val coverage: Map<Location, Long>
    )

    private data class Location(
            val startLine: Int,
            val startPos: Int,
            val endLine: Int,
            val endPos: Int
    )

    companion object {
        private val locationComparator = Comparator<Location> { o1, o2 ->
            val startLineCompare = o1.startLine.compareTo(o2.startLine)
            if (startLineCompare != 0) {
                startLineCompare
            } else {
                val startPosCompare = o1.startPos.compareTo(o2.startPos)
                if (startPosCompare != 0) {
                    startPosCompare
                } else {
                    val endLineCompare = o1.endLine.compareTo(o2.endLine)
                    if (endLineCompare != 0) {
                        endLineCompare
                    } else {
                        o1.endPos.compareTo(o2.endPos)
                    }
                }
            }
        }
    }

}


