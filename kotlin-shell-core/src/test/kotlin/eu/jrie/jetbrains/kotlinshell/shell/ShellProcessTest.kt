package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.configuration.KtsProcessConfiguration
import eu.jrie.jetbrains.kotlinshell.processes.configuration.SystemProcessConfiguration
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessState
import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.VIRTUAL_PID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalCoroutinesApi
class ShellProcessTest {

    private val shell = SampleShell()

    @Test
    fun `should construct system process executable from configuration`() {
        // given
        var somethingCalled: Boolean? = null
        fun something() { somethingCalled = true }

        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        // when
        shell.systemProcess { something() }

        // then
        assertTrue(somethingCalled!!)
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should construct system process executable with extension function from command`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        // when
        with(shell) { "cmd".process() }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should construct system process executable from command`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        // when
        with(shell) { systemProcess("cmd") }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should create system process executable with extension function from command with args`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        // when
        with(shell) { "some cmd with args".process() }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should create system process executable from command with args`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        // when
        with(shell) { systemProcess("some cmd with args") }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should construct system process executable with extension function from file`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()
        val file = File("some/file")

        // when
        with(shell) { file.process() }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should construct system process executable from file`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()
        val file = File("some/file")

        // when
        with(shell) { systemProcess(file) }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should create system process executable with extension function from file with args`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()
        val file = File("some/file")

        // when
        with(shell) { file.process("arg1", "arg2") }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should create system process executable from file with args`() {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()
        val file = File("some/file")

        // when
        with(shell) { systemProcess(file, "arg1", "arg2") }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should invoke system process executable from command`() = runBlocking {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        mockkConstructor(ProcessExecutable::class)
        coEvery { anyConstructed<ProcessExecutable>().invoke(any()) } just runs
        every { anyConstructed<ProcessExecutable>().process } returns mockk()

        // when
        with(shell) { "cmd"() }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
        coVerify { anyConstructed<ProcessExecutable>().invoke(any()) }
    }

    @Test
    fun `should invoke system process executable from file`() = runBlocking {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        mockkConstructor(ProcessExecutable::class)
        coEvery { anyConstructed<ProcessExecutable>().invoke(any()) } just runs
        every { anyConstructed<ProcessExecutable>().process } returns mockk()

        val file = File("some/file")

        // when
        with(shell) { file() }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
        coVerify { anyConstructed<ProcessExecutable>().invoke(any()) }
    }

    @Test
    fun `should invoke system process executable from file with args`() = runBlocking {
        // given
        mockkConstructor(SystemProcessConfiguration::class)
        every { anyConstructed<SystemProcessConfiguration>().builder() } returns mockk()

        mockkConstructor(ProcessExecutable::class)
        coEvery { anyConstructed<ProcessExecutable>().invoke(any()) } just runs
        every { anyConstructed<ProcessExecutable>().process } returns mockk()

        val file = File("some/file")

        // when
        with(shell) { file("arg1", "arg2") }

        // then
        verify {
            anyConstructed<SystemProcessConfiguration>().env(any())
            anyConstructed<SystemProcessConfiguration>().dir(any())
            anyConstructed<SystemProcessConfiguration>().builder()
        }
        coVerify { anyConstructed<ProcessExecutable>().invoke(any()) }
    }

    @Test
    fun `should construct kts process executable from configuration`() {
        // given
        var somethingCalled: Boolean? = null
        fun something() { somethingCalled = true }

        mockkConstructor(KtsProcessConfiguration::class)
        every { anyConstructed<KtsProcessConfiguration>().builder() } returns mockk()

        // when
        shell.ktsProcess { something() }

        // then
        assertTrue(somethingCalled!!)
        verify {
            anyConstructed<KtsProcessConfiguration>().env(any())
            anyConstructed<KtsProcessConfiguration>().dir(any())
            anyConstructed<KtsProcessConfiguration>().builder()
        }
    }

    @Test
    fun `should return only running processes`() {
        // when
        val result = with(shell) { processes.running() }

        // then
        assertIterableEquals(listOf(runningProcessMock), result)
    }

    @Test
    fun `should return only terminated processes`() {
        // when
        val result = with(shell) { processes.terminated() }

        // then
        assertIterableEquals(listOf(terminatedProcessMock), result)
    }

    @Test
    fun `should retrieve process by vPID`() {
        // when
        val result = with(shell) { processes.byVPID(VIRTUAL_PID) }

        // then
        assertEquals(readyProcessMock, result)
    }

    @Test
    fun `should await process`() = runBlocking {
        // when
        with(shell) { runningProcessMock.join() }

        // then
        coVerify { commanderMock.awaitProcess(runningProcessMock) }
    }

    @Test
    fun `should await all processes`() = runBlocking {
        // when
        with(shell) { joinAll() }

        // then
        coVerify { commanderMock.awaitAll() }
    }

    @Test
    fun `should kill given processes`() = runBlocking {
        // when
        shell.kill(runningProcessMock)

        // the
        coVerify { commanderMock.killProcess(runningProcessMock) }
    }

    @Test
    fun `should kill all processes`() = runBlocking {
        // when
        with(shell) { killAll() }

        // then
        coVerify { commanderMock.killAll() }
    }

    @ExperimentalCoroutinesApi
    private companion object {

        val readyProcessMock = mockk<Process> {
            every { vPID } returns VIRTUAL_PID
            every { pcb } returns spyk {
                every { state } returns ProcessState.READY
            }
        }

        val runningProcessMock = mockk<Process> {
            every { vPID } returns VIRTUAL_PID + 1
            every { pcb } returns spyk {
                every { state } returns ProcessState.RUNNING
            }
        }

        val terminatedProcessMock = mockk<Process> {
            every { vPID } returns VIRTUAL_PID + 2
            every { pcb } returns spyk {
                every { state } returns ProcessState.TERMINATED
            }
        }

        val processes = listOf(readyProcessMock, runningProcessMock, terminatedProcessMock)

        val commanderMock = mockk<ProcessCommander>{
            every { processes } returns ShellProcessTest.processes.toMutableSet()
            coEvery {
                startProcess(any())
                killProcess(any())
                awaitProcess(any())
                awaitAll()
                killAll()
            } just runs
        }
    }

    @ExperimentalCoroutinesApi
    private class SampleShell : ShellProcess {
        override val detachedProcesses: List<Process> = emptyList()
        override val daemons: List<Process> = emptyList()
        override val nullin: ProcessReceiveChannel = Channel()
        override val nullout: ProcessSendChannel = Channel()

        override suspend fun detach(executable: ProcessExecutable): Process = mockk()
        override suspend fun joinDetached() = Unit
        override suspend fun fg(process: Process) = Unit
        override suspend fun daemon(executable: ProcessExecutable): Process = mockk()

        override val environment: Map<String, String> = emptyMap()
        override val variables: Map<String, String> = emptyMap()
        override val directory: File = File("path")

        override fun exec(block: Shell.() -> String): ShellExecutable = mockk()

        override suspend fun finalize() = Unit

        override val stdin: ProcessReceiveChannel = Channel()
        override val stdout: ProcessSendChannel = Channel()
        override val stderr: ProcessSendChannel = Channel()
        @ExperimentalCoroutinesApi
        override val commander: ProcessCommander = commanderMock

        override val SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE: Int = 1
        override val PIPELINE_RW_PACKET_SIZE: Long = 1
        override val PIPELINE_CHANNEL_BUFFER_SIZE: Int = 1

    }

}