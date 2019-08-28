package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipingBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.io.core.BytePacketBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.io.OutputStream

@ExperimentalCoroutinesApi
class PipingFromStreamIntegrationTest : PipingBaseIntegrationTest() {

    private val contentIt = content
        .map { it.toInt() }
        .plus(-1)
        .iterator()
    
    private val stream = object : InputStream() {
        override fun read() = contentIt.next()
    }

    @Test
    fun `should start pipeline from stream to process`() {
        // when
        shell {
            pipeline { stream pipe "cat".process() pipe storeResult }
        }

        // then
        assertEquals("$content\n", readResult())
    }

    @Test
    fun `should start pipeline from stream to lambda`() {
        // when
        shell {
            pipeline { stream pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from stream to channel`() {
        // given
        val channel: ProcessChannel = Channel(16)

        // when
        shell {
            pipeline { stream pipe channel }
        }

        // then
        assertContent(channel.read())
    }

    @Test
    fun `should start pipeline from stream to packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        shell {
            pipeline { stream pipe builder }
        }

        // then
        assertContent(builder.build().readText())
    }

    @Test
    fun `should start pipeline from stream to stream`() {
        // given
        val result = mutableListOf<Char>()
        val outStream = object : OutputStream() {
            override fun write(b: Int) { result.add(b.toChar()) }
        }

        // when
        shell {
            pipeline { stream pipe outStream }
        }

        // then
        assertContent(result.joinToString(separator = ""))
    }

    @Test
    fun `should start pipeline from stream to file`() {
        // given
        val file = testFile()

        // when
        shell {
            pipeline { stream pipe file }
        }

        // then
        assertContent(file.readText())
    }

    @Test
    fun `should start pipeline from stream to file append`() {
        // given
        val fileContent = "def"
        val file = testFile(content = fileContent)

        // when
        shell {
            pipeline { stream pipeAppend file }
        }

        // then
        assertEquals(fileContent.plus(content), file.readText())
    }


    @Test
    fun `should start pipeline from stream to string builder`() {
        // given
        val builder = StringBuilder()

        // when
        shell {
            pipeline { stream pipe builder }
        }

        // then
        assertContent(builder.toString())
    }

    @Test
    fun `should start pipeline from stream with java like api`() {
        // when
        shell {
            pipeline {
                from(stream)
                    .throughLambda { storeResult(it) }
            }.join()
        }

        // then
        assertContent()
    }
}
