package eu.jrie.jetbrains.kotlinshell.processes.process.system

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi

class SystemProcessBuilder (
    val command: String,
    val arguments: List<String> = emptyList(),
    val systemProcessInputStreamBufferSize: Int
) : ProcessBuilder() {

    @ExperimentalCoroutinesApi
    override fun build() = SystemProcess(
        vPID,
        command,
        arguments,
        environment,
        directory,
        stdin,
        stdout,
        stderr,
        scope,
        systemProcessInputStreamBufferSize
    )
}
