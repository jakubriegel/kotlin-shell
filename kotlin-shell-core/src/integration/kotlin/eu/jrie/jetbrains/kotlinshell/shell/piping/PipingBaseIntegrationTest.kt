package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.ProcessBaseIntegrationTest
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import org.jetbrains.annotations.TestOnly
import org.junit.jupiter.api.Assertions

@ExperimentalCoroutinesApi
abstract class PipingBaseIntegrationTest : ProcessBaseIntegrationTest() {
    protected val content = "abc"

    protected fun assertContent() = assertContent(readResult())

    protected open fun assertContent(result: String) {
        Assertions.assertEquals(content, result)
    }

    @TestOnly
    protected fun ProcessChannel.read() = kotlinx.coroutines.runBlocking {
        StringBuilder().let { b ->
            this@read.consumeEach { b.append(it.readText()) }
            b.toString()
        }
    }
}
