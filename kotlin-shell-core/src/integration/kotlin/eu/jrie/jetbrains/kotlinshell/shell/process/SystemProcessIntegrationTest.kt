package eu.jrie.jetbrains.kotlinshell.shell.process

import eu.jrie.jetbrains.kotlinshell.ProcessBaseIntegrationTest
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessState
import eu.jrie.jetbrains.kotlinshell.shell.ExecutionMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class SystemProcessIntegrationTest : ProcessBaseIntegrationTest() {

    @Test
    fun `should execute "echo hello world"`() {
        // when
        shell {
            val echo = systemProcess {
                cmd {
                    "echo" withArgs listOf("hello", "world")
                }
            }

            pipeline { echo pipe storeResult }
        }

        // then
        assertEquals("hello world\n", readResult())
    }

    @Test
    fun `should run process from command line"`() {
        // given
        var process: Process? = null

        // when
        shell {
            "echo hello world"()
            process = processes.first()
        }

        // then
        with(process!!) {
            assertTrue(vPID > 0)
            assertEquals(pcb.state, ProcessState.TERMINATED)
        }
    }

    @Test
    fun `should execute "ls -l"`() {
        // given
        testFile("file1")
        testFile("file2")
        testDir()

        val dirRegex = Regex("drw.+testdir\n")
        val fileRegex = Regex("-rw.+file[0-9]\n")

        // when
        shell {
            pipeline { "ls -ls".process() pipe storeResult }
        }

        // then
        val result = readResult()
        assertRegex(dirRegex, result)
        assertRegex(fileRegex, result)
    }

    @Test
    fun `should run process sequentially`() {
        // given
        val file = scriptFile(250)
        var stateAfterCall: ProcessState? = null

        // when
        shell {
            val script = file.process()
            script()
            stateAfterCall = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.TERMINATED, stateAfterCall)
    }

    @Test
    fun `should detach process`() {
        // given
        val file = scriptFile(500)
        var stateAfterCall: ProcessState? = null

        // when
        shell {
            val script = file.process()
            detach(script)
            delay(5)
            stateAfterCall = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.RUNNING, stateAfterCall)
    }

    @Test
    fun `should detach process with invoke`() {
        // given
        val file = scriptFile(500)
        var stateAfterCall: ProcessState? = null

        // when
        shell {
            file(ExecutionMode.DETACHED)
            delay(5)
            stateAfterCall = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.RUNNING, stateAfterCall)
    }

    @Test
    fun `should detach equivalent processes`() {
        // given
        val file = scriptFile(500)
        var stateAfterCall: ProcessState? = null

        // when
        shell {
            val script = file.process()
            detach(script.copy(), script.copy(), script.copy())
            delay(5)
            stateAfterCall = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.RUNNING, stateAfterCall)
    }

    @Test
    fun `should list detached processes`() {
        // given
        val file = scriptFile(5)

        // when
        shell {
            val p1 = file.process()
            val p2 = file.process()
            val p3 = file.process()
            detach(p1, p2, p3)

            // then
            assertIterableEquals(listOf(1 to p1.process, 2 to p2.process, 3 to p3.process), detachedProcesses)
        }
    }

    @Test
    fun `should attach process`() {
        // given
        val file = scriptFile(250)
        var stateAfterAttach: ProcessState? = null

        // when
        shell {
            val script = file.process()
            detach(script)
            delay(50)
            fg()
            stateAfterAttach = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.TERMINATED, stateAfterAttach)
    }

    @Test
    fun `should detach multiple processes`() {
        // given
        val file = scriptFile(500)
        var stateAfterAttach: ProcessState? = null

        // when
        shell {
            val script = file.process()
            val detached = detach(script.copy(), script.copy(), script.copy())
            delay(5)
            join(*detached.toTypedArray())
            stateAfterAttach = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.TERMINATED, stateAfterAttach)
    }

    @Test
    fun `should crate a daemon process`() {
        // given
        val file = scriptFile(500)
        var stateAfterCall: ProcessState? = null

        // when
        shell {
            val script = systemProcess { cmd = "./${file.name}" }
            daemon(script)
            delay(50)
            stateAfterCall = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.RUNNING, stateAfterCall)
    }

    @Test
    fun `should crate a daemon process with extension function`() {
        // given
        val file = scriptFile(500)
        var stateAfterCall: ProcessState? = null

        // when
        shell {
            "./${file.name}"(ExecutionMode.DAEMON)
            delay(50)
            stateAfterCall = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.RUNNING, stateAfterCall)
    }

    @Test
    fun `should list daemon processes`() {
        // given
        val file = scriptFile(5)

        // when
        shell {
            val p1 = "./${file.name}".process()
            val p2 = "./${file.name}".process()
            val p3 = "./${file.name}".process()
            daemon(p1, p2, p3)
            delay(100)

            // then
            assertIterableEquals(listOf(p1.process, p2.process, p3.process), daemons)
        }
    }

    @Test
    fun `should await running process`() {
        // given
        val file = scriptFile(250)
        var stateAfterAttach: ProcessState? = null

        // when
        shell {
            val script = systemProcess { cmd = "./${file.name}" }
            detach(script)
            delay(50)
            script.process.join()
            stateAfterAttach = processes.first().pcb.state
        }

        // then
        assertEquals(ProcessState.TERMINATED, stateAfterAttach)
    }

    @Test
    fun `should kill running process`() {
        // given
        val n = 1_000
        val scriptCode = scriptFile(n)

        var beforeKill: ProcessState? = null
        var afterKill: ProcessState? = null

        // when
        shell {
            val script = systemProcess { cmd = "./${scriptCode.name}" }
            detach(script)

            beforeKill = script.process.pcb.state
            script.process.kill()
            afterKill =script.process.pcb.state
        }

        // then
        assertEquals(ProcessState.RUNNING, beforeKill)
        assertEquals(ProcessState.TERMINATED, afterKill)
    }

    @Test
    fun `should await all running processes`() {
        // given
        val n = 150
        val scriptCode = scriptFile(n)

        val states = mutableListOf<ProcessState>()

        // when
        shell {
            "./${scriptCode.name}"()
            "./${scriptCode.name}"()
            "./${scriptCode.name}"()

            joinAll()
            states.addAll(processes.map { it.pcb.state })
        }

        // then
        states.forEach { assertEquals(ProcessState.TERMINATED, it) }
    }

    @Test
    fun `should kill all running processes`() {
        // given
        val n = 1_000
        val scriptCode = scriptFile(n)

        val states = mutableListOf<ProcessState>()

        // when
        shell {
            "./${scriptCode.name}"()
            "./${scriptCode.name}"()
            "./${scriptCode.name}"()

            killAll()
            states.addAll(processes.map { it.pcb.state })
        }

        // then
        states.forEach { assertEquals(ProcessState.TERMINATED, it) }
    }

    @Test
    fun `should consume long line`() {
        // given
        val line = StringBuilder().let { b ->
            repeat(2048) { b.append("a") }
            b.toString()
        }

        val code = "echo $line"
        val file = testFile(content = code)

        // when
        shell {
            "chmod +x ${file.name}"()

            val echo = systemProcess { cmd = "./${file.name}" }
            val cat = systemProcess { cmd = "cat" }
            pipeline { echo pipe cat pipe storeResult }
        }

        // then
        assertEquals(line.plus('\n'), readResult())
    }
}
