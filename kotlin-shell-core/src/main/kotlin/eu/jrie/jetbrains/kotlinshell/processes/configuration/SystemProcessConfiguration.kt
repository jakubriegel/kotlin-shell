package eu.jrie.jetbrains.kotlinshell.processes.configuration

import eu.jrie.jetbrains.kotlinshell.processes.process.system.SystemProcessBuilder

class SystemProcessConfiguration : ProcessConfiguration() {

    override fun createBuilder() = SystemProcessBuilder(cmd, args, systemProcessInputStreamBufferSize)

    var cmd: String = ""

    var args: List<String> = emptyList()
        private set

    fun cmd(config: ProcessConfiguration.() -> Unit) = config()

    infix fun String.withArgs(arguments: List<String>) {
        cmd = this
        args = arguments
    }

    infix fun String.withArg(argument: String) {
        cmd = this
        args = listOf(argument)
    }

    var systemProcessInputStreamBufferSize: Int = -1

}
