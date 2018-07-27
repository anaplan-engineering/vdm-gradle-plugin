package com.anaplan.engineering.vdmgradleplugin

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Test
import org.overture.ast.intf.lex.ILexIdentifierToken
import org.overture.ast.modules.AModuleModules

class ModuleAppendixSummaryRendererTest {

    @Test
    fun renderTest() {
        val modules = listOf("b", "d", "c", "a").map {
            val token = mock<ILexIdentifierToken>()
            whenever(token.name).thenReturn(it)
            val module = mock<AModuleModules>()
            whenever(module.name).thenReturn(token)
            module
        }

        Assert.assertEquals("""
              |<html data-theme='vdm'>
              |  <head>
              |    <meta charset='UTF-8' />
              |    <link rel='stylesheet' type='text/css' href='../vdm.css' />
              |    <title>summary</title>
              |  </head>
              |  <body>
              |    <h1>summary</h1>
              |    <ul>
              |      <li>
              |        <a href='a.html'>a</a>
              |      </li>
              |      <li>
              |        <a href='b.html'>b</a>
              |      </li>
              |      <li>
              |        <a href='c.html'>c</a>
              |      </li>
              |      <li>
              |        <a href='d.html'>d</a>
              |      </li>
              |    </ul>
              |  </body>
              |</html>
            """.trimMargin(), ModuleAppendixSummaryRenderer().render("summary", modules))

    }
}