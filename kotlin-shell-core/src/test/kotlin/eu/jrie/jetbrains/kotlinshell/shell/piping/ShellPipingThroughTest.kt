package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.Readonly
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.PIPELINE_CHANNEL_BUFFER_SIZE
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import java.io.File

@ExperimentalCoroutinesApi
class ShellPipingThroughTest {

    private val shell = SampleShell()

    private class SampleShell : ShellPipingThrough {
        override fun cd(dir: File): File = mockk()
        override fun variable(variable: Pair<String, String>) = Unit
        override fun export(env: Pair<String, String>) = Unit
        override fun unset(key: String) = Unit
        override fun Readonly.variable(variable: Pair<String, String>) = Unit
        override fun Readonly.export(env: Pair<String, String>) = Unit

        override val scope: CoroutineScope = mockk()

        override var environment: Map<String, String> = mapOf(
            PIPELINE_CHANNEL_BUFFER_SIZE to "$DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE"
        )
        override var variables: Map<String, String> = emptyMap()
        override var directory: File = File("")

        override val commander: ProcessCommander = mockk()

        override val stdout: ProcessSendChannel = Channel()
        override val stderr: ProcessSendChannel = Channel()
        override val stdin: ProcessReceiveChannel = Channel()

        override suspend fun finalize() {}
    }
}