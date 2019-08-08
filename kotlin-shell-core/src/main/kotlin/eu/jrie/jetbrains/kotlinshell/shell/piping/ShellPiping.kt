@file:Suppress("EXPERIMENTAL_API_USAGE")

package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ExecutionMode
import eu.jrie.jetbrains.kotlinshell.shell.ShellUtility
import eu.jrie.jetbrains.kotlinshell.shell.piping.from.ShellPipingFrom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel

typealias PipeConfig =  suspend ShellPiping.() -> Pipeline
typealias PipelineFork = suspend (ProcessReceiveChannel) -> Unit

@ExperimentalCoroutinesApi
interface ShellPiping : ShellPipingFrom, ShellPipingThrough, ShellPipingTo, ShellUtility {

    /**
     * List of all pipelines in this shell
     */
    val pipelines: List<Pipeline>

    /**
     * Creates and executes new [Pipeline] specified by DSL [pipeConfig] and executes it in given [mode]
     * Part of piping DSL
     */
    suspend fun pipeline(mode: ExecutionMode = ExecutionMode.ATTACHED, pipeConfig: PipeConfig) = when (mode) {
        ExecutionMode.ATTACHED -> pipeConfig().apply { if (!closed) { toDefaultEndChannel(stdout) } } .join()
        ExecutionMode.DETACHED -> detach(pipeConfig)
        ExecutionMode.DAEMON -> TODO("implement daemon pipelines")
    }

    /**
     * Creates new [Pipeline] specified by DSL [pipeConfig] and executes it as detached job.
     * Part of piping DSL
     */
    suspend fun detach(pipeConfig: PipeConfig): Pipeline

    /**
     * Awaits this [Pipeline]
     * Part of piping DSL
     *
     * @see Pipeline.join
     * @return this [Pipeline]
     */
    @Suppress("UNUSED_PARAMETER")
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.join(it: It) = join()

    private suspend fun forkStdErr(process: ProcessExecutable, fork: PipelineFork) {
        forkStdErr(
            process,
            Channel<ProcessChannelUnit>(PIPELINE_CHANNEL_BUFFER_SIZE).also {
                fork(it)
                process.afterJoin = { it.close() }
            }
        )
    }

    private fun forkStdErr(process: ProcessExecutable, channel: ProcessSendChannel) {
        process.updateStdErr(channel)
    }

    /**
     * Forks current [Pipeline] by creating new [Pipeline] with stderr from last process as an input
     * Part of piping DSL
     *
     * @return this [ProcessBuilder]
     */
    suspend infix fun ProcessExecutable.forkErr(fork: PipelineFork) = this.also {
        forkStdErr(this, fork)
    }

    /**
     * Forks current [Pipeline] by creating new [Pipeline] with stderr from last process as an input
     * Part of piping DSL
     *
     * @return this [ProcessBuilder]
     */
    suspend infix fun ProcessExecutable.forkErr(channel: ProcessSendChannel) = this.also {
        forkStdErr(this, channel)
    }

    private class ForkErrorExecutionContext (
        override val stdin: ProcessReceiveChannel,
        override val stdout: ProcessSendChannel,
        override val stderr: ProcessSendChannel,
        override val commander: ProcessCommander
    ) : ProcessExecutionContext {
        constructor(from: ProcessExecutionContext, stderr: ProcessSendChannel)
                : this(from.stdin, from.stdout, stderr, from.commander)
    }

    private fun ProcessExecutable.updateStdErr(err: ProcessSendChannel) {
        this.context = ForkErrorExecutionContext(this.context as ProcessExecutionContext, err)
    }
}

/**
 * Object for [it] alias
 */
object It
/**
 * Alias to be used in piping DSL with [Pipeline.join]
 *
 * Ex: `p1 pipe p2 await all`
 *
 * @see ShellPiping
 * @see Pipeline
 */
typealias it = It
