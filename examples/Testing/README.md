# Example - Testing

This example illustrates the built in test harness.

There is one main module in `src/main/vdm` and one test module that tests it in `src/test/vdm`.

We can execute the tests:

```
$ gradle test

> Task :typeCheckTests
Parsed 2 modules in 0.001 secs. No syntax errors
Warning 5000: Definition 'TestAdd' not used in 'TestAdder' (/home/si/git/fspec-tools/vdm-gradle-plugin/examples/Testing/src/test/vdm/TestAdder.vdmsl) at line 7:3
Type checked 2 modules in 0.003 secs. No type errors and 1 warning
Saved 2 modules to /home/si/git/fspec-tools/vdm-gradle-plugin/examples/Testing/build/vdm/generated.lib in 0.03 secs. 

> Task :test
Loaded 2 modules from /home/si/git/fspec-tools/vdm-gradle-plugin/examples/Testing/build/vdm/generated.lib in 0.013 secs


BUILD SUCCESSFUL in 1s
3 actionable tasks: 3 executed
``` 

This does not give a great deal of information, so we can either add more logging:

```
$gradle test --info
...
> Task :test
Putting task artifact state for task ':test' into context took 0.0 secs.
Executing task ':test' (up-to-date check took 0.0 secs) due to:
  Task has not declared any outputs.
Loaded 2 modules from /home/si/git/fspec-tools/vdm-gradle-plugin/examples/Testing/build/vdm/generated.lib in 0.005 secs
SUCCESS -- 1 tests passed

:test (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.011 secs.

BUILD SUCCESSFUL in 0s
3 actionable tasks: 3 executed
```

Or inspect the generated JUnit reports. These can be found in `build/vdm/junitreports`.

Now try invalidating the post condition of ``TestAdder`TestAdd`` and re-running. The build will now fail as there are failing tests.