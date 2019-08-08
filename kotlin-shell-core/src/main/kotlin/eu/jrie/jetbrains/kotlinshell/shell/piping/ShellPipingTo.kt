package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.io.core.BytePacketBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@ExperimentalCoroutinesApi
interface ShellPipingTo : ShellBase {
    /**
     * Ends this [Pipeline] with [channel]
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.pipe(channel: ProcessSendChannel) = toEndChannel(channel)

    /**
     * Ends this [Pipeline] with [packetBuilder]
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.pipe(packetBuilder: BytePacketBuilder) = toEndPacket(packetBuilder)

    /**
     * Ends this [Pipeline] with [stream]
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.pipe(stream: OutputStream) = toEndStream(stream)

    /**
     * Ends this [Pipeline] with [file]
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.pipe(file: File) = toEndFile(file, false)

    /**
     * Ends this [Pipeline] with [file] by appending to it
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.pipeAppend(file: File) = toEndFile(file, true)

    private suspend fun Pipeline.toEndFile(file: File, append: Boolean) = toEndStream(FileOutputStream(file, append))

    /**
     * Ends this [Pipeline] with [stringBuilder]
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @ExperimentalCoroutinesApi
    suspend infix fun Pipeline.pipe(stringBuilder: StringBuilder) = toEndStringBuilder(stringBuilder)
}
