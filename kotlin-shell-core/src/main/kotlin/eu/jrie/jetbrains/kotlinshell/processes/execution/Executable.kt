package eu.jrie.jetbrains.kotlinshell.processes.execution

import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class Executable (
    internal var context: ExecutionContext
) {
    internal open fun init() = Unit

    internal abstract suspend fun exec()

    internal open suspend fun join() = Unit

    suspend operator fun invoke() = invoke(context)

    suspend operator fun invoke(context: ExecutionContext) {
        this.context = context
        init()
        exec()
        join()
    }
}

@ExperimentalCoroutinesApi
class ProcessExecutable (
    context: ProcessExecutionContext,
    private val builder: ProcessBuilder
) : Executable(context) {
    lateinit var process: Process
    internal var afterJoin: () -> Unit = {}

    override fun init() = with(context as ProcessExecutionContext) {
        builder
            .withStdin(stdin)
            .withStdout(stdout)
            .withStderr(stderr)
        process = commander.createProcess(builder)
    }

    override suspend fun exec() = with(context as ProcessExecutionContext) {
        commander.startProcess(process)
    }

    override suspend fun join() = with(context as ProcessExecutionContext) {
        commander.awaitProcess(process)
        afterJoin()
    }


}
