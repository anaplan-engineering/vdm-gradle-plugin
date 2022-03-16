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

import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeHtmlRenderStrategy
import com.anaplan.engineering.vdmprettyprinter.PrettyPrintConfig
import com.anaplan.engineering.vdmprettyprinter.VdmPrettyPrinter
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.overture.ast.definitions.AValueDefinition
import org.overture.ast.definitions.PDefinition
import org.overture.ast.modules.AModuleModules
import org.overture.ast.node.INode
import org.overture.interpreter.runtime.Interpreter
import org.overture.interpreter.runtime.ModuleInterpreter
import org.overture.interpreter.util.ExitStatus
import org.overture.parser.util.ParserUtil
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


const val docGen = "docGen"

internal fun Project.addDocGenTask() {
    createVdmTask(docGen, DocGenTask::class.java)
    afterEvaluate {
        it.tasks.matching { it.name == docGen }.forEach { it.dependsOn(typeCheckTests) }
    }
}

private val prettyPrinter = VdmPrettyPrinter(MathematicalUnicodeHtmlRenderStrategy(header = "", footer = ""))

@CacheableTask
open class DocGenTask : DefaultTask() {

    val dialect: Dialect
        @Input
        get() = project.vdmConfig.dialect

    val specificationFiles: List<File>
        @PathSensitive(PathSensitivity.RELATIVE)
        @InputFiles
        get() = project.locateAllSpecifications(dialect, true).map { File(it.absolutePath) }

    val vdmGenDocsDir: File
        @OutputDirectory
        get() = project.vdmGenDocsDir

    private val resourceTypes
        get() = project.vdmConfig.resourceFileTypes

    val resourceFiles: FileCollection
        @PathSensitive(PathSensitivity.RELATIVE)
        @InputFiles
        get() = project.files(locateFilesWithExtension(project.vdmMdDir, *resourceTypes))

    val mdSourceFiles: FileCollection
        @PathSensitive(PathSensitivity.RELATIVE)
        @InputFiles
        get() = project.files(locateFilesWithExtension(project.vdmMdDir, "md"))

    val mdDependencies: FileCollection
        @PathSensitive(PathSensitivity.RELATIVE)
        @InputFiles
        get() = project.files(locateFilesWithExtension(project.vdmMdDependencyDir, "md", *resourceTypes))

    val logUnhandledCases: Boolean
        @Input
        get() = project.vdmConfig.prettyPrinter.logUnhandledCases

    val minListLengthToUseNls: Int
        @Input
        get() = project.vdmConfig.prettyPrinter.minListLengthToUseNls

    private val mdRegex = Regex("\\.md\$")

    private val isMainModule: (AModuleModules) -> Boolean = { module ->
        module.files.all { file ->
            file.startsWith(project.vdmSourceDir) || file.startsWith(project.vdmDependencyDir)
        }
    }

    private val isTestModule: (AModuleModules) -> Boolean = { module ->
        module.files.all { file ->
            file.startsWith(project.vdmTestSourceDir) || file.startsWith(project.vdmTestDependencyDir)
        }
    }

    private val isModuleDependency: (AModuleModules) -> Boolean = { module ->
        module.files.all { file ->
            file.startsWith(project.vdmDependencyDir) || file.startsWith(project.vdmTestDependencyDir)
        }
    }

