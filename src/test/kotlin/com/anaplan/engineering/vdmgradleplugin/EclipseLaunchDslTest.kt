package com.anaplan.engineering.vdmgradleplugin

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class EclipseLaunchDslTest {

    @Test
    fun standardLaunch() = check("standard-launch") {
        testLaunch {
            stringAttribute {
                key = "vdm_launch_config_default"
                value = "MyModule"
            }
            booleanAttribute {
                key = "vdm_launch_config_dtc_checks"
                value = true
            }
            stringAttribute {
                key = "vdm_launch_config_expression"
                value = "MyModule`MyTest()"
            }
            stringAttribute {
                key = "vdm_launch_config_project"
                value = "my-project"
            }
            stringAttribute {
                key = "vdm_launch_config_method"
                value = "MyTest()"
            }
            stringAttribute {
                key = "vdm_launch_config_module"
                value = "MyModule"
            }
            booleanAttribute {
                key = "vdm_launch_config_inv_checks"
                value = true
            }
            booleanAttribute {
                key = "vdm_launch_config_pre_checks"
                value = true
            }
            booleanAttribute {
                key = "vdm_launch_config_post_checks"
                value = true
            }
            booleanAttribute {
                key = "vdm_launch_config_measure_checks"
                value = true
            }
        }
    }

    private fun check(testName: String, generator: () -> String) {
        val expectedFile = Paths.get(javaClass.getResource("$testName.launch").toURI())
        assertEquals(expectedFile.toFile().readText(), generator())
    }
}
