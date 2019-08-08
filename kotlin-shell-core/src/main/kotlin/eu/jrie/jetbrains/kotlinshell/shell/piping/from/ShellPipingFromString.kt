package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.io.core.BytePacketBuilder
import java.io.File
import java.io.OutputStream

@ExperimentalCoroutinesApi
interface ShellPipingFromString : ShellPipingFromStream {
    /**
     * Starts new [Pipeline] from this [String] to [process].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(process: ProcessExecutable) = fromUse(this.byteInputStream()) pipe process

    /**
     * Starts new [Pipeline] from this [String] to [lambda].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(lambda: PipelineContextLambda) = fromUse(this.byteInputStream()) pipe lambda

    /**
     * Starts new [Pipeline] from this [String] to [channel].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(channel: ProcessSendChannel) = fromUse(this.byteInputStream()) pipe channel

    /**
     * Starts new [Pipeline] from this [String] to [packetBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(packetBuilder: BytePacketBuilder) = fromUse(this.byteInputStream()) pipe packetBuilder

    /**
     * Starts new [Pipeline] from this [String] to [stream].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(stream: OutputStream) = fromUse(this.byteInputStream()) pipe stream

    /**
     * Starts new [Pipeline] from this [String] to [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(file: File) = fromUse(this.byteInputStream()) pipe file

    /**
     * Starts new [Pipeline] from this [String] and appends [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipeAppend(file: File) = fromUse(this.byteInputStream()) pipeAppend  file

    /**
     * Starts new [Pipeline] from this [String] to [stringBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun String.pipe(stringBuilder: StringBuilder) = fromUse(this.byteInputStream()) pipe stringBuilder
}
