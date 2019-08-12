package eu.jrie.jetbrains.kotlinshell.processes.process

import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.ENVIRONMENT
import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.VIRTUAL_PID
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File

@ExperimentalCoroutinesApi
class ProcessTest {

    private lateinit var process: Process

    @Test
    fun `should start process`() {
        // when
        runTest { process.start() }

        // then
        assertEquals(process.pcb.state, ProcessState.RUNNING)
    }

    @ParameterizedTest(name = "{index} {0} should throw exception when tried to start not READY process")
    @EnumSource(ProcessState::class)
    fun `should throw exception when tried to start not READY process`(state: ProcessState) = runTest {
        // given
        process.pcb.state = state

        // when
        if (state != ProcessState.READY) {
            // expect
            val e  = runCatching { process.start() }
            assertEquals(Exception::class, e.exceptionOrNull()!!::class)
        }
    }

    @Test
    fun `should await process`() = runBlocking {
        val timeout: Long = 500

        // when
        runTest {
            every { process.isAlive() } returns true

            process.await(timeout)
        }

        // then
        assertEquals(ProcessState.TERMINATED, process.pcb.state)
    }

    @Test
    fun `should kill process`() = runBlocking {
        // when
        runTest {
            process.kill()
        }

        // then
        assertEquals(ProcessState.TERMINATED, process.pcb.state)
    }

    @Test
    fun `should return true if process finished executing but state is still RUNNING`() {
        // when
        val result = runTest {
            process.pcb.state = ProcessState.RUNNING
            process.isAlive()
        }

        // then
        assertTrue(result)
    }

    @Test
    fun `should return false if process state is not RUNNING and process finished executing`() {
        // when
        val result = runTest {
            process.pcb.state = ProcessState.TERMINATED
            process.isAlive()
        }

        // then
        assertFalse(result)
    }

    private fun <T> runTest(test: suspend ProcessTest.() -> T): T = runBlocking {
        process = spyk(SampleProcess(this))
        val result = test()
        result
    }

    private class SampleProcess (
        scope: CoroutineScope
    ) : Process(
        VIRTUAL_PID,
        ENVIRONMENT,
        File(""),
        Channel(),
        Channel(),
        Channel(),
        scope
    ) {
        override val pcb: PCB = spyk()
        override val statusCmd = ""
        override val statusOther = ""

        override suspend fun execute() = delay(1)
        override fun isRunning(): Boolean = false
        override suspend fun expect(timeout: Long) {}
        override fun destroy() {}
    }

}
