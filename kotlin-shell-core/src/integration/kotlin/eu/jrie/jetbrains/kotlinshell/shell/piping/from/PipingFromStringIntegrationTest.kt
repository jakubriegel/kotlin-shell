package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipingBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.io.core.BytePacketBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.OutputStream

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class PipingFromStringIntegrationTest : PipingBaseIntegrationTest() {

    private val string = content
    

    @Test
    fun `should start pipeline from string to process`() {
        // when
        shell {
            pipeline { string pipe "cat".process() pipe storeResult }
        }

        // then
        assertEquals("$content\n", readResult())
    }

    @Test
    fun `should start pipeline from string to lambda`() {
        // when
        shell {
            pipeline { string pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from string to channel`() {
        // given
        val channel: ProcessChannel = Channel(16)

        // when
        shell {
            pipeline { string pipe channel }
        }

        // then
        assertContent(channel.read())
    }

    @Test
    fun `should start pipeline from string to packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        shell {
            pipeline { string pipe builder }
        }

        // then
        assertContent(builder.build().readText())
    }

    @Test
    fun `should start pipeline from string to stream`() {
        // given
        val result = mutableListOf<Char>()
        val outStream = object : OutputStream() {
            override fun write(b: Int) { result.add(b.toChar()) }
        }

        // when
        shell {
            pipeline { string pipe outStream }
        }

        // then
        assertContent(result.joinToString(separator = ""))
    }

    @Test
    fun `should start pipeline from string to file`() {
        // given
        val endFile = testFile()

        // when
        shell {
            pipeline { string pipe endFile }
        }

        // then
        assertContent(endFile.readText())
    }

    @Test
    fun `should start pipeline from string to file append`() {
        // given
        val fileContent = "def"
        val file = testFile(content = fileContent)

        // when
        shell {
            pipeline { string pipeAppend file }
        }

        // then
        assertEquals(fileContent.plus(content), file.readText())
    }


    @Test
    fun `should start pipeline from string to string builder`() {
        // given
        val builder = StringBuilder()

        // when
        shell {
            pipeline { string pipe builder }
        }

        // then
        assertContent(builder.toString())
    }

    @Test
    fun `should start pipeline from string with java like api`() {
        // when
        shell {
            pipeline {
                from(string.byteInputStream())
                    .throughLambda { storeResult(it) }
            }.join()
        }

        // then
        assertContent()
    }
}
