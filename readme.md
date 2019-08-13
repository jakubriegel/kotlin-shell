# Kotlin Shell

## about
tba

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
[![library](https://api.bintray.com/packages/jakubriegel/kotlin-shell/kotlin-shell-kts/images/download.svg) ](https://bintray.com/jakubriegel//kotlin-shell/kotlin-shell-kts/_latestVersion)

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
To read more and download the command go [here](https://github.com/jakubriegel/kshell).

##### shebang
Kotlin Shell scripts support shebang:
```
#!/usr/bin/env kshell
```

#### `kotlinc` command
A more low level approach is supported with `kotlinc`:
```
tba
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
tba

#### multiple calls
To run equivalent process multiple times call `ProcessExecutable.copy()` 

### pipelines
tba

### creating
```kotlin
pipeline { process1 pipe process2 pipe process3 }
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
tba

### sub shells
tba

## examples
Examples on writing Kotlin shell scripts can be found in [examples repository](https://github.com/jakubriegel/kotlin-shell-examples).
A good source of detailed examples are also integration tests in this repository.
