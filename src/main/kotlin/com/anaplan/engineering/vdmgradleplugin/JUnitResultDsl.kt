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

@DslMarker
annotation class JUnitResultMarker

@JUnitResultMarker
abstract class JUnitXmlTag(tagName: String) : XmlTag(tagName)

class FailureTag : JUnitXmlTag("failure") {
    @Attribute
    var message: String? = null
}

class ErrorTag : JUnitXmlTag("error") {
    @Attribute
    var message: String? = null
}

class TestCaseTag : JUnitXmlTag("testcase") {
    @Attribute
    var name: String? = null

    @Attribute
    var time: String? = null

    @Attribute
    var classname: String? = null

    @Element
    var failure: FailureTag? = null

    @Element
    var error: ErrorTag? = null

    fun setTime(time: Long) {
        this.time = formatTime(time)
    }

    fun failure(init: FailureTag.() -> Unit) {
        val failure = FailureTag()
        failure.init()
        this.failure = failure
    }

    fun error(init: ErrorTag.() -> Unit) {
        val error = ErrorTag()
        error.init()
        this.error = error
    }
}

class TestSuiteTag : JUnitXmlTag("testsuite") {
    @Attribute
    var name: String? = null

    @Attribute
    var time: String? = null

    @Attribute
    var timestamp: String? = null

    @Attribute
    var hostname: String? = null

    @Attribute
    var tests: Int = 0

    @Attribute
    var errors: Int = 0

    @Attribute
    var failures: Int = 0

    @ElementList
    val testCases = ArrayList<TestCaseTag>()

    fun testCase(init: TestCaseTag.() -> Unit) {
        val testCase = TestCaseTag()
        testCase.init()
        testCases.add(testCase)
    }

    fun setTime(time: Long) {
        this.time = formatTime(time)
    }

}

private fun formatTime(time: Long) = "${time / 1000}.${String.format("%03d", time % 1000)}"

fun testSuite(init: TestSuiteTag.() -> Unit): String {
    val testSuiteTag = TestSuiteTag()
    testSuiteTag.init()
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$testSuiteTag"
}

