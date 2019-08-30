package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase
import eu.jrie.jetbrains.kotlinshell.shell.piping.ShellPipingThrough
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.io.core.BytePacketBuilder
import java.io.File
import java.io.OutputStream

@ExperimentalCoroutinesApi
interface ShellPipingFromLambda : ShellPipingThrough {
    /**
     * Starts new [Pipeline] from [lambda].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend fun from(lambda: PipelineContextLambda) = Pipeline.fromLambda(
        lambda, this, env(ShellBase.PIPELINE_CHANNEL_BUFFER_SIZE).toInt()
    )

    /**
     * Starts new [Pipeline] from this lambda to [process].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    @InternalCoroutinesApi
    suspend infix fun PipelineContextLambda.pipe(process: ProcessExecutable) = from(this) pipe process

    /**
     * Starts new [Pipeline] from this lambda to [lambda].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipe(lambda: PipelineContextLambda) = from(this) pipe lambda

    /**
     * Starts new [Pipeline] from this lambda to [channel].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipe(channel: ProcessSendChannel) = from(this) pipe channel

    /**
     * Starts new [Pipeline] from this lambda to [packetBuilder].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipe(packetBuilder: BytePacketBuilder) = from(this) pipe packetBuilder

    /**
     * Starts new [Pipeline] from this lambda to [stream].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipe(stream: OutputStream) = from(this) pipe stream

    /**
     * Starts new [Pipeline] from this lambda to [file].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipe(file: File) = from(this) pipe file

    /**
     * Starts new [Pipeline] from this [PipelineContextLambda] and appends [file].
     * Shall be wrapped with piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipeAppend(file: File) = from(this) pipeAppend  file

    /**
     * Starts new [Pipeline] from this lambda to [stringBuilder].
     * Part of piping DSL
     *
     * @return this [Pipeline]
     */
    suspend infix fun PipelineContextLambda.pipe(stringBuilder: StringBuilder) = from(this) pipe stringBuilder
}
