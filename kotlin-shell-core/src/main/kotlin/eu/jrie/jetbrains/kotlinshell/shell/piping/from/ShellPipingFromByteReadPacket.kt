package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.streams.inputStream
import java.io.File
import java.io.OutputStream

@ExperimentalCoroutinesApi
interface ShellPipingFromByteReadPacket : ShellPipingFromStream {
    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [process].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    @InternalCoroutinesApi
    suspend infix fun ByteReadPacket.pipe(process: ProcessExecutable) = fromUse(this.inputStream()) pipe process

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [lambda].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipe(lambda: PipelineContextLambda) = fromUse(this.inputStream()) pipe lambda

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [channel].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipe(channel: ProcessSendChannel) = fromUse(this.inputStream()) pipe channel

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [packetBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipe(packetBuilder: BytePacketBuilder) = fromUse(this.inputStream()) pipe packetBuilder

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [stream].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipe(stream: OutputStream) = fromUse(this.inputStream()) pipe stream

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipe(file: File) = fromUse(this.inputStream()) pipe file

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] and appends [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipeAppend(file: File) = fromUse(this.inputStream()) pipeAppend  file

    /**
     * Starts new [Pipeline] from this [ByteReadPacket] to [stringBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun ByteReadPacket.pipe(stringBuilder: StringBuilder) = fromUse(this.inputStream()) pipe stringBuilder

}
