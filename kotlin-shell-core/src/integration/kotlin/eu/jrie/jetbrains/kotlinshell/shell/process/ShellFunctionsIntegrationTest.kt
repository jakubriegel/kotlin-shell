package eu.jrie.jetbrains.kotlinshell.shell.process

import eu.jrie.jetbrains.kotlinshell.ProcessBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.PrintStream

// TODO: rewrite after implementing terminated processes management
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class ShellFunctionsIntegrationTest : ProcessBaseIntegrationTest() {

    private val n = 10
    private lateinit var scriptCode: File
    private lateinit var outFile: File

    @BeforeEach
    fun createScript() {
        scriptCode = scriptFile(n)
    }

    @BeforeEach
    fun redirectOut() {
        outFile = testFile("console")
        System.setOut(PrintStream(outFile))
    }

    @Test
    fun `should show processes data`() {
        // given
        val psHeaderRegex = Regex("PID\\s+TIME\\s+CMD\\s*")
        val psProcessRegex = Regex("[\\d]+\\s+\\d\\d:\\d\\d:\\d\\d\\s(.+/)*[^/]+\\s(\\w=\\w)*")

        // when
        shell {
            "ls"()
            "./${scriptCode.name}"()
            pipeline { "echo hello".process() pipe nullout }

            detach("ls".process())
            detach("./${scriptCode.name}".process())
            detach { "echo hello".process() pipe nullout }

            joinAll()
            outFile.writeText("") // clear the output

            ps()
        }

        // then
        val result = outFile.withoutLogs().lines()

        assertRegex(psHeaderRegex, result.first())
        result
            .subList(1, result.lastIndex)
            .apply { assertEquals(6, size) }
            .forEach { assertRegex(psProcessRegex, it) }
    }

    @Test
    fun `should show detached jobs data`() {
        // given
        val jobRegex = Regex("\\[\\d+]\\s\\[.+]")

        // when
        shell {
            "ls"()
            "./${scriptCode.name}"()
            pipeline { "echo hello".process() pipe nullout }

            detach("ls".process())
            detach("./${scriptCode.name}".process())
            detach { "echo hello".process() pipe nullout }

            joinAll()
            outFile.writeText("") // clear the output

            jobs()
        }

        // then
        val result = outFile.withoutLogs().lines()
        result
            .subList(0, result.lastIndex)
            .onEach { System.err.println(it) }
            .apply { assertEquals(3, size) }
            .forEach { assertRegex(jobRegex, it) }
    }
}
