package eu.jrie.jetbrains.kotlinshell.processes.pipeline

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.ShellPiping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.streams.writePacket
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.io.OutputStream

typealias PipelineContextLambda = suspend (context: ExecutionContext) -> Unit

/**
 * The entity representing pipeline.
 * Should be used with piping DSL
 *
 * @see ShellPiping
 */
@ExperimentalCoroutinesApi
class Pipeline @TestOnly internal constructor (
    private val context: ProcessExecutionContext,
    private val channelBufferSize: Int
) {
    /**
     * Indicates wheater this [Pipeline] has ending element.
     * If `true` [Pipeline] cannot be appended.
     *
     * @see Pipeline.toEndLambda
     * @see Pipeline.toEndChannel
     * @see Pipeline.toEndPacket
     * @see Pipeline.toEndStream
     * @see Pipeline.toEndStringBuilder
     */
    var closed = false
        private set

    private val processLine = mutableListOf<Process>()

    private lateinit var lastOut: ProcessReceiveChannel

    private val asyncJobs = mutableListOf<Job>()

    /**
     * Read only list of [Process]es added to this [Pipeline].
     * Most recent [Process] is at the end of the list
     */
    val processes: List<Process>
        get() = processLine.toList()

    /**
     * Adds [process] to this [Pipeline]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun throughProcess(process: ProcessExecutable) = apply {
        addProcess(process.updateContext(newIn = lastOut))
    }

    private suspend fun addProcess(executable: ProcessExecutable) = ifNotEnded {
        executable.updateContext(newOut = channel())
            .apply {
                init()
                exec()
            }

        launch {
            executable.join()
            executable.context.stdout.close()
        }

        processLine.add(executable.process)
    }

    /**
     * Adds [lambda] to this [Pipeline]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun throughLambda(end: Boolean = false, closeOut: Boolean = true, lambda: PipelineContextLambda) = apply {
        addLambda(lambda, context.updated(newIn = lastOut), end, closeOut)
    }

    private suspend fun addLambda(
        lambda: PipelineContextLambda,
        lambdaContext: PipelineExecutionContext = PipelineExecutionContext(context),
        end: Boolean,
        closeOut: Boolean
    ) = ifNotEnded {
        lambdaContext
            .let { if (!end) it.updated(newOut = channel()) else it }
            .let { ctx ->
                launch {
                    lambda(ctx)
                    if (closeOut) ctx.stdout.close()
                }
            }
    }

    private suspend fun toEndLambda(
        closeOut: Boolean = false, lambda: suspend (ByteReadPacket) -> Unit
    ) = toEndLambda(closeOut, lambda, {})

    private suspend fun toEndLambda(
        closeOut: Boolean = false, lambda: suspend (ByteReadPacket) -> Unit, finalize: () -> Unit
    ) = apply {
        throughLambda(end = true, closeOut = closeOut) { ctx ->
            ctx.stdin.consumeEach { lambda(it) }
            finalize()
        }
        closed = true
    }

    /**
     * Ends this [Pipeline] with [channel]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun toEndChannel(channel: ProcessSendChannel) = toEndLambda (
        false,
        { channel.send(it) },
        { channel.close() }
    )

    internal suspend fun toDefaultEndChannel(channel: ProcessSendChannel) = toEndLambda { channel.send(it) }

    /**
     * Ends this [Pipeline] with [packetBuilder]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun toEndPacket(packetBuilder: BytePacketBuilder) = toEndLambda { packetBuilder.writePacket(it) }

    /**
     * Ends this [Pipeline] with [stream]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun toEndStream(stream: OutputStream) = toEndLambda { stream.writePacket(it) }

    /**
     * Ends this [Pipeline] with [stringBuilder]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun toEndStringBuilder(stringBuilder: StringBuilder) = toEndLambda { stringBuilder.append(it.readText()) }

    /**
     * Awaits all processes and jobs in this [Pipeline]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun join() = apply {
        logger.debug("awaiting pipeline $this")
        processLine.forEach { context.commander.awaitProcess(it) }
        asyncJobs.forEach { it.join() }
        logger.debug("awaited pipeline $this")
    }

    /**
     * Kills all processes in this [Pipeline]
     *
     * @see ShellPiping
     * @return this [Pipeline]
     */
    suspend fun kill() = apply {
        logger.debug("killing pipeline $this")
        processLine.forEach { context.commander.killProcess(it) }
        asyncJobs.forEach { it.cancelAndJoin() }
        logger.debug("killed pipeline $this")
    }

    /**
     * Returns new [ProcessSendChannel] and sets it as [lastOut]
     */
    private fun channel(): ProcessSendChannel = Channel<ProcessChannelUnit>(channelBufferSize).also { lastOut = it }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        asyncJobs.add(context.commander.scope.launch(block = block))
    }

    private suspend fun ifNotEnded(block: suspend () -> Unit) {
        if (closed) throw Exception("Pipeline closed")
        else block()
    }

    override fun toString() = "[${this::class.simpleName} ${hashCode()}]"

    companion object {
        /**
         * Starts new [Pipeline] with process specified by given [ProcessExecutable]
         *
         * @see ShellPiping
         */
        internal suspend fun fromProcess(
            process: ProcessExecutable, context: ProcessExecutionContext, channelBufferSize: Int
        ) = Pipeline(context, channelBufferSize)
            .apply { addProcess(process) }

        /**
         * Starts new [Pipeline] with [lambda]
         *
         * @see ShellPiping
         */
        internal suspend fun fromLambda(
            lambda: PipelineContextLambda, context: ProcessExecutionContext, channelBufferSize: Int
        ) = Pipeline(context, channelBufferSize)
            .apply { addLambda(lambda, end = false, closeOut = true) }

        /**
         * Starts new [Pipeline] with [channel]
         *
         * @see ShellPiping
         */
        internal fun fromChannel(
            channel: ProcessReceiveChannel, context: ProcessExecutionContext, channelBufferSize: Int
        ) = Pipeline(context, channelBufferSize)
            .apply { lastOut = channel }

        private val logger = LoggerFactory.getLogger(Pipeline::class.java)
    }

    private class PipelineExecutionContext (
        override val stdin: ProcessReceiveChannel,
        override val stdout: ProcessSendChannel,
        override val stderr: ProcessSendChannel,
        override val commander: ProcessCommander
    ) : ProcessExecutionContext {
        constructor(context: ProcessExecutionContext)
                : this(context.stdin, context.stdout, context.stderr, context.commander)
    }

    private fun ProcessExecutable.updateContext(
        newIn: ProcessReceiveChannel = this.context.stdin,
        newOut: ProcessSendChannel = this.context.stdout
    ) = apply {
        context = PipelineExecutionContext(
            newIn, newOut, context.stderr, (this.context as ProcessExecutionContext).commander
        )
    }

    private fun ProcessExecutionContext.updated(
        newIn: ProcessReceiveChannel = this.stdin,
        newOut: ProcessSendChannel = this.stdout
    ) = PipelineExecutionContext(newIn, newOut, stderr, commander)
}
