package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ExecutionMode
import eu.jrie.jetbrains.kotlinshell.shell.Readonly
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalCoroutinesApi
class ShellForkingTest {

    private val shell = SampleShell()

    @Test
    fun `should construct PipelineFork`() = runBlocking {
        // given
        var err: ProcessReceiveChannel? = null
        shell.testScope = this

        // when
        val fork = shell.pipelineFork {
            err = it
            it pipe {}
        }

        // then
        val channel: ProcessReceiveChannel = Channel()
        fork(ShellPipingTest.SamplePipingDSLShell().apply { testScope = this@runBlocking }, channel)
        Assertions.assertEquals(channel, err)
    }

    class SampleShell : ShellForking {
        var testScope: CoroutineScope? = null
        override val scope: CoroutineScope get() = testScope!!
        override val environment: Map<String, String> = mapOf("PIPELINE_CHANNEL_BUFFER_SIZE" to "1")

        override suspend fun pipeline(mode: ExecutionMode, pipelineConfig: PipelineConfig): Pipeline = mockk()
        override val detachedPipelines: List<Pair<Int, Pipeline>> = emptyList()
        override suspend fun detach(pipelineConfig: PipelineConfig): Pipeline = mockk()
        override suspend fun fg(pipeline: Pipeline) = Unit
        override fun cd(dir: File): File = mockk()
        override fun variable(variable: Pair<String, String>) = Unit
        override fun Readonly.variable(variable: Pair<String, String>) = Unit
        override fun export(env: Pair<String, String>) = Unit
        override fun Readonly.export(env: Pair<String, String>) = Unit
        override fun unset(key: String) = Unit
        override val variables: Map<String, String> = mockk()
        override val directory: File = mockk()
        override suspend fun finalize()  = Unit
        override val stdin: ProcessReceiveChannel = mockk()
        override val stdout: ProcessSendChannel = mockk()
        override val stderr: ProcessSendChannel = mockk()
        @ExperimentalCoroutinesApi
        override val commander: ProcessCommander = mockk()
    }
}