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

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class JUnitResultDslTest {

    @Test
    fun emptySuite() = check("empty-suite") {
        testSuite { }
    }

    @Test
    fun emptySuite_withAttributes() = check("empty-suite-with-attributes") {
        testSuite {
            setTime(35)
            name = "basic-attributes"
            timestamp = "timestamp"
            hostname = "hostname"
        }
    }

    @Test
    fun emptySuccessfulTests() = check("empty-successful-tests") {
        testSuite {
            tests = 3
            testCase { }
            testCase { }
            testCase { }
        }
    }

    @Test
    fun successfulTests() = check("successful-tests") {
        testSuite {
            tests = 3
            testCase {
                setTime(12)
                name = "t1"
                classname = "c1"
            }
            testCase {
                setTime(24)
                name = "t2"
                classname = "c2"
            }
            testCase {
                setTime(103435)
                name = "t3"
                classname = "c3"
            }
        }
    }

    @Test
    fun emptyFailedTests() = check("empty-failed-tests") {
        testSuite {
            tests = 3
            failures = 3
            testCase { }
            testCase { }
            testCase { }
        }
    }

    @Test
    fun failedTests() = check("failed-tests") {
        testSuite {
            tests = 3
            failures = 3
            testCase {
                setTime(12)
                name = "t1"
                classname = "c1"
                failure {
                    message = "m1"
                }
            }
            testCase {
                setTime(24)
                name = "t2"
                classname = "c2"
                failure {
                    message = "m2"
                }
            }
            testCase {
                setTime(103435)
                name = "t3"
                classname = "c3"
                failure {
                    message = "m3"
                }
            }
        }
    }

    @Test
    fun emptyErroredTests() = check("empty-errored-tests") {
        testSuite {
            tests = 3
            errors = 3
            testCase { }
            testCase { }
            testCase { }
        }
    }

    @Test
    fun erroredTests() = check("errored-tests") {
        testSuite {
            tests = 3
            errors = 3
            testCase {
                setTime(12)
                name = "t1"
                classname = "c1"
                error {
                    message = "m1"
                }
            }
            testCase {
                setTime(24)
                name = "t2"
                classname = "c2"
                error {
                    message = "m2"
                }
            }
            testCase {
                setTime(103435)
                name = "t3"
                classname = "c3"
                error {
                    message = "m3"
                }
            }
        }
    }

    @Test
    fun mixedTests() = check("mixed-tests") {
        testSuite {
            tests = 3
            errors = 1
            failures = 1
            testCase {
                setTime(12)
                name = "t1"
                classname = "c1"
            }
            testCase {
                setTime(24)
                name = "t2"
                classname = "c2"
                error {
                    message = "m2"
                }
            }
            testCase {
                setTime(103435)
                name = "t3"
                classname = "c3"
                failure {
                    message = "m3"
                }
            }
        }
    }

    private fun check(testName: String, generator: () -> String) {
        val expectedFile = Paths.get(javaClass.getResource("junit-$testName-expected.xml").toURI())
        assertEquals(expectedFile.toFile().readText(), generator())
    }
}
