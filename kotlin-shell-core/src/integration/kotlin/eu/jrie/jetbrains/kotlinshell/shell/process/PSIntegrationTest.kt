package eu.jrie.jetbrains.kotlinshell.shell.process

import eu.jrie.jetbrains.kotlinshell.ProcessBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import java.io.PrintStream

@ExperimentalCoroutinesApi
class PSIntegrationTest : ProcessBaseIntegrationTest() {

    @Test // TODO: rewrite when piping done
    fun `should show processes data`() {
        // given
        val n = 10
        val scriptCode = scriptFile(n)
        val outFile = testFile("console")
        System.setOut(PrintStream(outFile))

        val psHeaderRegex = Regex("PID\\s+TIME\\s+CMD\\s*")
        val psProcessRegex = Regex("[\\d]+\\s+\\d\\d:\\d\\d:\\d\\d\\s(.+/)*[^/]+\\s(\\w=\\w)*")

        // when
        shell {
            "ls"()
            "./${scriptCode.name}"()
            outFile.writeText("")

            ps()
        }

        // then
        val result = outFile.withoutLogs().lines()

        assertRegex(psHeaderRegex, result.first())
        result
            .subList(1, result.lastIndex)
            .forEachIndexed { i, it ->
                if (i != 0) assertRegex(psProcessRegex, it)
            }
    }
}
