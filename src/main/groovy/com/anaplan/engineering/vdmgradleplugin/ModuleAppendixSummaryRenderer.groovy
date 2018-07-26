package com.anaplan.engineering.vdmgradleplugin

import groovy.xml.MarkupBuilder

class ModuleAppendixSummaryRenderer {

    String render(String pageTitle, List modules) {
        def writer = new StringWriter()
        new MarkupBuilder(writer).html("data-theme": "vdm") {
            head {
                meta(charset: "UTF-8")
                link(rel: "stylesheet", type: "text/css", href: "../vdm.css")
                title pageTitle
            }
            body {
                h1 pageTitle
                ul {
                    modules.collect { it.name.name }.sort().each { name ->
                        li {
                            a(href : "${name}.html", name)
                        }
                    }
                }
            }
        }
        writer.toString()
    }

}
