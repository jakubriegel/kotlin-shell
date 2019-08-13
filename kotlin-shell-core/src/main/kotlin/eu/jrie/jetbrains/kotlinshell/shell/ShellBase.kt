@file:Suppress("EXPERIMENTAL_API_USAGE")

package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeText
import java.io.File

typealias ShellCommand = PipelineContextLambda//() -> String

@Suppress("PropertyName")
@ExperimentalCoroutinesApi
interface ShellBase : ProcessExecutionContext {
    /**
     * Environment of this shell.
     * These variables are being inherited to sub shells.
     *
     * @see [variables]
     */
    val environment: Map<String, String>
    /**
     * Variables of this shell.
     * These variables are not being inherited to sub shells.
     *
     * @see [environment]
     */
    val variables: Map<String, String>
    /**
     * Current directory of this shell
     */
    val directory: File

    suspend operator fun ShellCommand.invoke() = this.invoke(this@ShellBase)

    fun command(block: ShellBase.() -> String): PipelineContextLambda = {
        it.stdout.send(
            buildPacket { writeText(this@ShellBase.block()) }
        )
    }

    suspend fun finalize()

    fun closeOut() {
        stdout.close()
        stderr.close()
    }

    val SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE: Int
    val PIPELINE_RW_PACKET_SIZE: Long
    val PIPELINE_CHANNEL_BUFFER_SIZE: Int

}
