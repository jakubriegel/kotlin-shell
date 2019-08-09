# Kotlin Shell

## about
Tool for performing shell-like programing in Kotlin. Features Kotlin library and `kts` definition for writting scripts

## get it
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

The best way to run `sh.kts` scripts is to use `kshell` command. More about it can be found [here](https://github.com/jakubriegel/kshell)

For more low level usage you can run it directly with `kotlinc`. Sample command might be:
```shell
kotlinc -cp kotlin-shell-core.jar:kotlin-shell-kts.jar:kotlin-main-kts.jar:[other dependencies] -script hello.sh.kts
```

## usage
### writing scripts
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

### processes
starting system process with dsl:
```kotlin
val echo = systemProcess {
    cmd { "echo" withArg "hello" }
}
echo()
```

starting system with extensions:
```kotlin
val echo = "echo hello".process() 
echo()
```
or simply: 
```kotlin
"echo hello"() 
```

### pipelines
```kotlin
process1 pipe process2 pipe process3
```

 
