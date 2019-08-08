package eu.jrie.jetbrains.kotlinshell.shell

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ShellCreationIntegrationTest {

    @Test
    @ExperimentalCoroutinesApi
    fun `should create Shell with new scope`() = runBlocking {
        shell {
            /* script */
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `should create Shell with given scope`() = runBlocking {
        shell(
            scope = this
        ) {
            /* script */
        }
    }

}
