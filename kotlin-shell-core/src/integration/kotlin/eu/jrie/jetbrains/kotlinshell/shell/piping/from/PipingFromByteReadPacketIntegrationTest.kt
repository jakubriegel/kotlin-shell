package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipingBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeText
import kotlinx.io.streams.inputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.OutputStream

@ExperimentalCoroutinesApi
class PipingFromByteReadPacketIntegrationTest : PipingBaseIntegrationTest() {

    private val packet = buildPacket { writeText(content) }

    @Test
    fun `should start pipeline from packet to process`() {
        // when
        shell {
            pipeline { packet pipe "cat".process() pipe storeResult }
        }

        // then
        assertEquals("$content\n", readResult())
    }

    @Test
    fun `should start pipeline from packet to lambda`() {
        // when
        shell {
            pipeline { packet pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from packet to channel`() {
        // given
        val channel: ProcessChannel = Channel(16)

        // when
        shell {
            pipeline { packet pipe channel }
        }

        // then
        assertContent(channel.read())
    }

    @Test
    fun `should start pipeline from packet to packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        shell {
            pipeline { packet pipe builder }
        }

        // then
        assertContent(builder.build().readText())
    }

    @Test
    fun `should start pipeline from packet to stream`() {
        // given
        val result = mutableListOf<Char>()
        val stream = object : OutputStream() {
            override fun write(b: Int) { result.add(b.toChar()) }
        }

        // when
        shell {
            pipeline { packet pipe stream }
        }

        // then
        assertContent(result.joinToString(separator = ""))
    }

    @Test
    fun `should start pipeline from packet to file`() {
        // given
        val file = testFile()

        // when
        shell {
            pipeline { packet pipe file }
        }

        // then
        assertContent(file.readText())
    }

    @Test
    fun `should start pipeline from packet to file append`() {
        // given
        val fileContent = "def"
        val file = testFile(content = fileContent)

        // when
        shell {
            pipeline { packet pipeAppend file }
        }

        // then
        assertEquals(fileContent.plus(content), file.readText())
    }

    @Test
    fun `should start pipeline from packet to string builder`() {
        // given
        val builder = StringBuilder()

        // when
        shell {
            pipeline { packet pipe builder }
        }

        // then
        assertContent(builder.toString())
    }

    @Test
    fun `should start pipeline from packet with java like api`() {
        // when
        shell {
            pipeline {
                from(packet.inputStream())
                    .throughLambda { storeResult(it) }
            }.join()
        }

        // then
        assertContent()
    }
}
