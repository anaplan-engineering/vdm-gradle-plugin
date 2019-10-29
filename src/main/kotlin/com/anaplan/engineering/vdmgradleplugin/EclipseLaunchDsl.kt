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
annotation class EclipseLaunchResultMarker

@EclipseLaunchResultMarker
abstract class EclipseLaunchXmlTag(tagName: String) : XmlTag(tagName)

class LaunchConfigurationTag: EclipseLaunchXmlTag("launchConfiguration") {
    @Attribute
    val type = "org.overture.ide.vdmsl.debug.launchConfigurationType"

    @ElementList
    val typedAttributes = ArrayList<TypedAttributeTag>()

    fun stringAttribute(init: StringAttributeTag.() -> Unit) {
        val attributeTag = StringAttributeTag()
        attributeTag.init()
        typedAttributes.add(attributeTag)
    }

    fun booleanAttribute(init: BooleanAttributeTag.() -> Unit) {
        val attributeTag = BooleanAttributeTag()
        attributeTag.init()
        typedAttributes.add(attributeTag)
    }
}

abstract class TypedAttributeTag(tagName: String): EclipseLaunchXmlTag(tagName) {
    @Attribute
    var key: String? = null

}

class StringAttributeTag : TypedAttributeTag("stringAttribute") {
    @Attribute
    var value: String? = null
}

class BooleanAttributeTag : TypedAttributeTag("booleanAttribute") {
    @Attribute
    var value: Boolean? = null
}

fun testLaunch(init: LaunchConfigurationTag.() -> Unit): String {
    val launchConfigurationTag = LaunchConfigurationTag()
    launchConfigurationTag.init()
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$launchConfigurationTag"
}