    @TaskAction
    fun generateDocuments() {
        if (dialect != Dialect.vdmsl) {
            logger.info("Skipping as doc generation only defined for VDM-SL currently")
            return
        }
        val interpreter = loadSpecification(specificationFiles) as? ModuleInterpreter
            ?: // this should never happen as we have limited dialect to VDM-SL
            throw GradleException("Interpreter is not a container interpreter!")

        val sharedFiles = SharedFiles(
            cssFile = createCssFile(),
            modulesDirectory = File(vdmGenDocsDir, "modules"),
            testModulesDirectory = File(vdmGenDocsDir, "testModules")
        )
        processMarkdownFiles(interpreter, sharedFiles, project.vdmMdDir, vdmGenDocsDir)
        if (project.vdmMdDependencyDir.exists()) {
            locateImmediateSubDirectories(project.vdmMdDependencyDir).forEach { dependencyDir ->
                val targetDir = File(vdmGenDocsDir, dependencyDir.name)
                processMarkdownFiles(interpreter, sharedFiles, dependencyDir, targetDir)
                val resourceFiles = project.files(locateFilesWithExtension(dependencyDir, *resourceTypes))
                copyAdditionalResources(resourceFiles, dependencyDir, targetDir)
            }
        }
        generateModuleAppendix(
            "Modules",
            interpreter.modules.filter(isMainModule),
            sharedFiles.modulesDirectory,
            sharedFiles.cssFile
        )
        generateModuleAppendix(
            "Test modules",
            interpreter.modules.filter(isTestModule),
            sharedFiles.testModulesDirectory,
            sharedFiles.cssFile
        )

        copyAdditionalResources(resourceFiles, project.vdmMdDir, vdmGenDocsDir)
    }

    private fun loadSpecification(specificationFiles: List<File>, typeCheck: Boolean = false): Interpreter {
        val dialect = project.vdmConfig.dialect
        val controller = dialect.createController()
        val parseStatus = controller.parse(specificationFiles)
        if (parseStatus != ExitStatus.EXIT_OK) {
            throw GradleException("VDM specification cannot be parsed")
        }
        if (typeCheck) {
            val typeCheckStatus = controller.typeCheck()
            if (typeCheckStatus != ExitStatus.EXIT_OK) {
                throw GradleException("VDM specification does not type check")
            }
        }
        return controller.getInterpreter()
    }

    private fun copyAdditionalResources(
        resourceFiles: FileCollection,
        sourceDirectory: File,
        targetDirectory: File
    ) {
        resourceFiles.forEach { file ->
            val targetFile = File(targetDirectory, file.relativeTo(sourceDirectory).path)
            if (!targetFile.parentFile.exists()) {
                targetFile.parentFile.mkdirs()
            }
            file.copyTo(targetFile, overwrite = true)
        }
    }

    private fun processMarkdownFiles(
        interpreter: ModuleInterpreter,
        sharedFiles: SharedFiles,
        sourceDirectory: File,
        targetDirectory: File
    ) {
        val mdSourceFiles = locateFilesWithExtension(sourceDirectory, "md")
        val tableExtension = TablesExtension.create()
        mdSourceFiles.forEach { mdSourceFile ->
            val genDocFile =
                File(targetDirectory, mdSourceFile.relativeTo(sourceDirectory).path.replace(mdRegex, ".html"))
            val pathToModules = sharedFiles.modulesDirectory.relativeTo(mdSourceFile.parentFile).path
            val pathToTestModules = sharedFiles.testModulesDirectory.relativeTo(mdSourceFile.parentFile).path
            val pathToComponent = { groupId: String, artifactId: String ->
                val componentDirectory = File(vdmGenDocsDir, "$groupId/$artifactId")
                componentDirectory.relativeTo(genDocFile.parentFile).path
            }
            val vdmContext = VdmContext(
                interpreter,
                isMainModule,
                isTestModule,
                isModuleDependency,
                pathToModules,
                pathToTestModules,
                pathToComponent
            )
            val vdmExtension = VdmMdExtension(project.vdmConfig.prettyPrinter, vdmContext)
            val mdParser = Parser.builder().extensions(listOf(vdmExtension, tableExtension)).build()
            val htmlRenderer =
                HtmlRenderer.builder().extensions(listOf(vdmExtension, tableExtension)).attributeProviderFactory({ _ ->
                    AutoIdHeadingAttributeProvider()
                }).build()
            val mdDoc = mdParser.parse(mdSourceFile.readText())
            // TODO - pretty print the body/final text (ideally using kotlinx.html, but can't see how to do this easily)
            val htmlDoc = addMetadata(
                mdSourceFile.nameWithoutExtension,
                htmlRenderer.render(mdDoc),
                sharedFiles.cssFile.relativeTo(genDocFile.parentFile)
            )
            if (!genDocFile.parentFile.exists()) {
                genDocFile.parentFile.mkdirs()
            }
            genDocFile.writeText(htmlDoc)
        }
    }

