# Kotlin Shell

## about
Kotlin Shell is a prototype library for performing shell programming in Kotlin and KotlinScript.
It provides shell-like API which takes advantage of Kotlin high level possibilities.

## get and run
### library
[![library](https://api.bintray.com/packages/jakubriegel/kotlin-shell/kotlin-shell-core/images/download.svg) ](https://bintray.com/jakubriegel//kotlin-shell/kotlin-shell-core/_latestVersion)

```kotlin
repositories {
    maven("https://dl.bintray.com/jakubriegel/kotlin-shell")
}

dependencies {
    implementation("eu.jrie.jetbrains:kotlin-shell-core:VERSION")
}
```

Kotlin Shell features slf4j logging. To use it add logging implementation or NOP logger to turn it off: 
```kotlin
implementation("org.slf4j:slf4j-nop:1.7.26")
```
### scripting
[![scripting](https://api.bintray.com/packages/jakubriegel/kotlin-shell/kotlin-shell-kts/images/download.svg) ](https://bintray.com/jakubriegel//kotlin-shell/kotlin-shell-kts/_latestVersion)

Kotlin Shell scripts have `sh.kts` extension.

Some enviroment variables may be set to customize script execution:
* `SH_KTS_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE`
* `SH_KTS_PIPELINE_RW_PACKET_SIZE`
* `SH_KTS_PIPELINE_CHANNEL_BUFFER_SIZE`

If any of these will not be set, default values will be used.

#### `kshell` command

##### command line
To run the script type:
```
kshell script.sh.kts
```
Read more and download the command [here](https://github.com/jakubriegel/kshell).

##### shebang
Kotlin Shell scripts support shebang:
```
#!/usr/bin/env kshell
```

#### `kotlinc` command
A more low level approach is supported with `kotlinc`:

```
kotlinc -cp PATH_TO_SHELL_KTS_ALL_JAR -Dkotlin.script.classpath  -script SCRIPT.sh.kts ARGS
```

example:
```
kotlinc -cp lib/kotlin-shell-kts-all.jar -Dkotlin.script.classpath  -script hello.sh.kts
```

## usage
### writing scripts 
#### in Kotlin code
with new coroutine scope:
```kotlin
shell {
    "echo hello world!"()
}
```

with given scope:
```kotlin
shell (
    scope = myScope
) {
    "echo hello world!"()
}
```
#### in Kotlin Script
##### blocking api
The blocking api features basic shell commands without the need of wrapping it into coroutines calls:
```kotlin
"echo hello world!".process().run()
```
It can be accessed in Kotlin code as well by using `ScriptingShell` class.

##### non blocking api
The `shell()` function gives access to full api of `kotlin-shell`:
```kotlin
shell {
    "echo hello world!"()
}
```

### processes
#### creating and starting
Before starting any process you need to create `ProcessExecutable`. Then you can start it directly or use it in pipeline.

##### system process
To start new system process use dsl:
```kotlin
val echo = systemProcess {
    cmd { "echo" withArg "hello" }
}
echo()
```

or extensions:
```kotlin
val echo = "echo hello".process() 
echo()
```
or simply: 
```kotlin
"echo hello"() 
```

To start process from file contents use `File.process()` extension:
```kotlin
val process = scriptFile.process(agr1, agr2)
process()
```

or simply:
```kotlin
scriptFile(arg1, arg2)
```

##### kts process
_KotlinScript processes are not yet implemented_

#### multiple calls
To run equivalent process multiple times call `ProcessExecutable.copy()` 

### pipelines
Pipelines can operate on processes, lambdas, files, strings, byte packages and streams.

### creating

To create the pipeline line use `pipeline` block:
```kotlin
pipeline { process1 pipe process2 pipe process3 }
```

Pipeline can be started with `Process`, lambda, `File`, `String`, `ByteReadPacket` or `InputStream`.

### using lambdas
Basic lambda structure for piping is `PipelineContextLambda`:
```kotlin
suspend (ExecutionContext) -> Unit
```
It takes context which consists of `stdin`, `stdout` and `stderr` channels. It can receince content immediately after it was emited by producer, as well as its consumer can receive sent content simultaneously. 

The end of input is signalized with closed `stdin`. `PipelineContextLambda` shouldn't close outputs after execution.

#### lambdas suitable for piping
Most lambdas follow the template `(stdin) -> Pair<stdout, stderr>`

name | definition | builder
--- | --- | --- 
`PipelineContextLambda` | suspend (ExecutionContext) -> Unit | contextLambda { /* code */ }
`PipelinePacketLambda` | suspend (ByteReadPacket) -> Pair<ByteReadPacket, ByteReadPacket> | contextLambda { /* code */ }
`PipelineByteArrayLambda` | suspend (ByteArray) -> Pair<ByteArray, ByteArray> | contextLambda { /* code */ } 
`PipelineStringLambda` | suspend (String) -> Pair<String, String> | contextLambda { /* code */ }
`PipelineStreamLambda` | suspend (InputStream, OutputStream, OutputStream) -> Unit | streamLambda { /* code */ }

##### example
```kotlin
shell {
    val upper = stringLambda { line ->
        line.toUpperCase() to ""
    }
    pipeline { "cat file".process() pipe upper pipe file("result") }
}
```

### detaching
#### process
To detach process use `detach()` function:
```kotlin
val echo = "echo hello world!".process()
detach(echo)
``` 

To join process use `Process.join()` method:
```kotlin
process.join()
```

You can perform these operations also on multiple processes:
```kotlin
detach(p1, p2, p2)
```
```kotlin
await(p1, p2, p3)
```

To join all processes use `joinAll()`.

To access detached processes use `detachedProcesses` member. It stores list of pair of detached job id to process

#### pipeline
To detach pipeline use `detach()` builder:
```kotlin
detach { p1 pipe p2 pipe p3 }
```

or `pipeline()` with correct mode:
```kotlin
pipeline (ExecutionMode.DETACHED) { p1 pipe p2 pipe p3 }
```

To join pipeline call `Pipeline.join()`:
```kotlin
val pipeline = detach { p1 pipe p2 pipe p3 }
pipeline.join()
```

To access detached processes use `detachedPipelines` member. It stores list of pair of detached job id to pipeline

#### attaching
To attach detached job (process or pipeline) use `fg()`:
* `fg(Int)` accepting detached job id. By default it will use `1` as id.
* `fg(Process)`  accepting detached process
* `fg(Pipeline)`  accepting detached pipeline

To join all detached jobs call `joinDetached()`

### shell commands
Kotlin Shell implements some of the most popular shell commmands.

To call the command use `invoke()`:
```kotlin
cmd()
```
then its output will be processed to `stdout`.

To pipe the command put simply put it in the pipeline:
```kotlin
pipeline { cmd pipe process }
```

#### currenlty implemented commands
* `&` as `detach`
* `bg`
* `cd` with `cd(up)` for `cd ..` and `cd(pre)` for `cd -`
* `env`
* `export`
* `fg`
* `jobs`
* `mkdir`
* `print` and `echo` as `print()`
* `ps`
* `set`
* `unset`
* setting shell variable as `variable`

#### special properties
`Shell` class memers provide more Kotlin-like access to popular parameters:
* `detachedPipelines`
* `detachedProcesses`
* `directory`
* `environment`
* `nullin`
* `nullout`
* `processes`
* `systemEnv`
* `variables`

### sub shells
To create sub shell use `shell` block:
```koltin
shell {
    /* code */
    shell {
        /* code */
    }
}
```

By default sub shell will inherit environment, directory, IO streams and constants. You can explicitly specify shell variables and directory to use:
```kotlin
shell {
    shell (
        vars = mapOfVariables,
        dir = directoryAsFile
    ) {
        /* code */
    }
}
```

Sub shells suspend execution of parent shell.

### dependencies
Kotlin Shell scripts support adding dependencies

Kotlin Shell uses dependencies mechanism from `kotlin-main-kts`.

#### external
External dependencies from maven repositories can be added via `@file:Repository` `@file:DependsOn` annotation: 
```kotlin
@file:Repository("MAVEN_REPOSITORY_URL")
@file:DependsOn("GROUP:PACKAGE:VERSION")
```
then they can be imported with standatd `import` statement.

#### internal
To import something from local file use `@file:Import`:
```kotlin
@file:Import("SCRIPT.sh.kts")
```
then they can be imported with standatd `import` statement.


## examples
Examples on writing Kotlin shell scripts can be found in [examples repository](https://github.com/jakubriegel/kotlin-shell-examples).

A good source of detailed examples are also integration tests in this repository.
