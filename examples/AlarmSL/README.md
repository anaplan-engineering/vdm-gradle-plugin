# Example - AlarmSL

This is the classic AlarmSL example. This illustrates the basic usage of the plugin.

The specifications are in [src/main/vdm](src/main/vdm) and the markdown document is in [src/main/md](src/main/vdm).

Gradle is controlled by the contents of the [build.gradle](build.gradle) file.

Here are some tasks to try:

## Type check

In the root folder of the example run `gradle typecheck`

You will see our specifications being parsed and type checked:

```
$ gradle typeCheck

> Task :typeCheck
Parsed 1 module in 0.009 secs. No syntax errors
Warning 5008: alarms in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/testalarm.vdmsl) at line 39:3 hidden by alarms in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/alarm.vdmsl) at line 5:25
Warning 5008: exs in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/testalarm.vdmsl) at line 27:3 hidden by exs in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/alarm.vdmsl) at line 12:11
Warning 5008: exs in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/testalarm.vdmsl) at line 27:3 hidden by exs in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/alarm.vdmsl) at line 48:19
Warning 5008: alarms in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/testalarm.vdmsl) at line 39:3 hidden by alarms in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/changeexpert.vdmsl) at line 5:28
Type checked 1 module in 0.031 secs. No type errors and 4 warnings
Saved 1 module to /home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/build/vdm/generated.lib in 0.022 secs.


BUILD SUCCESSFUL in 0s
2 actionable tasks: 2 executed
```

Try making a breaking change to one of the specification files and re-run.

You will see that the build now fails, e.g.:

```
$ gradle typeCheck

> Task :typeCheck
Error 2017: Expecting ':' or '(' after name in function definition in 'DEFAULT' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/AlarmSL/src/main/vdm/alarm.vdmsl) at line 34:3
Parsed 1 module in 0.014 secs. Found 1 syntax error


FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':typeCheck'.
> VDM parse failed

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

* Get more help at https://help.gradle.org

BUILD FAILED in 0s
2 actionable tasks: 2 executed
```

## Document generation

Inspect the markdown file [src/main/md/alarm.md](src/main/md/alarm.md) and note the extra directives that use braces (
making sure to fix any error introduced in the previous section first).

Now run `gradle docGen`

This process will produce HTML documentation in [build/vdm/docs](build/vdm/docs). Open the `alarm.html` file in a
browser and observe how the directives have been processed.

## Assemble

Now run `gradle assemble`

This process will produce archives in [build/libs](build/libs). There will be three zips produced for this example:

- `AlarmSL-1.0.0.zip`—contains the VDM specification files
- `AlarmSL-1.0.0-doc.zip`—contains the generated HTML files
- `AlarmSL-1.0.0-md.zip`—contains the source markdown files

