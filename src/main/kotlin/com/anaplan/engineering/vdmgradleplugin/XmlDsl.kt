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

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

interface ElementTag {
    fun render(builder: StringBuilder, indent: String)
}

@Target(AnnotationTarget.PROPERTY)
annotation class Attribute(
        val name: String = ""
)

@Target(AnnotationTarget.PROPERTY)
annotation class Element

@Target(AnnotationTarget.PROPERTY)
annotation class ElementList

abstract class XmlTag(private val tagName: String) : ElementTag {

    override fun render(builder: StringBuilder, indent: String) {
        val elements = javaClass.kotlin.memberProperties.filter { property ->
            property.findAnnotation<Element>() != null
        }
        val elementLists = javaClass.kotlin.memberProperties.filter { property ->
            property.findAnnotation<ElementList>() != null
        }
        @Suppress("UNCHECKED_CAST") val childTags = elementLists.map { property -> property.get(this) as List<ElementTag> }.flatten() +
                elements.map { property -> property.get(this) }.filterNotNull().map { it as ElementTag }
        if (childTags.isEmpty()) {
            builder.append("$indent<$tagName${renderAttributes()}/>\n")
        } else {
            builder.append("$indent<$tagName${renderAttributes()}>\n")
            childTags.forEach { child ->
                child.render(builder, "$indent  ")
            }
            builder.append("$indent</$tagName>\n")
        }
    }

    private fun renderAttributes(): String {
        val attributeProperties = javaClass.kotlin.memberProperties.filter { property ->
            property.findAnnotation<Attribute>() != null
        }
        val builder = StringBuilder()
        attributeProperties.forEach { property ->
            val annotation = property.findAnnotation<Attribute>() as Attribute
            val attributeName = if (annotation.name.isEmpty()) {
                property.name
            } else {
                annotation.name
            }
            val value = property.get(this)
            // ignore null attributes
            if (value != null) {
                builder.append(" $attributeName=\"$value\"")
            }
        }
        return builder.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}