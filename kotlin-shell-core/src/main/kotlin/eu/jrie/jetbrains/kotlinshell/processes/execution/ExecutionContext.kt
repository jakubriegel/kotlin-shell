package eu.jrie.jetbrains.kotlinshell.processes.execution

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi

interface ExecutionContext {
    val stdin: ProcessReceiveChannel
    val stdout: ProcessSendChannel
    val stderr: ProcessSendChannel
}

interface ProcessExecutionContext : ExecutionContext {
    @ExperimentalCoroutinesApi
    val commander: ProcessCommander
}