    private fun createCssFile(): File {
        val vdmCss = javaClass.getResource("/css/vdm.css").readText()
        val cssFile = vdmGenDocsDir.resolve("vdm.css")
        if (!cssFile.exists()) {
            if (!cssFile.parentFile.exists()) {
                cssFile.parentFile.mkdirs()
            }
            cssFile.createNewFile()
        }
        cssFile.writeText(vdmCss)
        return cssFile
    }

    private fun addMetadata(title: String, bodyText: String, relativeCssFile: File) =
        buildString {
            appendLine("<!DOCTYPE html>")
            appendHTML(xhtmlCompatible = true).html {
                attributes["data-theme"] = "vdm"
                head {
                    meta(charset = "UTF-8")
                    link(rel = "stylesheet", type = "text/css", href = relativeCssFile.path)
                    title(content = title)
                }
                body { unsafe { +bodyText } }
            }
        }

    private fun generateModuleAppendix(
        pageTitle: String,
        modules: List<AModuleModules>,
        moduleDirectory: File,
        cssFile: File
    ) {
        if (!moduleDirectory.exists()) {
            moduleDirectory.mkdirs()
        }
        File(moduleDirectory, "index.html").writeText(renderModuleAppendixSummary(pageTitle, modules))
        modules.forEach { module ->
            val name = module.name.name
            val genDocFile = File(moduleDirectory, "$name.html")
            if (!genDocFile.parentFile.exists()) {
                genDocFile.parentFile.mkdirs()
            }
            val config = project.vdmConfig.prettyPrinter.toConfig()
            val prettyPrintedModule = prettyPrinter.prettyPrint(module, config)
            genDocFile.writeText(addMetadata(name, prettyPrintedModule, cssFile.relativeTo(genDocFile.parentFile)))
        }
    }
}

private data class SharedFiles(
    val cssFile: File,
    val modulesDirectory: File,
    val testModulesDirectory: File
)


private class AutoIdHeadingAttributeProvider : AttributeProvider {
    companion object {
        val alphaNumericRegex = Regex("[^A-Za-z0-9]")
    }

    override fun setAttributes(node: Node, tagName: String, attributes: MutableMap<String, String>) {
        if (node is Heading) {
            val child = node.firstChild
            if (child is Text) {
                attributes.put("id", child.literal.replace(alphaNumericRegex, ""))
            }
        }
    }
}

private data class VdmContext(
    val interpreter: ModuleInterpreter,
    val isMainModule: (AModuleModules) -> Boolean,
    val isTestModule: (AModuleModules) -> Boolean,
    val isModuleDependency: (AModuleModules) -> Boolean,
    val pathToModules: String,
    val pathToTestModules: String,
    val pathToComponent: (String, String) -> String
)

private class VdmMdExtension(private val config: PrettyPrinterConfig, private val vdmContext: VdmContext) :
    Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory({ cxt ->
            VdmHtmlNodeRenderer(config, vdmContext, cxt)
        })
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(VdmDelimiterProcessor())
    }
}

private class VdmNode : CustomNode(), Delimited {
    var vdmReference: String? = null

    override fun getOpeningDelimiter() = "{"
    override fun getClosingDelimiter() = "}"
}

private abstract class VdmNodeRenderer : NodeRenderer {
    override fun getNodeTypes() = setOf(VdmNode::class.java)
}

