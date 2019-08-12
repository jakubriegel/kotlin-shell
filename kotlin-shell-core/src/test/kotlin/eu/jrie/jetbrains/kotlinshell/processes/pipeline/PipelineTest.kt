package eu.jrie.jetbrains.kotlinshell.processes.pipeline

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.BytePacketBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.OutputStream

@ExperimentalCoroutinesApi
class PipelineTest {

    private lateinit var contextMock: ProcessExecutionContext

    companion object {
        private const val RW_PACKET_SIZE: Long = 4
        private const val CHANNEL_BUFFER_SIZE = 4
    }

    @Test
    fun `should create new pipeline from process`() {
        // given
        val processMock = mockk<Process>()
        val executableMock = processExecutableSpy(processMock)

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(executableMock, contextMock,  CHANNEL_BUFFER_SIZE)
        }

        // then
        verify {
            executableMock.context
            executableMock.process
        }
        verify (exactly = 1) { executableMock setProperty("context") value ofType(ProcessExecutionContext::class) }
        coVerify (exactly = 1) {
            executableMock.init()
            executableMock.exec()
            executableMock.join()
        }
        confirmVerified(executableMock)

        assertEquals(processMock, pipeline.processes.last())
        assertFalse(pipeline.closed)
    }

    @Test
    fun `should create new pipeline from lambda`() {
        // given
        var started: Boolean? = null
        val lambda: PipelineContextLambda = { started = true }

        // when
        val pipeline = runTest {
            Pipeline.fromLambda(lambda, contextMock,  CHANNEL_BUFFER_SIZE)
        }

        // then
        verify {
            contextMock.stdin
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }

        assertTrue(started!!)
        assertIterableEquals(emptyList<Process>(), pipeline.processes)
        assertFalse(pipeline.closed)
    }

    @Test
    fun `should create new pipeline from channel`() {
        // given
        val channel: ProcessReceiveChannel = spyk()

        // when
        val pipeline = runTest {
            Pipeline.fromChannel(channel, contextMock,  CHANNEL_BUFFER_SIZE)
        }

        // then
        confirmVerified(channel)
        confirmVerified(contextMock)

        assertIterableEquals(emptyList<Process>(), pipeline.processes)
        assertFalse(pipeline.closed)
    }

    @Test
    fun `should add process to pipeline`() {
        // given
        val processMock = mockk<Process>()
        val executableMock = processExecutableSpy(processMock)

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .throughProcess(executableMock)
        }


        // then
        verify {
            executableMock.context
            executableMock.process
            executableMock setProperty("context") value ofType(ProcessExecutionContext::class)
        }
        coVerify (exactly = 1) {
            executableMock.init()
            executableMock.exec()
            executableMock.join()
        }
        confirmVerified(executableMock)

        assertEquals(processMock, pipeline.processes.last())
        assertEquals(2, pipeline.processes.size)
        assertFalse(pipeline.closed)
    }

    @Test
    fun `should add lambda to pipeline`() {
        // given
        var started: Boolean? = null
        val lambda: PipelineContextLambda = { started = true }

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .throughLambda(lambda = lambda).apply { join() }
        }

        // then
        verify {
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }

        assertTrue(started!!)
        assertEquals(1, pipeline.processes.size)
        assertFalse(pipeline.closed)
    }

    @Test
    fun `should end pipeline with channel`() {
        // given
        val channel: ProcessChannel = spyk(Channel())

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .toEndChannel(channel)
        }

        // then
        verify { channel.close() }
        confirmVerified(channel)

        verify {
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }
        confirmVerified(contextMock)

        assertEquals(1, pipeline.processes.size)
        assertTrue(pipeline.closed)
    }

    @Test
    fun `should end pipeline with channel and do not close it`() {
        // given
        val channel: ProcessChannel = spyk(Channel())

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .toDefaultEndChannel(channel)
        }

        // then
        confirmVerified(channel)

        verify {
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }
        confirmVerified(contextMock)

        assertEquals(1, pipeline.processes.size)
        assertTrue(pipeline.closed)
    }

    @Test
    fun `should end pipeline with packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .toEndPacket(builder)
        }

        // then
        verify {
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }
        confirmVerified(contextMock)

        assertEquals(1, pipeline.processes.size)
        assertTrue(pipeline.closed)
    }

    @Test
    fun `should end pipeline with stream`() {
        // given
        val stream = mockk<OutputStream>()

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .toEndStream(stream)
        }

        // then
        verify {
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }
        confirmVerified(contextMock)

        assertEquals(1, pipeline.processes.size)
        assertTrue(pipeline.closed)
    }

    @Test
    fun `should end pipeline with string builder`() {
        // given
        val builder = StringBuilder()

        // when
        val pipeline = runTest {
            Pipeline.fromProcess(processExecutableSpy(), contextMock,  CHANNEL_BUFFER_SIZE)
                .toEndStringBuilder(builder)
        }

        // then
        verify {
            contextMock.stdout
            contextMock.stderr
            contextMock.commander
        }
        confirmVerified(contextMock)

        assertEquals(1, pipeline.processes.size)
        assertTrue(pipeline.closed)
    }

    @Suppress("UNUSED_EXPRESSION") // bug in Kotlin compiler 1.3.50 EAP
    private fun <T> runTest(test: suspend PipelineTest.() -> T): T = runBlocking {
        contextMock = contextMock(this)
        test()
    }

    private fun contextMock(scopeMock: CoroutineScope = mockk()) = mockk<ProcessExecutionContext> {
        every { stdin } returns Channel()
        every { stdout } returns Channel()
        every { stderr } returns Channel()
        every { commander } returns  mockk {
            every { scope } returns scopeMock
            coEvery { awaitProcess(any()) } just runs
        }
    }

    private fun processExecutableSpy(processMock: Process = mockk()) = spyk (ProcessExecutable(contextMock(), mockk())) {
        every { process } returns processMock
        every { init() } just runs
        coEvery { exec() } just runs
        coEvery { join() } just runs
    }
}
