package eu.jrie.jetbrains.kotlinshell.processes

import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import eu.jrie.jetbrains.kotlinshell.processes.process.system.SystemProcessBuilder
import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.PROCESS_COMMAND
import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.PROCESS_NAME
import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.VIRTUAL_PID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class ProcessCommanderTest {
    private val scopeSpy = spyk<CoroutineScope>()

    private val commander = ProcessCommander(scopeSpy)

    @Test
    fun `should create new SystemProcess`() = runBlocking {
        // given
        val scope = this
        val c = ProcessCommander(scope)
        val builder = spyk(SystemProcessBuilder(PROCESS_COMMAND, systemProcessInputStreamBufferSize = 1)).withChannels()

        // when
        val process = c.createProcess(builder)

        // then
        verifyOrder {
            builder.withVirtualPID(ofType(Int::class))
            builder.withScope(scope)
            builder.build()
        }

        assertTrue(process.vPID > 0)
    }

    @Test
    fun `should assign unique vPID to process`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val builder1 = spyk(SystemProcessBuilder(PROCESS_COMMAND, systemProcessInputStreamBufferSize = 1)).withChannels()
        val builder2 = spyk(SystemProcessBuilder(PROCESS_COMMAND, systemProcessInputStreamBufferSize = 1)).withChannels()

        // when
        val process1 = c.createProcess(builder1)
        val process2 = c.createProcess(builder2)

        // then
        verify (exactly = 1) {
            builder1.withVirtualPID(ofType(Int::class))
            builder2.withVirtualPID(ofType(Int::class))
        }
        assertTrue(process1.vPID > 0)
        assertTrue(process2.vPID > 0)
        assertTrue(process1.vPID != process2.vPID)
    }

    @Test
    fun `should start process`() = runBlocking {
        // given
        val processMock = mockk<Process> {
            coEvery { start() } returns mockk()
            every { vPID } returns VIRTUAL_PID
        }

        // when
        commander.startProcess(processMock)

        // then
        coVerify (exactly = 1) { processMock.start() }

    }

    @Test
    fun `should await process`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val timeout: Long = 500
        val processMock = mockk<Process> {
            coEvery { await(timeout) } just runs
            every { vPID } returns VIRTUAL_PID
            every { name } returns PROCESS_NAME
        }

        val builderSpy = spyk<ProcessBuilder> {
            every { build() } returns processMock
        }
        c.createProcess(builderSpy)

        // when
        c.awaitProcess(processMock, timeout)

        // then
        coVerify (exactly = 1) { processMock.await(timeout) }
    }

    @Test
    fun `should throw exception when await unknown process`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val timeout: Long = 500
        val processMock = mockk<Process> {
            coEvery { await(timeout) } just runs
            every { vPID } returns VIRTUAL_PID
        }

        // when
        val result = runCatching { c.awaitProcess(processMock, timeout) }

        // then
        coVerify (exactly = 0) { processMock.await(timeout) }
        assertTrue(result.isFailure)
    }

    @Test
    fun `should throw exception when await unknown process by vPID`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val timeout: Long = 500
        val processMock = mockk<Process> {
            coEvery { await(timeout) } just runs
            every { vPID } returns VIRTUAL_PID
        }

        // when
        val result = runCatching { c.awaitProcess(processMock, timeout) }

        // then
        coVerify (exactly = 0) { processMock.await(timeout) }
        assertTrue(result.isFailure)
    }

    @Test
    fun `should await all processes`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val processMock1 = mockk<Process> {
            coEvery { await(0) } just runs
            every { name } returns PROCESS_NAME
        }
        val processMock2 = mockk<Process> {
            coEvery { await(0) } just runs
            every { name } returns PROCESS_NAME
        }

        val builder1 = spyk<ProcessBuilder> {
            every { build() } returns processMock1
        }
        val builder2 = spyk<ProcessBuilder> {
            every { build() } returns processMock2
        }

        c.createProcess(builder1)
        c.createProcess(builder2)

        // when
        c.awaitAll()

        // then
        coVerify (exactly = 1) {
            processMock1.await(0)
            processMock2.await(0)
        }
    }

    @Test
    fun `should kill process`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val processMock = mockk<Process> {
            coEvery { kill() } just runs
        }

        val builderSpy = spyk<ProcessBuilder> {
            every { build() } returns processMock
        }
        c.createProcess(builderSpy.withChannels())

        // when
        c.killProcess(processMock)

        // then
        coVerify (exactly = 1) { processMock.kill() }
    }

    @Test
    fun `should throw exception when kill unknown process`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val processMock = mockk<Process> {
            coEvery { kill() } just runs
        }

        // when
        val e = runCatching { c.killProcess(processMock) }
            .exceptionOrNull()!!

        // then
        coVerify (exactly = 0) { processMock.kill() }
        assertEquals(Exception::class, e::class)
    }

    @Test
    fun `should kill all processes`() = runBlocking {
        // given
        val c = ProcessCommander(this)
        val processMock1 = mockk<Process> {
            coEvery { kill() } just runs
        }
        val processMock2 = mockk<Process> {
            coEvery { kill() } just runs
        }

        val builder1 = spyk<ProcessBuilder> {
            every { build() } returns processMock1
        }
        val builder2 = spyk<ProcessBuilder> {
            every { build() } returns processMock2
        }

        c.createProcess(builder1.withChannels())
        c.createProcess(builder2.withChannels())

        // when
        c.killAll()

        // then
        coVerify (exactly = 1) {
            processMock1.kill()
            processMock2.kill()
        }
    }

    private fun ProcessBuilder.withChannels() = apply {
        withStdin(Channel())
        withStdout(Channel())
        withStderr(Channel())
    }
}
