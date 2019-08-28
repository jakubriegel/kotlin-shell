# Kotlin Shell

## about
Kotlin Shell is a prototype tool for performing shell programming in Kotlin and KotlinScript.
It provides shell-like API which takes advantage of Kotlin high level possibilities.

For examples go to the [examples](#examples) section.

## content
* [about](#about)
* [content](#content)
* [get and run](#get-and-run)
  + [library](#library)
  + [scripting](#scripting)
    - [kshell command](#kshell-command)
      * [command line](#command-line)
      * [shebang](#shebang)
    - [kotlinc command](#kotlinc-command)
* [usage](#usage)
  + [writing scripts](#writing-scripts)
    - [in Kotlin code](#in-kotlin-code)
    - [in Kotlin Script](#in-kotlin-script)
      * [blocking api](#blocking-api)
      * [non blocking api](#non-blocking-api)
  + [processes](#processes)
    - [creating and starting processes](#creating-and-starting-processes)
      * [system process](#system-process)
      * [kts process](#kts-process)
    - [multiple calls to processes](#multiple-calls-to-processes)
  + [pipelines](#pipelines)
    - [piping logic](#piping-logic)
      * [introduction](#introduction)
      * [piping grammar](#piping-grammar)
    - [creating pipeline](#creating-pipeline)
    - [forking stderr](#forking-stderr)
    - [lambdas in pipelines](#lambdas-in-pipelines)
    - [lambdas suitable for piping](#lambdas-suitable-for-piping)
      * [example](#example)
  + [detaching](#detaching)
    - [detaching process](#detaching-process)
    - [detaching pipeline](#detaching-pipeline)
    - [attaching](#attaching)
  + [demonizing](#demonizing)
  + [environment](#environment)
    - [system environment](#system-environment)
    - [shell environment](#shell-environment)
    - [shell variables](#shell-variables)
    - [special variables](#special-variables)
  + [shell commands](#shell-commands)
    - [implemented shell commands](#implemented-shell-commands)
    - [shell methods](#shell-methods)
    - [custom shell commands](#custom-shell-commands)
    - [custom shell methods](#custom-shell-methods)
    - [special properties](#special-properties)
  + [sub shells](#sub-shells)
  + [dependencies](#dependencies)
    - [external dependencies](#external-dependencies)
    - [internal dependencies](#internal-dependencies)
* [examples](#examples)
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

Some environment variables may be set to customize script execution. Go to the [environment](#environment) section to learn more.

#### kshell command
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

#### kotlinc command
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
#### creating and starting processes
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
_creating virtual KotlinScript processes is not implemented yet_

#### multiple calls to processes
To run equivalent process multiple times call `ProcessExecutable.copy()` 

### pipelines
Pipelines can operate on processes, lambdas, files, strings, byte packages and streams. 

#### piping logic
##### introduction
Every executable element in Kotlin Shell receives its own `ExecutionContext`, which consist of `stdin`, `stdout` and `stderr` implemented as `Channels`. In the library channels are used under aliases `ProcessChannel`, `ProcessSendChannel` and `ProcessReceiveChannel` their unit is always `ByteReadPacket`. `Shell` itself is an `ExecutionContext` and provides default channels:
* `stdin` is always empty and closed `ProcessReceiveChannel`, which effectively acts like `/dev/null`. It  It can be accessed elsewhere by `nullin` member.
* `stdout` is a rendezvous `ProcessSendChannel`, that passes everything to `System.out`.
* `stderr` is a reference to `stdout`.

Beside them there is also special member `ProcessSendChannel` called `nullout`, which acts like `/dev/null`. 

Pipeline elements are connected by `ProcessChannel`s, that override their context's default IO. Only the neccessary streams are overriden, so not piped ones are redirected to the channels, that came with the context. Each element in the pipeline ends its execution after processing the last received packet before receiving close signal from `stdin` channel. 

Pipelines are logically divided into three parts: `FROM`, `THROUGH` and `TO`. The api is designed to look seamless, but in order to take full advantage of piping it is necessary to distinguish these parts. Every element can emit some output, but doesn't have to. They also shouldn't close they outputs after execution. It is done automatically by piping engine and ensures that channels used by other entities (such as `stdout`) won't be closed.

Every pipeline starts with single element `FROM` section. It can be `Process`, [lambda](#lambdas-in-pipelines), `File`, `String`, `InputStream`, `ByteReadPacket` or `Channel`. Elements used here receive no input (for processes and lambdas there is `nullin` provided). Then the `THROUGH` or `TO` part occurs. 
Piping `THROUGH` can be performed on `Process` or [lambda](#lambdas-in-pipelines) and can consist of any number of elements. They receive the input simutanously while the producer is going (due to the limitations of `zt-exec` library `SystemProcess` may wait till the end of input) and can emit output as they go. 
Every pipeline is ended with single element `TO` section. Elements here take input, but do not emit any output. If no `TO` element is provided, the `pipeline` builder will implicitly end the pipeline with `stdout`.

##### piping grammar
Schematic grammar for piping could look like this:
```
PIPELINE -> FROM THROUGH TO
PIPELINE -> FROM TO
FROM -> PROCESS | LAMBDA | FILE | STRING | INPUT_STREAM | BYTE_READ_PACKET | PROCESS_SEND_CHANNEL
THROUGH -> PROCESS | LAMBDA
TO -> PROCESS | LAMBDA | FILE | STRING_BUILDER | OUTPUT_STREAM | BYTE_PACKET_BUILDER | PROCESS_RECEIVE_CHANNEL
```

#### creating pipeline
To construct and execute the pipeline use `pipeline` builder:
```kotlin
pipeline { a pipe b pipe c }
```

Pipeline can be started with `Process`, lambda, `File`, `String`, `ByteReadPacket` or `InputStream`. Once the pipeline is created it cannot be modified.

The `pipeline` builder takes an optional parameter `mode` of type `ExecutionMode`. It can be used for [detaching](#detaching) or [demonizing](#demonizing) the pipeline. By default it uses `ExecutionMode.ATTACHED`
```kotlin
pipeline (ExecutionMode.ATTACHED) { a pipe b pipe c }
pipeline (ExecutionMode.DETACHED) { a pipe b pipe c }
pipeline (ExecutionMode.DAEMON) { a pipe b pipe c }
```

Constructed pipeline can be stored in an object of `Pipeline` type:
```kotlin
val p = pipeline { a pipe b }
```

You can perform several operiations on it:
* `Pipeline.join()` joins the pipeline
* `Pipeline.kill()` kill all elements of the pipeline

And access `processes` member, which is a list of all processes in the pipeline.

#### forking stderr
To fork `stderr` from process or lambda use `forkErr`:
```kotlin
pipeline { a pipe b forkErr { /* fork logic */ } pipe c }
```

it redirects elements error stream to given pipeline. 

The builder function receives the new error `ProcessReceiveChannel` as an implicit argument. 
The function should return new `Pipeline`. If this pipeline wont be ended with `TO`, it will implicitly be appended with `stdout`.

The fork logic can be stored in a variable:
```kotlin
val fork = pipelineFork { it pipe filter pipe file }
pipeline { a forkErr fork }
```

The fork belongs to process executable or lambda itself so it can be used outside pipeline as well:
```kotlin
val process = "cmd arg".process() forkErr { /* fork */ }
process()
```

```kotlin
val lambda = stringLambda { /* lambda */ } forkErr { /* fork */ }
pipeline { lambda pipe { /* ... */} }
```

As a shorthand it is possible to fork error directly to given channel:
```kotlin
val channel: ProcessChannel = Channel()
val b = a forkErr channel
```


#### lambdas in pipelines
Basic lambda structure for piping is `PipelineContextLambda`:
```kotlin
suspend (ExecutionContext) -> Unit
```
It takes context which consists of `stdin`, `stdout` and `stderr` channels. It can receive content immediately after it was emitted by the producer, as well as its consumer can receive sent content simultaneously. 

The end of input is signalized with closed `stdin`. `PipelineContextLambda` shouldn't close outputs after execution.

#### lambdas suitable for piping
There are several wrappers for `PipelineContextLambda`, that can make piping easier. Most of them follow the template `(stdin) -> Pair<stdout, stderr>`

name | definition | builder
--- | --- | --- 
`PipelineContextLambda` | suspend (ExecutionContext) -> Unit | contextLambda { }
`PipelinePacketLambda` | suspend (ByteReadPacket) -> Pair<ByteReadPacket, ByteReadPacket> | packetLambda { }
`PipelineByteArrayLambda` | suspend (ByteArray) -> Pair<ByteArray, ByteArray> | byteArrayLambda { } 
`PipelineStringLambda` | suspend (String) -> Pair<String, String> | stringLambda { /* code */ }
`PipelineStreamLambda` | suspend (InputStream, OutputStream, OutputStream) -> Unit | streamLambda { }

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
#### detaching process
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

#### detaching pipeline
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

### demonizing
> At the current stage demonizing processes and pipelines is implemented in very unstable and experimental way. 
> 
> Though it should not be used. 

### environment
Environment in Kotlin Shell is divided into two parts `shell environment` and `shell variables`. The environment from system is also inherited

To access the environment call:
* `environment` list or `env` command for `shell environment`
* `variables` list for `shell variables`
* `shellEnv` or `set` command for combined environment
* `systemEnv` for the environment inherited from system

#### system environment
`system environment` is copied to `shell environment` at its creation. To access system environment any time call `systemEnv`

#### shell environment
`shell environment` is inherited by `Shell` from the system. It can be modified and is copied to sub shells.

To set environment use `export`:
```kotlin
export("KEY" to "VALUE")
```

To make it read-only add `readonly`:
```kotlin
readonly export("KEY" to "VALUE")
```

To print environment variable use `env`:
```kotlin
env("KEY")
```

To remove use `unset`:
```kotlin
unset("key")
```

#### shell variables
`shell variables` are empty by default. They can be modified and __are not__ copied to sub shells   

To set variable use `variable`:
```kotlin
variable("KEY" to "VALUE")
``` 

To make it read-only add `readonly`:
```kotlin
readonly variable("KEY" to "VALUE")
```

To print shell variable use `env`:
```kotlin
env("KEY")
```

To remove variable use `unset`:
```kotlin
unset("key")
```

#### special variables
Kotlin Shell uses some special variables for customisation of execution. 
They can be set explicitly by `shell` builders or can be inherited from system. 
If any of these will not be set, default values will be used.

variable | type | usage | default value
--- | --- | --- | ---
`SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE` | `Int` | size of `SystemProcessInputStream` buffer | 16
`PIPELINE_RW_PACKET_SIZE` | `Long` | maximal size of packets used in piping | 16
`PIPELINE_CHANNEL_BUFFER_SIZE` | `Int` | size of `ProcessChannel`s used in piping  | 16
`REDIRECT_SYSTEM_OUT` | `YES`/`NO` | Specifies weather `System.out` should be bypassed with `Shell.stdout`. As a result it will synchronize stdlib `print()` and `println()` with `shell` outputs | YES



### shell commands
Kotlin Shell implements some of the most popular shell commands with additions of special methods and properties.

To call the command use `invoke()`:
```kotlin
cmd()
```
then its output will be processed to `stdout`.

To pipe the command put simply put it in the pipeline:
```kotlin
pipeline { cmd pipe process }
```

#### implemented shell commands
* `&` as `detach`
* `cd` with `cd(up)` for `cd ..` and `cd(pre)` for `cd -`
* `env`
* `exit` as `return@shell`
* `export`
* `fg`
* `jobs`
* `mkdir`
* `print` and `echo` as `print()`/`println()`
* `ps`
* `readonly`
* `set`
* `unset`
* setting shell variable as `variable`

#### shell methods
`Shell` member functions provide easy ways for performing popular shell tasks:
* `file()` - gets or creates file relative to current directory

#### custom shell commands
To implement custom shell command create extension member of Shell class and override its getter:
```kotlin
val Shell.cmd: ShellCommand
    get() = command {
        /* command implementation returning String */
    }
```

such command can be declared outside `shell` block and be used as [dependency](#internal-dependencies).

#### custom shell methods 
To implement custom shell method use the basic function template:
```kotlin
suspend fun Shell.() -> T
```

where `T` is desired return type or `Unit`. Such functions can be declared outside `shell` block and be used as [dependency](#internal-dependencies).

#### special properties
`Shell` members provide easy Kotlin-like access to popular parameters:
* `detachedPipelines`
* `detachedProcesses`
* `directory`
* `environment`
* `nullin`
* `nullout`
* `processes`
* `shellEnv`
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

By default sub shell will inherit environment, directory, IO streams and constants. 
You can explicitly specify shell variables and directory to use:
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

Sub shells suspend execution of the parent shell.

### dependencies
Kotlin Shell scripts support adding dependencies

Kotlin Shell uses dependencies mechanism from `kotlin-main-kts`.

#### external dependencies
External dependencies from maven repositories can be added via `@file:Repository` `@file:DependsOn` annotation: 
```kotlin
@file:Repository("MAVEN_REPOSITORY_URL")
@file:DependsOn("GROUP:PACKAGE:VERSION")
```
then they can be imported with standard `import` statement.

#### internal dependencies
To import something from local file use `@file:Import`:
```kotlin
@file:Import("SCRIPT.sh.kts")
```
then they can be imported with standatd `import` statement.


## examples
Examples on writing Kotlin shell scripts can be found in the [examples repository](https://github.com/jakubriegel/kotlin-shell-examples).

A good source of detailed examples are also integration tests in this repository.
