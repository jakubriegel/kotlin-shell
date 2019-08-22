package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.PIPELINE_RW_PACKET_SIZE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.streams.readPacketAtMost
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@ExperimentalCoroutinesApi
interface ShellPipingFromStream : ShellPipingFromLambda {
    /**
     * Starts new [Pipeline] from [stream].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend fun from(stream: InputStream) = from(stream.readFully())

    /**
     * Starts new [Pipeline] from [stream] and closes the [stream] after using it.
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend fun fromUse(stream: InputStream) = from(stream.useFully())

    private fun InputStream.readFully() = contextLambda {
        val size = env(PIPELINE_RW_PACKET_SIZE).toLong()
        try {
            do {
                val packet = readPacketAtMost(size)
                it.stdout.send(packet)
            } while (packet.remaining == size)
        } catch (e: EOFException) {}
    }

    private fun InputStream.useFully() = contextLambda { ctx ->
        use { readFully().invoke(ctx) }
    }

    /**
     * Starts new [Pipeline] from this [InputStream] to [process].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(process: ProcessExecutable) = from(this) pipe process

    /**
     * Starts new [Pipeline] from this [InputStream] to [lambda].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(lambda: PipelineContextLambda) = from(this) pipe lambda

    /**
     * Starts new [Pipeline] from this [InputStream] to [channel].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(channel: ProcessSendChannel) = from(this) pipe channel

    /**
     * Starts new [Pipeline] from this [InputStream] to [packetBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(packetBuilder: BytePacketBuilder) = from(this) pipe packetBuilder

    /**
     * Starts new [Pipeline] from this [InputStream] to [stream].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(stream: OutputStream) = from(this) pipe stream

    /**
     * Starts new [Pipeline] from this [InputStream] and appends [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipeAppend(file: File) = from(this) pipeAppend  file

    /**
     * Starts new [Pipeline] from this [InputStream] to [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(file: File) = from(this) pipe file

    /**
     * Starts new [Pipeline] from this [InputStream] to [stringBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun InputStream.pipe(stringBuilder: StringBuilder) = from(this) pipe stringBuilder
}
