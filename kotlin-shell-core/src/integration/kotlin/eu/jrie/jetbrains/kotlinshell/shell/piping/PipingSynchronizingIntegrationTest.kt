package eu.jrie.jetbrains.kotlinshell.shell.piping

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class PipingSynchronizingIntegrationTest : PipingBaseIntegrationTest() {

    @Test
    fun `should suspending execute pipeline`() {
        // given
        val statesAfterExecution = mutableListOf<String>()

        // when
        shell {
            val pipeline = pipeline { "echo abc".process() pipe "grep abc".process() }
            pipeline.processes.forEach { statesAfterExecution.add(it.pcb.state.name) }
        }

        // then
        statesAfterExecution.forEach { assertEquals(ProcessState.TERMINATED.name, it) }
    }

    @Test
    fun `should detach pipeline`() {
        // given
        val statesAfterDetach = mutableListOf<String>()

        // when
        shell {
            val lambda = contextLambda { ctx ->
                repeat(50) { ctx.stdout.send(packet("$it\n")) }
            }

            val pipeline = detach { lambda pipe "grep 29".process() }

            delay(5)
            pipeline.processes.forEach { statesAfterDetach.add(it.pcb.state.name) }
        }

        // then
        statesAfterDetach.forEach { assertNotEquals(ProcessState.TERMINATED.name, it) }
    }


    @Test
    fun `should await pipeline`() {
        // given
        val statesAfterDetach = mutableListOf<String>()
        val statesAfterAwait = mutableListOf<String>()

        // when
        shell {
            val lambda = contextLambda { ctx ->
                repeat(150) { ctx.stdout.send(packet("$it\n")) }
            }

            val pipeline = detach { lambda pipe "grep 99".process() }

            delay(5)
            pipeline.processes.forEach { statesAfterDetach.add(it.pcb.state.name) }

            pipeline.join()
            pipeline.processes.forEach { statesAfterAwait.add(it.pcb.state.name) }
        }

        // then
        statesAfterDetach.forEach { assertNotEquals(ProcessState.TERMINATED.name, it) }
        statesAfterAwait.forEach { assertEquals(ProcessState.TERMINATED.name, it) }
    }

    @Test
    fun `should kill pipeline`() {
        // given
        val statesAfterDetach = mutableListOf<String>()
        val statesAfterKill = mutableListOf<String>()

        // when
        shell {
            val lambda = contextLambda { ctx ->
                repeat(150) { ctx.stdout.send(packet("$it\n")) }
            }

            val pipeline = detach { lambda pipe "grep 99".process() }

            delay(5)
            pipeline.processes.forEach { statesAfterDetach.add(it.pcb.state.name) }

            pipeline.kill()
            pipeline.processes.forEach { statesAfterKill.add(it.pcb.state.name) }
        }

        // then
        statesAfterDetach.forEach { assertNotEquals(ProcessState.TERMINATED.name, it) }
        statesAfterKill.forEach { assertEquals(ProcessState.TERMINATED.name, it) }
    }
}
