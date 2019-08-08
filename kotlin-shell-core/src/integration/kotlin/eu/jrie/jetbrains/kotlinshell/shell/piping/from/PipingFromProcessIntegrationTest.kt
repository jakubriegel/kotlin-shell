package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.shell.Shell
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipingBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.BytePacketBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.OutputStream

@ExperimentalCoroutinesApi
class PipingFromProcessIntegrationTest : PipingBaseIntegrationTest() {

    private val Shell.echo: ProcessExecutable
        get() = "echo $content".process()

    override fun assertContent(result: String) {
        super.assertContent(result.removeSuffix("\n"))
    }

    @Test
    fun `should start pipeline from process to process`() {
        // when
        shell {
            pipeline { echo pipe "cat".process() pipe storeResult }
        }
        
        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from process to lambda`() {
        // when
        shell {
            pipeline { echo pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from process to channel`() {
        // given
        val channel: ProcessChannel = Channel(16)

        // when
        shell {
            pipeline { echo pipe channel }
        }

        // then
        val result = runBlocking {
            StringBuilder().let { b ->
                channel.consumeEach { b.append(it.readText()) }
                b.toString()
            }
        }
        assertContent(result)
    }

    @Test
    fun `should start pipeline from process to packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        shell {
            pipeline { echo pipe builder }
        }

        // then
        assertContent(builder.build().readText())
    }

    @Test
    fun `should start pipeline from process to stream`() {
        // given
        val result = mutableListOf<Char>()
        val stream = object : OutputStream() {
            override fun write(b: Int) { result.add(b.toChar()) }
        }

        // when
        shell {
            pipeline { echo pipe stream }
        }

        // then
        assertContent(result.joinToString(separator = ""))
    }

    @Test
    fun `should start pipeline from process to file`() {
        // given
        val file = testFile()

        // when
        shell {
            pipeline { echo pipe file }
        }

        // then
        assertContent(file.readText())
    }

    @Test
    fun `should start pipeline from process to file append`() {
        // given
        val fileContent = "def"
        val file = testFile(content = fileContent)

        // when
        shell {
            pipeline { echo pipeAppend file }
        }

        // then
        assertEquals(fileContent.plus(content).plus("\n"), file.readText())
    }


    @Test
    fun `should start pipeline from process to string builder`() {
        // given
        val builder = StringBuilder()

        // when
        shell {
            pipeline { echo pipe builder }
        }

        // then
        assertContent(builder.toString())
    }

    @Test
    fun `should start pipeline from process with java like api`() {
        // when
        shell {
            from(echo)
                .throughLambda { storeResult(it) }
                .join()
        }

        // then
        assertContent()
    }
}
