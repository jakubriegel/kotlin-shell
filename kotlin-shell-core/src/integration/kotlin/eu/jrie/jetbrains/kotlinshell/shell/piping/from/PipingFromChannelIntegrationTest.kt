package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipingBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.OutputStream

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class PipingFromChannelIntegrationTest : PipingBaseIntegrationTest() {

    private val channel: ProcessChannel = Channel(16)

    @BeforeEach
    fun send() {
        runBlocking { channel.send(buildPacket { writeText(content) }) }
        channel.close()
    }

    @Test
    fun `should start pipeline from channel to process`() {
        // when
        shell {
            pipeline { channel pipe "cat".process() pipe storeResult }
        }

        // then
        assertEquals("$content\n", readResult())
    }

    @Test
    fun `should start pipeline from channel to lambda`() {
        // when
        shell {
            pipeline { channel pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from channel to channel`() {
        // given
        val endChannel: ProcessChannel = Channel(16)

        // when
        shell {
            pipeline { channel pipe endChannel }
        }

        // then
        assertContent(endChannel.read())
    }

    @Test
    fun `should start pipeline from channel to packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        shell {
            pipeline { channel pipe builder }
        }

        // then
        assertContent(builder.build().readText())
    }

    @Test
    fun `should start pipeline from channel to stream`() {
        // given
        val result = mutableListOf<Char>()
        val stream = object : OutputStream() {
            override fun write(b: Int) { result.add(b.toChar()) }
        }

        // when
        shell {
            pipeline { channel pipe stream }
        }

        // then
        assertContent(result.joinToString(separator = ""))
    }

    @Test
    fun `should start pipeline from channel to file`() {
        // given
        val file = testFile()

        // when
        shell {
            pipeline { channel pipe file }
        }

        // then
        assertContent(file.readText())
    }

    @Test
    fun `should start pipeline from channel to file append`() {
        // given
        val fileContent = "def"
        val file = testFile(content = fileContent)

        // when
        shell {
            pipeline { channel pipeAppend file }
        }

        // then
        assertEquals(fileContent.plus(content), file.readText())
    }


    @Test
    fun `should start pipeline from channel to string builder`() {
        // given
        val builder = StringBuilder()

        // when
        shell {
            pipeline { channel pipe builder }
        }

        // then
        assertContent(builder.toString())
    }

    @Test
    fun `should start pipeline from channel with java like api`() {
        // when
        shell {
            pipeline {
                from(channel)
                    .throughLambda { storeResult(it) }
            }.join()
        }

        // then
        assertContent()
    }
}
