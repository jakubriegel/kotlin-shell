package eu.jrie.jetbrains.kotlinshell.processes.process.kts

import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class KtsProcessBuilder (
    val script: String
) : ProcessBuilder() {
    override fun build(): Process {
        TODO("implement KtsProcess")
    }
}