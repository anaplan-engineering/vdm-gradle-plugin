# vdm-gradle-plugin

## Contents

- [Introduction](#introduction)
- [Version](#version)
- [Using the plugin](#using-the-plugin)
    - [Publishing artifacts](#publishing-artifacts)
    - [Dependency management](#depdendency-management)
- [Tasks](#tasks)
    - [Task summary](#task-summary)
    - [clean](#clean)
    - [dependencyUnpack](#dependencyunpack)
    - [typeCheck](#typecheck)
    - [typeCheckTests](#typechecktests)
    - [package](#package)
    - [docGen](#docgen)
    - [docPackage](#docpackage)
    - [assemble](#assemble)
    - [test](#test)
    - [check](#check)
    - [build](#build)
- [Configuration](#configuration)
- [Examples](#examples)

## Introduction

When using VDM within a development environment that employs continuous delivery we need mechanisms that allows us to
type-check, test and publish specifications in an automated manner. Tools like Maven and Gradle perform this function
for programming languages such as Java and most CI/CD systems can handle projects built with these tools with minimal
effort. This project introduces a Gradle plugin that enables the building and publishing of VDM specifications.

This plugin provides headless mechanisms to run the processes available in the Overture
IDE (https://github.com/overturetool/overture) as well as adapting traditional dependency management, packaging and
publishing provisions for VDM-SL.

Additionally, the plugin provides the capability to combine specifications with markdown documents to produce an
integrated HTML rendering of the specification (see [the docGen task](#docgen))

More details regarding the motivations for the plugin are provided
in [this paper](doc/fraser-integratedVdmSlIntoCdPipelines-final.pdf).

## Version

This page was last updated, to correctly describe the use and behaviour of version **3.0.2** of the plug-in.

Version numbers are currently tied to the version of Overture that is used by the plugin. However, it is possible for a
consumer to choose to use a different version of Overture. This is done, by excluding the version of Overture included
by the plugin and then adding an explicit dependency on the required version. For example, to use 3.0.0 of Overture with
version 3.0.2 of the plugin, you would include the plugin as follows:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(group: 'com.anaplan.engineering', name: 'vdm-gradle-plugin', version: '3.0.2') {
            exclude group: 'org.overturetool.core'
        }
        classpath group: 'org.overturetool.core', name: 'interpreter', version: '3.0.0'
        classpath group: 'org.overturetool.core.annotations', name: 'provided', version: '3.0.0'
    }
}
```

Note that, this method will only work where there are no changes to the signatures of the VDMJ interface used by the
plugin between Overture versions.

### Gradle Version

Support for Gradle 4.x was deprecated with the release of 2.7.0 of this plugin, users should upgrade to Gradle 5.0+.

## Using the plugin

In order to use the plugin, the following must be added to a `build.gradle` file in the root of the specification
project:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath group: 'com.anaplan.engineering', name: 'vdm-gradle-plugin', version: '3.0.2'
    }
}

apply plugin: 'com.anaplan.engineering.vdm'
```

The plugin will automatically pick up any annotation classes that have been added to the classpath dependencies.

The tasks of the plugin are then invoked as they would be for any other Gradle build, for example:

```bash
gradle clean build
```

The tasks that the plugin makes available are described in [tasks](#tasks) below.

The behaviour of the plugin can be customized by adding additional configuration to the `build.gradle` file. The options
available are described in [configuration](#configuration) below.

### Publishing artifacts

It is trivial to instruct Gradle to publish the specification to a Maven-formatted artifact repository using the
maven-publish plugin. The vdm-gradle-plugin will automatically mark artifacts—according to
the [configuration](#configuration)—for publishing when the maven-publish plugin has been applied to the project.

For example, given the following build.gradle file.

```groovy
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath group: 'com.anaplan.engineering', name: 'vdm-gradle-plugin', version: '3.0.2'
    }
}

apply plugin: 'com.anaplan.engineering.vdm'
apply plugin: 'maven-publish'

group = 'com.acme'
version = '0.0.1'

repositories {
    mavenLocal()
    mavenCentral()
}
```

If `gradle publishToMavenLocal` is invoked, then all of the outputs of the [assemble](#assemble) task will be published
to the local Maven repository.

Note that, the maven-publish plugin requires the correct specification of group and version in the `build.gradle` file.

### Dependency management

Once specifications have been published to an artifact repository other components can use them without adding them
directly to the project. This facilitates componentization and encourages reuse.

Specifications are retrieved using the Gradle dependency mechanism via GAV co-ordinates from listed repositories. This
plug-in will be able to consume other projects that have been built with this plug-in, but should also work with any
other zip of specification files.

Dependencies are declared using a `vdm` configuration and by specifying the GAV co-ordinates in the build.gradle file.
For example:

```groovy
dependencies {
    vdm group: 'com.anaplan.car-specification', name: 'wheel', version: '1.0.0'
}
```

The specifications contained in these dependencies will be added to the list of files included in the *typecheck* phase,
and will be present in the project for use in IDEs such as Overture.

By default, the plugin will also look for and download any test or documentation source artifacts associated with each
dependency. This enables test data to be easily shared between projects and for documenation to be aggregated to produce
sites describing the holisitic behaviour of a system. This behaviour can be [configured](#configuration).

Note that, it is not currently possible for the 'main' specification of one project to depend upon the 'test'
specifications of another.

#### External library dependencies

Specifications can use external Java libraries (see Chapter 16 of the Overture IDE User Guide). These can be depended
upon in the same way as other specification projects and these dependencies will be transitive in the same way.

For example, we can add the following to our build.gradle file:

```groovy
dependencies {
    vdm group: 'com.anaplan.engineering', name: 'overture-iso8601-vdm', version: '1.0.0'
}
```

This will add a dependency to the VDM specification in `overture-iso8601-vdm` but transitively to the jar
in `overture-iso8601-lib`. This jar will be added to the classpath when animating the projects tests, and it will also
be added to the project's lib folder so that tests can be run from the Overture IDE.

When creating a library specification, we can just depend upon a published jar library, for example:

```groovy
dependencies {
    vdm group: 'com.anaplan.engineering', name: 'overture-iso8601-lib', version: '1.0.0'
}
```

We would then need to add the VDM module that acted as an interface to the jar in our specification in the usual way.

## Tasks

The Gradle lifecycle is controlled through tasks. Users can execute tasks from the command line and each task will
perform some part of the build. The exact behaviour of a task can often be [configured](#configuration).

Tasks often depend upon other tasks and a task is executed, the graph of dependencies will be determined and all
pre-requisite tasks executed in the appropriate order. The `build` task aggregates all of the principal tasks, and
executing this task gives a quick way to completely assemble and check a project.

[Task summary](#task-summary) gives an overview of the tasks made available by this plugin and their behaviour is
discussed in more detail below.

### Task summary

The following table provides a summary of tasks and their dependencies.

|Task|Depends on|Description|
|----|----------|-----------|
|[*clean*](#clean)| |Removes temporary files generated by the build|
|[dependencyUnpack](#dependencyunpack)| |Extracts dependency specifications to project build directory|
|[typeCheck](#typecheck)|dependencyUnpack|Parses and type checks specs in 'main' folder and all dependencies|
|[typeCheckTests](#typechecktests)|dependencyUnpack|Parses and type checks specs in 'main' and 'test' folders and all dependencies|
|[package](#package)|typeCheckTests|Creates zip file of sources including main specs, test specs and docs)|
|[docGen](#docGen)|typeCheckTests|Processes markdown files and VDM source to generate project documentation|
|[docPackage](#docPackage)|docGen|Packages markdown and associated resource files|
|[*assemble*](#assemble)|**docPackage**, package|Assembles the outputs of the project|
|[test](#test)|typeCheckTests|Runs tests and produces JUnit formatted reports of results (currently supports VDM-SL only)|
|[*check*](#check)|test|Runs all checks|
|[*build*](#build)|assemble, check|Assembles and tests this project|

Note that:

- *Italicized* tasks are part of Gradle's base lifecycle.
- **Bold** dependencies are optional depending upon a project's configuration.

### clean

This task deletes all temporary files that the build generates. Essentially, this involves deleting the *
${project.buildDir}/vdm directory*.

### dependencyUnpack

This task extracts all of the specifications contained in VDM dependencies (
see [Dependency management](#Dependency management)), making them available to subsequent tasks.

Dependencies are extracted to a directory in the project build directory. Each dependency is extracted to its own
folder. The directory for a specific dependency is `${project.buildDir}/vdm/dependencies/${group}/${name}`.

Test and documentation dependencies are also unpacked at this stage. The root folder for test dependencies
is `${project.buildDir}/vdm/test-dependencies`  and for documentation
dependencies `${project.buildDir}/vdm/md-dependencies`.

### typeCheck

This task collects all of the specification files in the main folder (`src/main/vdm` by default) and then parses those
files. If parsing is successful, then the same specifications are type checked. If type checking is successful then a
binary version of the complete specification is produced at `${project.buildDir}/vdm/generated.lib`

### typeCheckTests

This task collects all of the specification files in the main and tests folder (`src/main/vdm` and `src/test/java` by
default) and then parses those files. If parsing is successful, then the same specifications are type checked. If type
checking is successful then a binary version of the complete main and test specification is produced
at `${project.buildDir}/vdm/generated.lib`.

Note: currently main and test specifications are both combined into the generated binary specification (along with the
specifications from dependencies). Ideally these would be separated and the type checking of test specifications will be
isolated from, but dependent on, the main specifications, but the way in which Overture performs binary serialization
does not currently provide us with the means to do this in the way that we would need.

### docGen

*This task will only be performed when using VDM-SL dialect.*

Traditionally formal specifications methodologies have integrated formal and informal languages in LaTeX documents. This
does not work in modern development environments where LaTeX is rarely used.

This plugin enables users to create [markdown](http://commonmark.org/) documents and incorporate elements of their VDM
specifications using the directives in the table below.

|Directive|Example|Description|
|---------|-------------|-----------|
|{@mainModuleList}|{@mainModuleList}|Prints the list of main modules|
|{@testModuleList}|{@testModuleList}|Prints the list of test modules|
|{@link:Module`Definition}|{@link:Seq`drop}|Prints a link to a definition within a module|
|{@ref:Module`Definition}|{@ref:Seq`drop}|Prints a formatted definition from a module|
|(@a:Anchor}|{@a:GettingStarted}|Creates an anchor to which links can be created in the normal way e.g. `[Getting started](#GettingStarted)`|
|{@page:PageName:LinkText}|{@page:EngineSpec:the engine specification}|Create a link to a page from the same project|
|{@page:Group:Module:PageName:LinkText}|{@page:com.anaplan.engineering:set-toolkit:Sets:the set toolkit}|Create a link to a page that is provided by a dependency|

This task collects all of the markdown files in the document source folder (`src/main/md` by default)—and any additional
resources such as images—and uses them to create a collection of HTML files that render both markdown documents and VDM
specifications. These files will be placed in `${project.buildDir}/vdm/docs`.

Additionally a rendering of all main modules will be generated into `${project.buildDir}/vdm/docs/modules` and all test
modules into `${project.buildDir}/vdm/docs/testModules`.

The [vdm-pretty-printer](https://github.com/anaplan-engineering/vdm-pretty-printer) is used to render VDM specifications
using a Unicode/HTML strategy.

### docPackage

*This task will only be performed when using VDM-SL dialect.*

This task collects all of the generated documentation files (from `${project.buildDir}/vdm/docs`) and uses them to
create a zip archive. This archive can then be deployed to a web server to provide a cloud-shareable complete
specification . The task creates the zip archive at *
${project.buildDir}/libs/${project.name}-${project.version}-doc.zip*

### package

This task creates a number of archives depending upon the contents of the project. These archives can be consumed by
other specification projects when they are published and the relevant downstream dependencies added.

The archives created are:

- a collection of the specification files in the main folder (`src/main/vdm` by default) written
  to `${project.buildDir}/libs/${project.name}-${project.version}.zip`
- a collection of the specification files in the test folder (`src/main/vdm` by default) written
  to `${project.buildDir}/libs/${project.name}-${project.version}-test.zip`
- a collection of the markdown files, and other resources, in the main documentation folder (`src/main/md` by default)
  written to `${project.buildDir}/libs/${project.name}-${project.version}-md.zip`

### assemble

This task has no actions of its own, but ensures that other tasks have been executed such that **all** outputs of this
project will be created.

### test

VDMUnit has test framework support for VDM++, but VDM-SL support has not yet been fully integrated into Overture.
Therefore this plug-in is a test harness as well as just a runner. The identification of tests is based on convention
rather than configuration.

This task loads the previously type checked specification binary and identifies all modules whose name begins with '
Test'. Each of these modules is considered a test suite. In each module, the list of operations is examined and those
whose name begins with 'Test' are considered test cases. Each test case is then evaluated. If the evaluation completes
without error the test is considered to have passed. If the evaluation leads to a post condition error then the test is
considered to have failed (we expect the post condition of the test operation to hold the test's assertions about the
result). If the evaluation leads to another type of error (such as precondition or invariant failure then the test is
considered to have errors.

For example, in the following block we have one test suite: `TestArithmetic` and three test cases `TestAdd`
, `TestMultiply` and `TestDivide`. `CheckSubtract` is not a test case as its name does not start with 'Test'. When
evaluated `TestAdd` will be recorded as a pass as the evaluation completes withour error. `TestMultiply` will be
recorded as a failure as we have a post-condition failure. `TestDivide` will be recorded as an error there will (
presumably) be a failing precondition check to prevent division by zero.

```
module TestArithmetic

imports from Arithmetic

definitions

operations

  TestAdd:() ==> real
  TestAdd() == Arithmetic`Add(3, 4)
  post RESULT = 7;

  TestMultiply:() ==> real
  TestMultiply() == Arithmetic`Multiply(3, 4)
  post RESULT = 14;

  TestDivide:() ==> real
  TestDivide() == Arithmetic`Divide(3, 0)
  post RESULT = 0;

  CheckSubtract:() ==> real
  CheckSubtract() == Arithmetic`Subtract(6, 4)
  post RESULT = 2;

end TestArithmetic
```

In order that test results can be displayed in the UIs of common CI tools, the task records the result in JUnit format (
the most common test result, interchange format), producing one file per test suite
in `${project.buildDir}/vdm/junitreports`. As well as a simple result, any failure messages are retained and the
evaluation duration recorded.

In some circumstances, it can be useful to write a test where a precondition, postcondition or invariant check is
expected to fail. Without annotations in VDM there is no particularly clean way to indicate this. Such expected failures
can be signalled to the test runner, by including one of the following strings in the name of the test:

- ExpectPreconditionFailure
- ExpectPostconditionFailure
- ExpectInvariantFailure

For example, in following module, the first test will pass as a precondition will fail, but the second will fail as a
precondition failure is expected, but will not occur.

```
module TestExpectedFailures

definitions

functions

  AddSmallNumbers: nat * nat -> nat
  AddSmallNumbers(x, y) == x + y
  pre x < 10 and y < 10

operations

  TestAddSmallNumbers_Large_ExpectPreconditionFailure:() ==> nat
  TestAddSmallNumbers_Large_ExpectPreconditionFailure() == AddSmallNumbers(13, 4);

  TestAddSmallNumbers_Small_ExpectPreconditionFailure:() ==> nat
  TestAddSmallNumbers_Small_ExpectPreconditionFailure() == AddSmallNumbers(3, 4);

end TestExpectedFailures
```

Test coverage can also be enabled (see [Configuration](#configuration)). When enabled HTML files are created
in `${project.buildDir}/vdm/coverage`. There is one HTML file generated for each source file. Each file contains the
text of the module with green and red highlighting indicating the locations that were hit or missed by the tests
respectively. Tooltips indicate the number of times a specific location was hit. A rudimentary summary of statistics is
also generated in `${project.buildDir}/vdm/coverage/report.html`.

If tests fail, then launch files are generated that enable the test to be easily run from within Overture. These launch
files can be found in `${project.buildDir}/vdm/testLaunch`. It is possible to configure this generation to produce
launches for all tests or none at all.

### check

This task has no actions of its own, but ensures that all verification tasks have been executed.

### build

This task has no actions of its own, but ensures that all assembly and verification tasks have been executed.

## Configuration

The plug-in can be configured by adding a `vdm` block to the build.gradle, for example:

```groovy
vdm {
    dialect 'vdmpp'
    resourceFileTypes = ["svg", "docx"]
    prettyPrinter {
        minListLengthToUseNls = 3
    }
}
```

The following attributes can be configured:

|Attribute|Default value|Description|
|---------|-------------|-----------|
|dialect|"vmdsl"|The VDM dialect to use. One of: *vdmsl, vdmpp, vdmrt*|
|sourcesDir|"src/main/vdm"|The root directory for 'main' vdm specifications|
|testSourcesDir|"src/test/vdm"|The root directory for 'test' vdm specifications|
|docsDir|"src/md/vdm"|The root directory for markdown documentation
|packaging.mdSource|true|If true, automatically produce a package of markdown source files|
|packaging.testSource|true|If true, automatically produce a package of test source files|
|dependencies.autoDependMd|true|If true, automatically download markdown source packages of dependencies|
|dependencies.autoDependTest|true|If true, automatically download test source packages of dependencies|
|prettyPrinter.logUnhandledCases|false|If true, log unhandled node types when pretty printing|
|prettyPrinter.minListLengthToUseNls|5|The min list length at which to print list items on new lines when pretty printing|
|resourceFileTypes|["svg", "png", "gif"]|The resource file types to include in generated documentation and associated packages|
|autoDocGeneration|true|If true, automatically generates documentation before packaging|
|recordCoverage|false|If true, generates HTML files illustrating test coverage of the specification|
|testLaunchGeneration|"FAILING"|Determines for which tests an Overture launch file will be generated. One of "ALL", "FAILING" or "NONE".

## Examples

Example projects and appropriate documentation can be found [here](examples).

The plugin's functional tests execute most scenarios and thus can also provide examples of usage. The projects that are
used in these functional tests are found in [src/test-fn/resources](src/test-fn/resources).
