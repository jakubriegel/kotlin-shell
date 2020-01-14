package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class PipingThroughIntegrationTest : PipingBaseIntegrationTest() {
    @Test
    fun `should pipe through process`() {
        // when
        shell {
            pipeline { content pipe "cat".process() pipe storeResult }
        }

        // then
        assertContent(readResult().removeSuffix("\n"))
    }

    @Test
    fun `should pipe through lambda`() {
        // given
        val lambda: PipelineContextLambda = { ctx ->
            ctx.stdin.consumeEach { ctx.stdout.send(it) }
        }

        // when
        shell {
            pipeline { content pipe lambda pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should construct and pipe through PipelinePacketLambda`() {
        // when
        shell {
            val lambda = packetLambda { it to emptyPacket() }
            pipeline { content pipe lambda pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should construct and pipe through PipelineByteArrayLambda`() {
        // when
        shell {
            val lambda = byteArrayLambda { it to emptyByteArray() }
            pipeline { content pipe lambda pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should construct and pipe through PipelineStringLambda`() {
        // when
        shell {
            val lambda = stringLambda { it to "" }
            pipeline { content pipe lambda pipe storeResult }
        }

        // then
        assertContent()
    }

    @Test
    fun `should construct and pipe through PipelineStreamLambda`() {
        // when
        shell {
            val lambda = streamLambda { inputStream, outputStream, _ ->
                inputStream.use { input ->
                    outputStream.use { output ->
                        output.write(input.readBytes())
                    }
                }
            }
            pipeline { content pipe lambda pipe storeResult }
        }

        // then
        assertContent()
    }

}
