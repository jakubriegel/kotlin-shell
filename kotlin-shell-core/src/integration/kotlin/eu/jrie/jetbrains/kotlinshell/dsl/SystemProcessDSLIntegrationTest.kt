package eu.jrie.jetbrains.kotlinshell.dsl

import eu.jrie.jetbrains.kotlinshell.BaseIntegrationTest
import eu.jrie.jetbrains.kotlinshell.processes.configuration.SystemProcessConfiguration
import eu.jrie.jetbrains.kotlinshell.processes.process.system.SystemProcessBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.annotations.TestOnly
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class SystemProcessDSLIntegrationTest : BaseIntegrationTest() {
    @Test
    fun `should create system process with given command`() {
        // when
        val process = create {
            cmd = command
        }

        // then
        assertEquals(process.command, command)
    }

    @Test
    fun `should create system process with given command and single argument`() {
        // when
        val process = create {
            cmd {
                command withArg argument
            }
        }

        // then
        assertEquals(process.command, command)
        assertIterableEquals(process.arguments, listOf(argument))
    }

    @Test
    fun `should create system process with given command and arguments`() {
        // when
        val process = create {
            cmd {
                command withArgs arguments
            }
        }

        // then
        assertEquals(process.command, command)
        assertIterableEquals(process.arguments, arguments)
    }

    @TestOnly
    private fun create(config: SystemProcessConfiguration.() -> Unit): SystemProcessBuilder {
        var processBuilder: SystemProcessBuilder? = null
        shell {
            processBuilder = systemBuilder(config) as SystemProcessBuilder
        }
        return processBuilder ?: throw NullPointerException("process builder did not initialized correctly")
    }
}
