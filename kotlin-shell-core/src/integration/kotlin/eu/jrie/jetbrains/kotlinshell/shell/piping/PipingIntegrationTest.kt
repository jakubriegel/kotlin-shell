package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.PrintStream

@ExperimentalCoroutinesApi
class PipingIntegrationTest : PipingBaseIntegrationTest() {

    @Test
    fun `should pipe "echo abc | cat | lambda"`() {
        // when
        shell {
            val echo = systemProcess {
                cmd { "echo" withArg content }
            }
            val cat = systemProcess { cmd = "cat" }

            pipeline { echo pipe cat pipe storeResult }
        }
        // then
        assertEquals("$content\n", readResult())
    }

    @Test
    fun `should pipe file to "grep "Llorem""`() {
        // given
        val file = testFile(content = LOREM_IPSUM)
        val pattern = "[Ll]orem"

        // when
        shell {
            val grep = systemProcess {
                cmd { "grep" withArg pattern }
            }

            pipeline { file pipe grep pipe storeResult }
        }

        // then
        assertEquals(LOREM_IPSUM.grep(pattern), readResult())
    }

    @Test
    fun `should pipe "file to grep "Llorem" | wc -m"`() {
        // given
        val file = testFile(content = LOREM_IPSUM)
        val pattern = "[Ll]orem"

        // when
        shell {
            val grep = systemProcess {
                cmd { "grep" withArg pattern }
            }

            val wc = systemProcess {
                cmd { "wc" withArg "-m" }
            }

            pipeline { file pipe grep pipe wc pipe storeResult }
        }

        // then
        val expected = LOREM_IPSUM.grep(pattern).count()
        assertRegex(Regex("[\n\t\r ]+$expected[\n\t\r ]+"), readResult())
    }

    @Test
    fun `should pipe "file to grep "Llorem" | wc --chars to file"`() {
        // given
        val file = testFile(content = LOREM_IPSUM)
        val resultFile = testFile("result")
        val pattern = "[Ll]orem"

        // when
        shell {
            val grep = systemProcess {
                cmd { "grep" withArg pattern }
            }

            val wc = systemProcess {
                cmd { "wc" withArg "-m" }
            }

            file pipe grep pipe wc pipe resultFile
        }

        // then
        val expected = LOREM_IPSUM.grep(pattern).count()
        assertRegex(Regex("[\n\t\r ]+$expected[\n\t\r ]+"), resultFile.readText())
    }

    @Test
    fun `should pipe to console`() {
        // given
        val outFile = testFile("console")
        System.setOut(PrintStream(outFile))

        // when
        shell {
            val echo = systemProcess {
                cmd { "echo" withArg content }
            }

            echo pipe stdout join it
        }

        // then
        assertRegex(Regex(content), outFile.readText())
    }


    @Test
    fun `should pipe to console by default`() {
        // given
        val outFile = testFile("console")
        System.setOut(PrintStream(outFile))

        // when
        shell {
            val echo = systemProcess {
                cmd { "echo" withArg content }
            }

            pipeline { echo pipe "cat".process() }
        }

        // then
        assertRegex(Regex(content), outFile.readText())
    }

    @Test
    fun `should pipe long stream`() {
        // given
        val n = 100_000
        val file = scriptFile(n)
        val pattern = "2"

        // when
        shell {
            val script = systemProcess { cmd = "./${file.name}" }
            val cat = systemProcess { cmd = "cat" }
            val grep = systemProcess { cmd { "grep" withArg pattern } }

            (script forkErr nullout) pipe cat pipe grep pipe storeResult
        }

        // then
        assertEquals(scriptStdOut(n).grep(pattern), readResult())
    }

    @Test
    fun `should pipe infinite fibonacci numbers`() {
        shell {
            // given
            val n = 15

            val result: ProcessChannel = Channel(n)

            val buffer: ProcessChannel = Channel<ProcessChannelUnit>(2).apply {
                send(packet("0"))
                send(packet("1"))
            }

            val fibonacci = contextLambda { ctx ->
                repeat(n) {
                    val a = ctx.stdin.receive().readText().toLong()
                    val b = ctx.stdin.receive()
                        .also { p -> ctx.stdout.send(p.copy()) }
                        .readText().toLong()

                    packet("${a+b}").let { next ->
                        ctx.stdout.send(next.copy())
                        result.send(next)
                    }

                }
                result.close()
            }

            // when
            detach { buffer pipe fibonacci pipe buffer }
            pipeline { result pipe stringLambda { "$it, " to "" } pipe storeResult }

            // then
            val expected = "1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, "
            assertEquals(expected, readResult())
        }
    }

    @Test
    fun `should make pipeline with non DSL api`() {
        // when
        shell {
            val echo = systemProcess {
                cmd { "echo" withArg "abc\ndef" }
            }
            val grep = systemProcess {
                cmd { "grep" withArg "c" }
            }

            from(echo)
                .throughProcess(grep)
                .throughProcess(systemProcess { cmd = "cat" })
                .throughLambda { storeResult(it) }
                .join()
        }

        // then
        assertEquals("abc\n", readResult())
    }
}
