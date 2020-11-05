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

import org.overture.interpreter.VDMJ
import org.overture.interpreter.VDMPP
import org.overture.interpreter.VDMRT
import org.overture.interpreter.VDMSL
import org.overture.interpreter.runtime.Interpreter
import org.overture.interpreter.util.ExitStatus
import java.io.File
import java.util.*

enum class Dialect(val fileExtension : String, val createController : () -> GradleVdm) {
    vdmsl("vdmsl", { GradleVdmSl() }),
    vdmpp("vdmpp", { GradleVdmPp() }),
    vdmrt("vdmrt", { GradleVdmRt() }),
}

interface GradleVdm {
    fun setOutfile(outfile : String)

    fun parse(files: List<File>): ExitStatus

    fun typeCheck(): ExitStatus

    fun getInterpreter(): Interpreter
}

open class GradleVdmSl : VDMSL(), GradleVdm {

    override fun setOutfile(outfile : String) {
        VDMJ.outfile = outfile
    }

}

open class GradleVdmPp : VDMPP(), GradleVdm {

    override fun setOutfile(outfile : String) {
        VDMJ.outfile = outfile
    }

}

open class GradleVdmRt : VDMRT(), GradleVdm {

    override fun setOutfile(outfile : String) {
        VDMJ.outfile = outfile
    }

}