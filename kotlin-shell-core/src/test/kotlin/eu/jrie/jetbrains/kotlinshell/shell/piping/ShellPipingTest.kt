package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ExecutionMode
import eu.jrie.jetbrains.kotlinshell.shell.Readonly
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeText
import kotlinx.io.streams.writePacket
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalCoroutinesApi
class ShellPipingTest {

    private val shell = SampleShell()

    private val abc: ByteReadPacket get() = buildPacket { writeText("abc") }
    private val def: ByteReadPacket get() = buildPacket { writeText("def") }
    private val xyz: ByteReadPacket get() = buildPacket { writeText("xyz") }

    @Test
    fun `should create ContextLambda`() {
        // when
        val lambda = shell.contextLambda {
            it.stdin.receive()
            it.stdout.send(abc)
            it.stderr.send(def)
        }

        // then
        assertLambda(lambda)
    }

    @Test
    fun `should create PacketLambda`() {
        // when
        val lambda = shell.packetLambda { abc to def }

        // then
        assertLambda(lambda)
    }

    @Test
    fun `should create ByteArrayLambda`() {
        // when
        val lambda = shell.byteArrayLambda { abc.readBytes() to def.readBytes() }

        // then
        assertLambda(lambda)
    }

    @Test
    fun `should create StringLambda`() {
        // when
        val lambda = shell.stringLambda { abc.readText() to def.readText() }

        // then
        assertLambda(lambda)
    }


    @Test
    fun `should create StreamLambda`() {
        // expect no error
        shell.streamLambda { stdin, stdout, stderr ->
            stdin.use { it.readAllBytes() }
            stdout.writePacket(abc)
            stderr.writePacket(def)
        }
    }

    @Test
    fun `should create packet from given ByteArray`() {
        // given
        val bytes = "abc".toByteArray()

        // when
        val packet = shell.packet(bytes)

        // then
        Assertions.assertIterableEquals(bytes.toList(), packet.readBytes().toList())
    }

    @Test
    fun `should create packet from given String`() {
        // given
        val string = "abc"

        // when
        val packet = shell.packet(string)

        // then
        assertEquals(string, packet.readText())
    }

    @Test
    fun `should create empty packet`() {
        // when
        val packet = shell.emptyPacket()

        // then
        assertTrue(packet.isEmpty)
    }

    @Test
    fun `should create empty ByteArray`() {
        // when
        val array = shell.emptyByteArray()

        // then
        assertEquals(0, array.size)
    }

    private fun assertLambda(lambda: PipelineContextLambda) = runBlocking {
        val context = SampleContext()

        (context.stdin as ProcessChannel).apply {
            send(xyz)
            close()
        }

        lambda(context)

        assertEquals(abc.readText(), (context.stdout as ProcessChannel).receive().readText())
        assertEquals(def.readText(), (context.stderr as ProcessChannel).receive().readText())
        assertTrue(context.stdout.isEmpty)
        assertTrue(context.stderr.isEmpty)
    }

    class SampleShell : ShellPiping {

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

    class SamplePipingDSLShell : AbstractPipingDSLShell {

        var testScope: CoroutineScope? = null
        override val scope: CoroutineScope get() = testScope!!
        override val environment: Map<String, String> = mapOf("PIPELINE_CHANNEL_BUFFER_SIZE" to "1")

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

    private class SampleContext : ExecutionContext {
        override val stdin: ProcessReceiveChannel = Channel(5)
        override val stdout: ProcessSendChannel = Channel(5)
        override val stderr: ProcessSendChannel = Channel(5)
    }
}