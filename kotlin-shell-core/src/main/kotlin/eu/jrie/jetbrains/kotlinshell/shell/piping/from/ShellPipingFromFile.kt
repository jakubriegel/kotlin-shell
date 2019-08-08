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
interface ShellPipingFromFile : ShellPipingFromStream {
    /**
     * Starts new [Pipeline] from this [File] to [process].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(process: ProcessExecutable) = fromUse(this.inputStream()) pipe process

    /**
     * Starts new [Pipeline] from this [File] to [lambda].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(lambda: PipelineContextLambda) = fromUse(this.inputStream()) pipe lambda

    /**
     * Starts new [Pipeline] from this [File] to [channel].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(channel: ProcessSendChannel) = fromUse(this.inputStream()) pipe channel

    /**
     * Starts new [Pipeline] from this [File] to [packetBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(packetBuilder: BytePacketBuilder) = fromUse(this.inputStream()) pipe packetBuilder

    /**
     * Starts new [Pipeline] from this [File] to [stream].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(stream: OutputStream) = fromUse(this.inputStream()) pipe stream

    /**
     * Starts new [Pipeline] from this [File] to [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(file: File) = fromUse(this.inputStream()) pipe file

    /**
     * Starts new [Pipeline] from this [File] and appends [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipeAppend(file: File) = fromUse(this.inputStream()) pipeAppend  file

    /**
     * Starts new [Pipeline] from this [File] to [stringBuilder].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun File.pipe(stringBuilder: StringBuilder) = fromUse(this.inputStream()) pipe stringBuilder
}
