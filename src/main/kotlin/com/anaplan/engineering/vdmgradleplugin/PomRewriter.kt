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

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class PomRewriter(val file: File) {

    private fun writeDocument(document: Document) {
        val domSource = DOMSource(document)
        val result = StreamResult(FileWriter(file))
        val transformer = TransformerFactory.newInstance().newTransformer()
        // TODO - indenting is not perfect (first appended node misses a level of indentation)
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
        transformer.transform(domSource, result)
    }

    private fun getUniqueNode(path: String, document: Document): Node? {
        val xPath = XPathFactory.newInstance().newXPath()
        val dependencies = xPath.compile(path).evaluate(document, XPathConstants.NODESET) as NodeList
        return if (dependencies.length == 0) {
            null
        } else {
            if (dependencies.length > 1) {
                throw IllegalStateException("More than one node unexpectedly matches $path in pom")
            }
            dependencies.item(0) as Node
        }
    }

    private fun addDependenciesNode(document: Document) : Node {
        val projectNode = getUniqueNode("/project", document) ?: throw IllegalStateException("No project node in pom file")
        val dependenciesNode = document.createElement("dependencies")
        projectNode.appendChild(dependenciesNode)
        return dependenciesNode
    }

    private fun addDependencyCharacteristic(name: String, value: String?, dependencyNode: Node, document: Document) {
        if (value != null) {
            val element = document.createElement(name)
            val text = document.createTextNode(value)
            element.appendChild(text)
            dependencyNode.appendChild(element)
        }
    }

    fun addDependencies(dependencies: List<Dependency>) {
        val document = XmlHelper.readDocument(file)
        val dependenciesNode = getUniqueNode("/project/dependencies", document) ?: addDependenciesNode(document)
        dependencies.forEach { dependency ->
            val dependencyNode = document.createElement("dependency")
            addDependencyCharacteristic("groupId", dependency.groupId, dependencyNode, document)
            addDependencyCharacteristic("artifactId", dependency.artifactId, dependencyNode, document)
            addDependencyCharacteristic("version", dependency.version, dependencyNode, document)
            addDependencyCharacteristic("classifier", dependency.classifier, dependencyNode, document)
            addDependencyCharacteristic("type", dependency.type, dependencyNode, document)
            addDependencyCharacteristic("scope", dependency.scope, dependencyNode, document)
            dependenciesNode.appendChild(dependencyNode)
        }
        writeDocument(document)
    }

    data class Dependency(
            val groupId: String,
            val artifactId: String,
            val version: String,
            val classifier: String? = null,
            val type: String? = null,
            val scope: String?
    )
}

internal object XmlHelper {
    fun readDocument(file: File): Document {
        val stream = FileInputStream(file)
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return builder.parse(stream)
    }

    fun readDocument(path: Path) = readDocument(path.toFile())

    fun normalizeDocument(path: Path) = readDocument(path).normalizeDocument()
}