// TODO - references assume we're in root directory
private class VdmHtmlNodeRenderer(
    private val config: PrettyPrinterConfig,
    private val vdmContext: VdmContext,
    private val cxt: HtmlNodeRendererContext
) : VdmNodeRenderer() {
    override fun render(node: Node) {
        if (node !is VdmNode) {
            throw IllegalStateException()
        }
        val vdmNodeText = node.vdmReference ?: throw IllegalStateException()
        if (vdmNodeText.startsWith("@")) {
            renderDirective(vdmNodeText.drop(1))
        } else {
            renderVdm(vdmNodeText)
        }
    }

    private fun renderDirective(directive: String) {
        val commandAndParams = directive.split(Regex(":"))
        val params = commandAndParams.drop(1)
        when (commandAndParams.first()) {
            "mainModuleList" -> renderModuleList(
                vdmContext.interpreter.getModules().filterNot(vdmContext.isModuleDependency)
                    .filter(vdmContext.isMainModule), "modules"
            )
            "testModuleList" -> renderModuleList(
                vdmContext.interpreter.getModules().filterNot(vdmContext.isModuleDependency)
                    .filter(vdmContext.isTestModule), "testModules"
            )
            "a" -> renderAnchor(params)
            "page" -> renderPageLink(params)
            "link" -> renderLink(params.first())
            "ref" -> renderDefinition(params.first(), params.drop(1))
            else -> throw IllegalStateException("Illegal VDM directive: $directive")
        }
    }

    private fun renderPageLink(args: List<String>) {
        fun sanitiseFileName(link: String): String {
            val linkParts = link.split("#")
            val (fileName, anchor) = if (linkParts.size == 1) {
                Pair(linkParts[0], null)
            } else if (linkParts.size == 2) {
                Pair(linkParts[0], linkParts[1])
            } else {
                throw IllegalStateException("Invalid formatting of link '$link' (multiple #s)")
            }

            val targetFile = if (fileName.endsWith(".md")) {
                "${fileName.dropLast(3)}.html"
            } else if (fileName.endsWith(".html")) {
                fileName
            } else {
                "$fileName.html"
            }

            return if (anchor == null) {
                targetFile
            } else {
                "$targetFile#$anchor"
            }
        }


        val (pathToFile, displayText) = if (args.size == 2) { // local link
            val (fileName, displayText) = args
            Pair(sanitiseFileName(fileName), displayText)
        } else if (args.size == 4) { // x-component link
            val (groupId, artifactId, fileName, displayText) = args
            Pair(vdmContext.pathToComponent(groupId, artifactId) + "/" + sanitiseFileName(fileName), displayText)
        } else {
            throw IllegalArgumentException("Incorrectly formatted page directive:\nShould be {@page:fileName:displayText}' for local links or {@page:groupId:artifactId:fileName:displayText} for cross component links.")
        }
        // TODO - check file exists?
        cxt.writer.raw(htmlSnippet().a(href = pathToFile) { +displayText })
    }

    private fun renderAnchor(names: List<String>) =
        names.forEach { name ->
            cxt.writer.raw(htmlSnippet().div { id = name })
        }

    private fun renderModuleList(modules: List<AModuleModules>, directory: String) {
        cxt.writer.raw(htmlSnippet().ul {
            modules.sortedBy { it.name.name }.map { module ->
                val name = module.name.name
                li { a(href = "$directory/$name.html") { +name } }
            }
        })
    }

    private fun determineMinListLengthToUseNls(params: List<String>) =
        params.find { it.startsWith("minListLengthToUseNls") }?.split(Regex("="))?.last()?.trim()?.toIntOrNull()
            ?: config.minListLengthToUseNls

    private fun renderDefinition(vdmReference: String, params: List<String>) {
        val (moduleName, definitionName) = vdmReference.split(Regex("`"))
        val module = findModuleOrDie(moduleName)
        val definition = findDefinitionOrDie(module, definitionName)
        val definitionText = prettyPrinter.prettyPrint(
            definition,
            PrettyPrintConfig(
                includeHeaderFooter = false, minListLengthToUseNls = determineMinListLengthToUseNls(params),
                logUnhandledCases = config.logUnhandledCases
            )
        )
        cxt.writer.raw(htmlSnippet().a(href = "${getModuleDirectory(module)}/$moduleName.html#$definitionName") {
            attributes["class"] = "vdm-cite"
            blockQuote { p { unsafe { +definitionText } } }
        })
    }

    private fun findDefinitionOrDie(module: AModuleModules, definitionName: String): PDefinition {
        return module.defs?.find { def ->
            def.name?.name == definitionName || (def is AValueDefinition && def.pattern.toString() == definitionName)
        } ?: throw IllegalStateException("Cannot find definition ${module.name}`$definitionName")
    }

    private fun renderLink(vdmReference: String) =
        if (vdmReference.contains("`")) {
            val (moduleName, definitionName) = vdmReference.split(Regex("`"))
            renderDefinitionLink(moduleName, definitionName)
        } else {
            renderModuleLink(vdmReference)
        }

    private fun renderModuleLink(moduleName: String) {
        val module = findModuleOrDie(moduleName)
        cxt.writer.raw(htmlSnippet().a(href = "${getModuleDirectory(module)}/$moduleName.html") { +moduleName })
    }

    private fun renderDefinitionLink(moduleName: String, definitionName: String) {
        val module = findModuleOrDie(moduleName)
        findDefinitionOrDie(module, definitionName)
        cxt.writer.raw(htmlSnippet().a(href = "${getModuleDirectory(module)}/$moduleName.html#$definitionName") { +"$moduleName`$definitionName" })
    }

    private fun htmlSnippet() = createHTML(xhtmlCompatible = true)

    private fun getModuleDirectory(module: AModuleModules) =
        if (vdmContext.isMainModule(module)) vdmContext.pathToModules else vdmContext.pathToTestModules

    private fun findModuleOrDie(moduleName: String): AModuleModules {
        return vdmContext.interpreter.modules.find { module ->
            module.name.name == moduleName
        } ?: throw IllegalStateException("Cannot find container $moduleName")
    }

    private fun renderVdm(vdmText: String) {
        val node: INode = ParserUtil.parseExpression(vdmText).result
        val definitionText = prettyPrinter.prettyPrint(
            node,
            PrettyPrintConfig(
                includeHeaderFooter = false, minListLengthToUseNls = config.minListLengthToUseNls,
                logUnhandledCases = config.logUnhandledCases
            )
        )
        cxt.writer.raw(definitionText)
    }
}

