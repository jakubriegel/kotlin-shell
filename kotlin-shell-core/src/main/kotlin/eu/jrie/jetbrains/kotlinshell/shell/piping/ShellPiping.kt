@file:Suppress("EXPERIMENTAL_API_USAGE")

package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.execution.ExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelInputStream
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelOutputStream
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.shell.ExecutionMode
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.PIPELINE_CHANNEL_BUFFER_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellUtility
import eu.jrie.jetbrains.kotlinshell.shell.piping.from.ShellPipingFrom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import java.io.InputStream
import java.io.OutputStream

interface AbstractPipingDSLShell : ShellPipingFrom, ShellPipingThrough, ShellPipingTo

typealias PipelineConfig =  suspend AbstractPipingDSLShell.() -> Pipeline
typealias PipelineFork = suspend AbstractPipingDSLShell.(ProcessReceiveChannel) -> Pipeline

typealias PipelinePacketLambda = suspend (ByteReadPacket) -> Pair<ByteReadPacket, ByteReadPacket>
typealias PipelineByteArrayLambda = suspend (ByteArray) -> Pair<ByteArray, ByteArray>
typealias PipelineStringLambda = suspend (String) -> Pair<String, String>
typealias PipelineStreamLambda = suspend (InputStream, OutputStream, OutputStream) -> Unit

@ExperimentalCoroutinesApi
interface ShellPiping : ShellUtility {

    /**
     * List of detached pipelines
     */
    val detachedPipelines: List<Pair<Int, Pipeline>>

    /**
     * Creates and executes new [Pipeline] specified by DSL [pipelineConfig] and executes it in given [mode]
     * Part of piping DSL
     */
    suspend fun pipeline(mode: ExecutionMode = ExecutionMode.ATTACHED, pipelineConfig: PipelineConfig): Pipeline

    /**
     * Creates new [Pipeline] specified by DSL [pipelineConfig] and executes it as detached job.
     * Part of piping DSL
     */
    suspend fun detach(pipelineConfig: PipelineConfig): Pipeline

    /**
     * Attaches selected [Pipeline]
     */
    suspend fun fg(pipeline: Pipeline)

    /**
     * Constructs [PipelineContextLambda] to be used in piping
     * Part of piping DSL
     */
    fun contextLambda(lambda: PipelineContextLambda) = lambda

    /**
     * Constructs [PipelinePacketLambda] to be used in piping
     * Part of piping DSL
     */
    fun packetLambda(
        lambda: PipelinePacketLambda
    ) = contextLambda { ctx ->
        with(ctx.stdin) {
            if(isClosedForReceive) processPacket(lambda(emptyPacket()), ctx)
            else consumeEach { processPacket(lambda(it), ctx) }
        }
    }

    private suspend inline fun processPacket(out: Pair<ByteReadPacket, ByteReadPacket>, ctx: ExecutionContext) {
        ctx.stdout.send(out.first)
        ctx.stderr.send(out.second)
    }

    /**
     * Constructs [ByteReadPacket] from given [bytes]
     * Part of piping DSL
     *
     * @see packetLambda
     */
    fun packet(bytes: ByteArray) = buildPacket { writeFully(bytes) }

    /**
     * Constructs [ByteReadPacket] from given [string]
     * Part of piping DSL
     *
     * @see packetLambda
     * @see stringLambda
     */
    fun packet(string: String) = packet(string.toByteArray())

    /**
     * Constructs empty [ByteReadPacket]
     * Part of piping DSL
     *
     * @see packetLambda
     * @see stringLambda
     */
    fun emptyPacket() = packet(emptyByteArray())

    /**
     * Constructs [PipelineByteArrayLambda] to be used in piping
     * Part of piping DS
     */
    fun byteArrayLambda(
        lambda: PipelineByteArrayLambda
    ) = packetLambda { p ->
        lambda(p.readBytes()).let { packet(it.first) to packet(it.second) }
    }

    fun emptyByteArray() = ByteArray(0)

    /**
     * Constructs [PipelineStringLambda] to be used in piping
     * Part of piping DSL
     */
    fun stringLambda(
        lambda: PipelineStringLambda
    ) = packetLambda { b ->
        lambda(b.readText()).let { packet(it.first) to packet(it.second) }
    }

    /**
     * Constructs [PipelineStreamLambda] to be used in piping
     * Part of piping DSL
     */
    fun streamLambda(lambda: PipelineStreamLambda) = env(PIPELINE_CHANNEL_BUFFER_SIZE).toInt().let { size ->
        contextLambda { ctx ->
            val inStream = ProcessChannelInputStream(ctx.stdin, scope, size)
            val stdStream = ProcessChannelOutputStream(ctx.stdout, scope, size)
            val errStream = ProcessChannelOutputStream(ctx.stderr, scope, size)
            lambda(inStream, stdStream, errStream)
        }
    }


}
