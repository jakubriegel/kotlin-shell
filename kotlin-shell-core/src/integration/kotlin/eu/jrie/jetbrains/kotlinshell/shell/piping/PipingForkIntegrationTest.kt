package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.PrintStream

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class PipingForkIntegrationTest : PipingBaseIntegrationTest() {

    private val n = 5
    private val pattern = "2"

    private lateinit var script: File
    private val lambda: PipelineContextLambda = { ctx ->
       val stdout = buildPacket { writeText(scriptStdOut(n)) }
        ctx.stdout.send(stdout)
        val stderr = buildPacket { writeText(scriptStdErr(n)) }
        ctx.stderr.send(stderr)
    }


    @BeforeEach
    fun initScript() {
        script = scriptFile(n)
    }


    @Test
    fun `should fork stderr of process`() {
        // when
        shell {
            val process = script.process() forkErr { it pipe storeResult }
            process()
        }

        // then
        assertEquals(scriptStdErr(n), readResult())
    }

    @Test
    fun `should pipe forked process stderr`() {
        // when
        shell {
            val grep = "grep $pattern".process()
            val process = script.process() forkErr { it pipe grep pipe storeResult }
            process()
        }

        // then
        assertEquals(scriptStdErr(n).grep(pattern), readResult())
    }

    @Test
    fun `should pipe stdout correctly after forking from process`() {
        // when
        shell {
            val grep = "grep $pattern".process()
            pipeline { (script.process() forkErr { it pipe nullout }) pipe grep pipe storeResult }
        }

        // then
        assertEquals(scriptStdOut(n).grep(pattern), readResult())
    }

    @Test
    fun `should fork stderr of lambda`() {
        // when
        shell {
            val forkedLambda = lambda forkErr { it pipe storeResult }
            pipeline { forkedLambda pipe nullout }
        }

        // then
        assertEquals(scriptStdErr(n), readResult())
    }

    @Test
    fun `should pipe forked lambda stderr`() {
        // when
        shell {
            val grep = "grep $pattern".process()
            val forkedLambda = lambda forkErr { it pipe grep pipe storeResult }
            pipeline { forkedLambda pipe nullout }
        }

        // then
        assertEquals(scriptStdErr(n).grep(pattern), readResult())
    }

    @Test
    fun `should pipe stdout correctly after forking from lambda`() {
        // when
        shell {
            val grep = "grep $pattern".process()
            pipeline { (lambda forkErr { it pipe nullout }) pipe grep pipe storeResult }
        }

        // then
        assertEquals(scriptStdOut(n).grep(pattern), readResult())
    }

    @Test
    fun `should pipe stdout to null`() {
        // given
        val outFile = redirectOut()

        // when
        shell {
            pipeline { (script.process() forkErr { it pipe storeResult }) pipe nullout }
        }

        // then
        assertEquals("", outFile.withoutLogs())
    }

    @Test
    fun `should pipe stderr to null`() {
        // given
        val outFile = redirectOut()

        // when
        shell {
            pipeline { (script.process() forkErr nullout) pipe storeResult }
        }

        // then
        assertEquals("", outFile.withoutLogs())
    }

    @Test
    fun `should pipe stdout and stderr to null`() {
        // given
        val outFile = redirectOut()

        // when
        shell {
            pipeline { (script.process() forkErr nullout) pipe nullout }
        }

        // then
        assertEquals("", outFile.withoutLogs())
    }

    private fun redirectOut() = testFile("console").also {
        System.setOut(PrintStream(it))
    }
}
