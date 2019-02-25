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

import com.anaplan.engineering.vdmprettyprinter.PrettyPrintConfig
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

open class VdmConfigExtension @javax.inject.Inject constructor(objectFactory: ObjectFactory) {
    var dialect: Dialect = Dialect.vdmsl
    var sourcesDir: String = "src/main/vdm"
    var testSourcesDir: String = "src/test/vdm"
    var docsDir: String = "src/main/md"
    var packaging: Packaging = objectFactory.newInstance(Packaging::class.java)
    var dependencies: Dependencies = objectFactory.newInstance(Dependencies::class.java)
    var prettyPrinter: PrettyPrinterConfig = objectFactory.newInstance(PrettyPrinterConfig::class.java)
    var resourceFileTypes: Array<String> = arrayOf("svg", "png", "gif")
    var autoDocGeneration: Boolean = true
    var recordCoverage: Boolean = false

    fun packaging(action: Action<Packaging>) {
        action.execute(packaging)
    }

    fun dependencies(action: Action<Dependencies>) {
        action.execute(dependencies)
    }

    fun prettyPrinter(action: Action<PrettyPrinterConfig>) {
        action.execute(prettyPrinter)
    }
}

open class Packaging @javax.inject.Inject constructor(@Suppress("UNUSED_PARAMETER") objectFactory: ObjectFactory) {
    var mdSource: Boolean = true
    var testSource: Boolean = true
}

open class Dependencies @javax.inject.Inject constructor(@Suppress("UNUSED_PARAMETER") objectFactory: ObjectFactory) {
    var autoDependMd: Boolean = true
    var autoDependTest: Boolean = true
}

open class PrettyPrinterConfig @javax.inject.Inject constructor(@Suppress("UNUSED_PARAMETER") objectFactory: ObjectFactory) {
    var logUnhandledCases: Boolean = false
    var minListLengthToUseNls: Int = 5

    fun toConfig() = PrettyPrintConfig(
            logUnhandledCases = logUnhandledCases,
            minListLengthToUseNls = minListLengthToUseNls
    )
}

