package eu.jrie.jetbrains.kotlinshell

import eu.jrie.jetbrains.kotlinshell.processes.pipeline.PipelineContextLambda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.io.core.BytePacketBuilder
import org.junit.jupiter.api.BeforeEach
import java.io.File

@ExperimentalCoroutinesApi
abstract class ProcessBaseIntegrationTest : BaseIntegrationTest() {
    private lateinit var resultBuilder: BytePacketBuilder

    protected val storeResult: PipelineContextLambda = { ctx ->
        ctx.stdin.consumeEach { resultBuilder.writePacket(it) }
    }

    @BeforeEach
    fun initResultBuilder() {
        resultBuilder = BytePacketBuilder()
    }

    protected fun readResult() = resultBuilder.build().readText()

    private var printScriptMessage = "hello"
    private val printScriptErrorMessage: String
            get() = "${printScriptMessage}_error"

    protected fun scriptCode(n: Int) =
        "for (( i = 0; i < $n; ++i )); do\n" +
        "    echo $printScriptMessage\$i\n" +
        "    >&2 echo $printScriptErrorMessage\$i\n" +
        "done\n"

    fun scriptFile(n: Int): File {
        val file = testFile("script", scriptCode(n))

        shell {
            val chmod = systemProcess {
                cmd { "chmod" withArgs listOf("+x", file.name) }
            }
            chmod()
        }

        return file
    }

    protected fun scriptStdOut(n: Int) = StringBuilder().let { b ->
        repeat(n) { b.append("$printScriptMessage$it\n") }
        b.toString()
    }

    protected fun scriptStdErr(n: Int) = StringBuilder().let { b ->
        repeat(n) { b.append("$printScriptErrorMessage$it\n") }
        b.toString()
    }

    protected fun scriptOut(n: Int) = StringBuilder().let { b ->
        repeat(n) {
            b.append("$printScriptMessage$it\n")
            b.append("$printScriptErrorMessage$it\n")
        }
        b.toString()
    }

    protected fun File.withoutLogs() = StringBuilder().let { b ->
        readText().lines().forEach {
            if (!it.matches(logLineRegex)) b.append(it.plus('\n'))
        }
        b.removeSuffix("\n").toString()
    }

    protected fun String.grep(pattern: String) = lines()
        .filter { it.matches(Regex(".*$pattern.*")) }
        .joinToString("\n")
        .plus('\n')

    companion object {
        private val logLineRegex = Regex("^\\d\\d:\\d\\d:\\d\\d\\s[A-Z]+\\s.+$")
    }
}
