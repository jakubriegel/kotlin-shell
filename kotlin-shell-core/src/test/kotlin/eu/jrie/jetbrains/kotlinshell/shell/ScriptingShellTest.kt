package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipeConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalCoroutinesApi
class ScriptingShellTest {

    private val shellSpy = spyk(
        Shell.build(emptyMap(), File(System.getProperty("user.home")), ProcessCommander(GlobalScope), 16, 16, 16)
    ) {
        coEvery { systemProcess(ofType(String::class)) } returns mockk()
        coEvery { detach(*anyVararg()) } returns mockk()
        coEvery { fg(ofType(Int::class)) } returns mockk()
        coEvery { join(*anyVararg()) } returns mockk()
        coEvery { joinAll() } returns mockk()
        coEvery { kill(*anyVararg()) } returns mockk()
        coEvery { killAll() } returns mockk()
        coEvery { pipeline(ExecutionMode.ATTACHED, any()) } returns mockk()
        coEvery { shell(script = any()) } returns mockk()
    }
    private val scriptingShell = ScriptingShell(GlobalScope, shellSpy)

    @Test
    fun `should map String process to shell`() {
        // given
        val cmd = "echo abc"

        // when
        scriptingShell.apply {
            cmd.process()
        }

        // then
        coVerify (exactly = 1) { shellSpy.systemProcess(cmd) }
    }

    @Test
    fun `should invoke process`() {
        // given
        val executableMock = mockk<ProcessExecutable> {
            coEvery { this@mockk.invoke() } just runs
        }

        // when
        scriptingShell.apply {
            executableMock.run()
        }

        // then
        coVerify (exactly = 1) { executableMock.invoke() }
    }

    @Suppress("RemoveRedundantSpreadOperator")
    @Test
    fun `should map detach to shell`() {
        // given
        val executableMock = mockk<ProcessExecutable>()

        // when
        scriptingShell.detach(executableMock)

        // then
        coVerify (exactly = 1) { shellSpy.detach(*arrayOf(executableMock)) }
    }

    @Test
    fun `should map fg to shell`() {
        // when
        scriptingShell.fg()

        // then
        coVerify (exactly = 1) { shellSpy.fg(1) }
    }

    @Test
    fun `should map join to shell`() {
        // given
        val processMock = mockk<Process>()

        // when
        scriptingShell.join(processMock)

        // then
        coVerify (exactly = 1) { shellSpy.join(processMock) }
    }

    @Test
    fun `should map joinAll to shell`() {
        // when
        scriptingShell.joinAll()

        // then
        coVerify (exactly = 1) { shellSpy.joinAll() }
    }

    @Test
    fun `should map kill to shell`() {
        // given
        val processMock = mockk<Process>()

        // when
        scriptingShell.kill(processMock)

        // then
        coVerify (exactly = 1) { shellSpy.kill(processMock) }
    }

    @Test
    fun `should map killAll to shell`() {
        // when
        scriptingShell.killAll()

        // then
        coVerify (exactly = 1) { shellSpy.killAll() }
    }

    @Test
    fun `should map pipeline to shell`() {
        // given
        val configMock = mockk<PipeConfig>()

        // when
        scriptingShell.pipeline(configMock)

        // then
        coVerify (exactly = 1) { shellSpy.pipeline(ExecutionMode.ATTACHED, configMock) }
    }

    @Test
    fun `should map script to shell`() {
        // given
        val script: ShellScript = {}

        // when
        scriptingShell.shell(script)

        // then
        coVerify (exactly = 1) { shellSpy.shell(script = script) }
    }
}
