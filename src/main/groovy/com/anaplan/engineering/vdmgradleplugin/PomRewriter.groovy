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

import groovy.xml.XmlUtil

class PomRewriter {

    private static XmlSlurper slurper = new XmlSlurper(false, false)

    private File pomFile

    PomRewriter(File pomFile) {
        this.pomFile = pomFile
    }

    def addDependencies(Collection<Dependency> dependenciesToAdd) {
        def project = slurper.parse(pomFile)
        if (project.dependencies == null || project.dependencies.isEmpty()) {
            project.appendNode {
                dependencies {
                    dependenciesToAdd.each { d ->
                        dependency {
                            groupId d.groupId
                            artifactId d.artifactId
                            version d.version
                            classifier d.classifier
                            type d.type
                            scope d.scope
                        }
                    }
                }
            }
        } else {
            project.dependencies.appendNode {
                dependenciesToAdd.each { d ->
                    dependency {
                        groupId d.groupId
                        artifactId d.artifactId
                        version d.version
                        classifier d.classifier
                        type d.type
                        scope d.scope
                    }
                }
            }
        }
        pomFile.write(XmlUtil.serialize(project))
    }
}

class Dependency {
    String groupId
    String artifactId
    String version
    String classifier
    String type
    String scope
}
