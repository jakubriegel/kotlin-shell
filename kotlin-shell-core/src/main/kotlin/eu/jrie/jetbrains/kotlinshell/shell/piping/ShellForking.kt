package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.PipingDSLShell
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel

@ExperimentalCoroutinesApi
interface ShellForking : ShellPiping {

    private val newStderr: ProcessChannel
        get() = Channel(env(ShellBase.PIPELINE_CHANNEL_BUFFER_SIZE).toInt())

    private suspend fun forkStdErr(process: ProcessExecutable, fork: PipelineFork) {
        val shell = PipingDSLShell(environment, variables, directory, scope, commander, stdout, stderr)
        forkStdErr(
            process,
            newStderr.also {
                val forked = shell.fork(it).apply {
                    if (!closed) {
                        toDefaultEndChannel(stdout)
                    }
                }
                process.afterJoin = {
                    it.close()
                    forked.join()
                }
            }
        )
    }

    private fun forkStdErr(process: ProcessExecutable, channel: ProcessSendChannel) {
        process.updateStdErr(channel)
    }

    private suspend fun forkStdErr(
        lambda: PipelineContextLambda,
        context: LambdaForkExecutionContext
    ) {
        lambda.invoke(context)
        context.stderr.close()
    }

    /**
     * Builds pipeline for forked stream
     */
    fun pipelineFork(fork: PipelineFork) = fork

    /**
     * Forks stderr of process by creating new [Pipeline] with stderr from last process as an input
     * Part of piping DSL
     *
     * @return this [ProcessBuilder]
     */
    suspend infix fun ProcessExecutable.forkErr(fork: PipelineFork) = this.also {
        forkStdErr(this, fork)
    }

    /**
     * Forks stderr of process by pumping it to given [channel]
     * Part of piping DSL
     *
     * @return this [ProcessBuilder]
     */
    suspend infix fun ProcessExecutable.forkErr(channel: ProcessSendChannel) = this.also {
        forkStdErr(this, channel)
    }

    /**
     * Forks stderr of lambda by creating new [Pipeline] with stderr from last process as an input
     * Part of piping DSL
     *
     * @return this [ProcessBuilder]
     */
    suspend infix fun PipelineContextLambda.forkErr(fork: PipelineFork): PipelineContextLambda = { ctx ->
        newStderr.let { channel ->
            val shell = PipingDSLShell(environment, variables, directory, scope, commander, stdout, stderr)
            shell.fork(channel)
                .apply { if (!closed) toDefaultEndChannel(stdout) }
                .also { forkStdErr(this, LambdaForkExecutionContext(ctx, channel)) }
                .join()
        }
    }

    /**
     * Forks stderr of lambda by pumping it to given [channel]
     * Part of piping DSL
     *
     * @return this [ProcessBuilder]
     */
    suspend infix fun PipelineContextLambda.forkErr(channel: ProcessSendChannel): PipelineContextLambda = {
        forkStdErr(this, LambdaForkExecutionContext(it, channel))
    }

    private class LambdaForkExecutionContext (
        override val stdin: ProcessReceiveChannel,
        override val stdout: ProcessSendChannel,
        override val stderr: ProcessSendChannel
    ) : ExecutionContext {
        constructor(from: ExecutionContext, stderr: ProcessSendChannel)
                : this(from.stdin, from.stdout, stderr)
    }

    private class ProcessForkExecutionContext (
        override val stdin: ProcessReceiveChannel,
        override val stdout: ProcessSendChannel,
        override val stderr: ProcessSendChannel,
        override val commander: ProcessCommander
    ) : ProcessExecutionContext {
        constructor(from: ProcessExecutionContext, stderr: ProcessSendChannel)
                : this(from.stdin, from.stdout, stderr, from.commander)
    }

    private fun ProcessExecutable.updateStdErr(err: ProcessSendChannel) {
        this.context = ProcessForkExecutionContext(this.context as ProcessExecutionContext, err)
    }
}
