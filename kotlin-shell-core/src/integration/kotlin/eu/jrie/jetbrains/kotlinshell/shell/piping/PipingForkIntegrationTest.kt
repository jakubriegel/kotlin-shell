package eu.jrie.jetbrains.kotlinshell.shell.piping

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.PrintStream

@ExperimentalCoroutinesApi
class PipingForkIntegrationTest : PipingBaseIntegrationTest() {

    @Test
    fun `should fork stderr for single process`() {
        // given
        val n = 5
        val code = scriptFile(n)

        // when
        shell {
            val script = systemProcess {
                cmd { cmd = "./${code.name}" }
            }

            pipeline { (script forkErr { it pipe storeResult }) pipe nullout }
        }

        // then
        assertEquals(scriptStdErr(n), readResult())
    }

    @Test
    fun `should pipe forked stderr`() {
        // given
        val n = 5
        val code = scriptFile(n)
        val pattern = "2"

        // when
        shell {
            val script = systemProcess {
                cmd { cmd = "./${code.name}" }
            }

            val grep = systemProcess {
                cmd { "grep" withArg pattern }
            }

            pipeline { (script forkErr { it pipe grep pipe storeResult }) pipe nullout }
        }

        // then
        assertEquals(scriptStdErr(n).grep(pattern), readResult())
    }

    @Test
    fun `should pipe stdout correctly after forking`() {
        // given
        val n = 5
        val code = scriptFile(n)
        val pattern = "2"

        // when
        shell {
            val grep = systemProcess {
                cmd { "grep" withArg pattern }
            }

            val script = systemProcess {
                cmd { cmd = "./${code.name}" }
            }

            pipeline { (script forkErr { it pipe nullout }) pipe grep pipe storeResult }
        }

        // then
        assertEquals(scriptStdOut(n).grep(pattern), readResult())
    }

    @Test
    fun `should pipe stdout to null`() {
        // given
        val outFile = testFile("console")
        System.setOut(PrintStream(outFile))

        val n = 5
        val code = scriptFile(n)

        // when
        shell {
            val script = systemProcess {
                cmd { cmd = "./${code.name}" }
            }

            pipeline { (script forkErr { it pipe storeResult }) pipe nullout }
        }

        // then
        assertEquals("", outFile.withoutLogs())
    }

    @Test
    fun `should pipe stderr to null`() {
        // given
        val outFile = testFile("console")
        System.setOut(PrintStream(outFile))

        val n = 5
        val code = scriptFile(n)

        // when
        shell {
            val script = systemProcess {
                cmd {
                    cmd = "./${code.name}"
                }
            }

            pipeline { (script forkErr nullout) pipe storeResult }
        }

        // then
        assertEquals("", outFile.withoutLogs())
    }

    @Test
    fun `should pipe stdout and stderr to null`() {
        // given
        val outFile = testFile("console")
        System.setOut(PrintStream(outFile))

        val n = 5
        val code = scriptFile(n)

        // when
        shell {
            val script = systemProcess {
                cmd {
                    cmd = "./${code.name}"
                }
            }

            pipeline { (script forkErr nullout) pipe nullout }
        }

        // then
        assertEquals("", outFile.withoutLogs())
    }
}
