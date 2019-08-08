package eu.jrie.jetbrains.kotlinshell.shell.process

import eu.jrie.jetbrains.kotlinshell.ProcessBaseIntegrationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.PrintStream

@ExperimentalCoroutinesApi
class ProcessIOIntegrationTest : ProcessBaseIntegrationTest() {

    private lateinit var outFile: File

    private val n = 5
    private lateinit var script: File

    @BeforeEach
    fun redirectSystemOut() {
        outFile = testFile("console")
        System.setOut(PrintStream(outFile))
    }

    @BeforeEach
    fun prepareScript() {
        script = scriptFile(n)
    }

    @Test
    fun `should print stdout and stderr to console when no redirect buffers given`() {
        // when
        shell {
            val process = systemProcess { cmd = "./${script.name}" }
            process()
        }

        // then
//        TODO: synchronize std and err
//        assertEquals(scriptOut(n), outFile.withoutLogs())
        val expected = scriptOut(n).lines().toHashSet()
        val actual = outFile.withoutLogs().lines().toHashSet()
        assertIterableEquals(expected, actual)

    }

    companion object {

        private lateinit var defaultOut: PrintStream

        @BeforeAll
        @JvmStatic
        fun storeSystemOut() {
            defaultOut = System.out
        }

        @AfterAll
        @JvmStatic
        fun resetSystemOut() {
            System.setOut(defaultOut)
        }
    }

}