private class VdmDelimiterProcessor : DelimiterProcessor {
    override fun getClosingCharacter() = '}'
    override fun getOpeningCharacter() = '{'
    override fun getMinLength() = 1
    override fun getDelimiterUse(opener: DelimiterRun?, closer: DelimiterRun?) = 1

    override fun process(opener: Text, closer: Text, delimiterUse: Int) {
        val vdmNode = VdmNode()
        var tmp = opener.getNext()
        while (tmp != null && tmp != closer) {
            if (tmp is Text && vdmNode.vdmReference == null) {
                vdmNode.vdmReference = tmp.literal
                vdmNode.appendChild(tmp)
            } else {
                // TODO - make this more useful
                throw GradleException("Vdm reference must be single reference")
            }
            tmp = tmp.next
        }
        opener.insertAfter(vdmNode)
    }
}

fun locateImmediateSubDirectories(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    val dirs = ArrayList<Path>()
    val fileVisitor = object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (dir == directory.toPath()) {
                return FileVisitResult.CONTINUE
            }
            dirs.add(dir)
            return FileVisitResult.SKIP_SUBTREE
        }
    }
    Files.walkFileTree(directory.toPath(), fileVisitor)
    return dirs.map { it.toFile() }
}

fun renderModuleAppendixSummary(pageTitle: String, modules: List<AModuleModules>) =
    buildString {
        appendHTML(xhtmlCompatible = true).html {
            attributes["data-theme"] = "vdm"
            head {
                meta(charset = "UTF-8")
                link(rel = "stylesheet", type = "text/css", href = "../vdm.css")
                title(content = pageTitle)
            }
            body {
                h1 { +pageTitle }
                ul {
                    modules.map { it.name.name }.sorted().forEach { name ->
                        li {
                            a(href = "$name.html") { +name }
                        }
                    }
                }
            }
        }
    }
