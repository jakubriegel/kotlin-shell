package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipingBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.OutputStream

@ExperimentalCoroutinesApi
class PipingFromLambdaIntegrationTest : PipingBaseIntegrationTest() {

    private val lambda: PipelineContextLambda = { ctx ->
        ctx.stdout.send(buildPacket { writeText(content) })
    }

    @Test
    fun `should start pipeline from lambda to process`() {
        // when
        shell {
            pipeline { lambda pipe "cat".process() pipe storeResult }
        }

        // then
        assertEquals("$content\n", readResult())
    }

    @Test
    fun `should start pipeline from lambda to lambda`() {
        // when
        shell {
            pipeline { lambda pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should start pipeline from lambda to channel`() {
        // given
        val channel: ProcessChannel = Channel(16)

        // when
        shell {
            pipeline { lambda pipe channel }
        }

        // then
        assertContent(channel.read())
    }

    @Test
    fun `should start pipeline from lambda to packet builder`() {
        // given
        val builder = BytePacketBuilder()

        // when
        shell {
            pipeline { lambda pipe builder }
        }

        // then
        assertContent(builder.build().readText())
    }

    @Test
    fun `should start pipeline from lambda to stream`() {
        // given
        val result = mutableListOf<Char>()
        val stream = object : OutputStream() {
            override fun write(b: Int) { result.add(b.toChar()) }
        }

        // when
        shell {
            pipeline { lambda pipe stream }
        }

        // then
        assertContent(result.joinToString(separator = ""))
    }

    @Test
    fun `should start pipeline from lambda to file`() {
        // given
        val file = testFile()

        // when
        shell {
            pipeline { lambda pipe file }
        }

        // then
        assertContent(file.readText())
    }

    @Test
    fun `should start pipeline from lambda to file append`() {
        // given
        val fileContent = "def"
        val file = testFile(content = fileContent)

        // when
        shell {
            pipeline { lambda pipeAppend file }
        }

        // then
        assertEquals(fileContent.plus(content), file.readText())
    }


    @Test
    fun `should start pipeline from lambda to string builder`() {
        // given
        val builder = StringBuilder()

        // when
        shell {
            pipeline { lambda pipe builder }
        }

        // then
        assertContent(builder.toString())
    }

    @Test
    fun `should start pipeline from lambda with java like api`() {
        // when
        shell {
            pipeline {
                from(lambda)
                    .throughLambda { storeResult(it) }
            }.join()
        }

        // then
        assertContent()
    }
}
