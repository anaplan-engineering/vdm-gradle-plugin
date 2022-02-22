package com.anaplan.engineering.vdmgradleplugin

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.junit.Assert
import org.junit.Test
import org.overture.ast.intf.lex.ILexIdentifierToken
import org.overture.ast.modules.AModuleModules

class DocumentGeneratorTest {

    @Test
    fun renderModuleAppendixSummary() {
        val modules = listOf("b", "d", "c", "a").map {
            val token = mock<ILexIdentifierToken>()
            whenever(token.name).thenReturn(it)
            val module = mock<AModuleModules>()
            whenever(module.name).thenReturn(token)
            module
        }

        Assert.assertEquals("""
              |<html data-theme="vdm">
              |  <head>
              |    <meta charset="UTF-8"/>
              |    <link href="../vdm.css" rel="stylesheet" type="text/css"/>
              |    <title>summary</title>
              |  </head>
              |  <body>
              |    <h1>summary</h1>
              |    <ul>
              |      <li><a href="a.html">a</a></li>
              |      <li><a href="b.html">b</a></li>
              |      <li><a href="c.html">c</a></li>
              |      <li><a href="d.html">d</a></li>
              |    </ul>
              |  </body>
              |</html>
              |
            """.trimMargin(), renderModuleAppendixSummary("summary", modules))

    }

    @Test
    fun dummy() {
        val tableExtension = TablesExtension.create()
        val mdParser = Parser.builder().extensions(listOf(tableExtension)).build()
        val htmlRenderer = HtmlRenderer.builder().extensions(listOf(tableExtension)).build()
        val mdDoc = mdParser.parse("# hello\nthere")
        println(htmlRenderer.render(mdDoc))
    }
}