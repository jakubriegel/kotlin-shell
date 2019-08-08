@file:Suppress("EXPERIMENTAL_API_USAGE")

package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import java.io.File

typealias BlockingShellScript = Shell.() -> Unit
typealias ShellScript = suspend Shell.() -> Unit

const val DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE: Int = 512
const val DEFAULT_PIPELINE_RW_PACKET_SIZE: Long = 256
const val DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE: Int = 16

/**
 * Creates new [Shell] with given parameters and executes the [script]
 */
@ExperimentalCoroutinesApi
suspend fun shell(
    env: Map<String, String>? = null,
    dir: File? = null,
    systemProcessInputStreamBufferSize: Int = DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE,
    pipelineRwPacketSize: Long = DEFAULT_PIPELINE_RW_PACKET_SIZE,
    pipelineChannelBufferSize: Int = DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE,
    script: ShellScript
) = coroutineScope {
    shell(
        env,
        dir,
        this,
        systemProcessInputStreamBufferSize,
        pipelineRwPacketSize,
        pipelineChannelBufferSize,
        script
    )
}

/**
 * Creates new [Shell] with given parameters and executes the [script]
 */
@ExperimentalCoroutinesApi
suspend fun shell(
    env: Map<String, String>? = null,
    dir: File? = null,
    scope: CoroutineScope,
    systemProcessInputStreamBufferSize: Int = DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE,
    pipelineRwPacketSize: Long = DEFAULT_PIPELINE_RW_PACKET_SIZE,
    pipelineChannelBufferSize: Int = DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE,
    script: ShellScript
) {
    shell(
        env,
        dir,
        ProcessCommander(scope),
        systemProcessInputStreamBufferSize,
        pipelineRwPacketSize,
        pipelineChannelBufferSize,
        script
    )
    Shell.logger.debug("shell end")
}

@ExperimentalCoroutinesApi
private suspend fun shell(
    env: Map<String, String>? = null,
    dir: File? = null,
    commander: ProcessCommander,
    systemProcessInputStreamBufferSize: Int,
    pipelineRwPacketSize: Long,
    pipelineChannelBufferSize: Int,
    script: ShellScript
) {
    Shell.build(
        env,
        dir,
        commander,
        systemProcessInputStreamBufferSize,
        pipelineRwPacketSize,
        pipelineChannelBufferSize
    )
        .apply { script() }
        .finalize()
    Shell.logger.debug("script end")
}
