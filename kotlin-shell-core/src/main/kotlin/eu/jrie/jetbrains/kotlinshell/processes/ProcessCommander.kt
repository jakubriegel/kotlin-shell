package eu.jrie.jetbrains.kotlinshell.processes

import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import eu.jrie.jetbrains.kotlinshell.processes.process.system.SystemProcess
import eu.jrie.jetbrains.kotlinshell.processes.process.system.SystemProcessBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

@ExperimentalCoroutinesApi
class ProcessCommander internal constructor (
    private val scope: CoroutineScope
) {

    internal val processes = mutableSetOf<Process>()

    internal fun createProcess(builder: ProcessBuilder): Process {
        return builder
            .withVirtualPID(virtualPID())
            .withScope(scope)
            .build()
            .also { processes.add(it) }
    }

    internal suspend fun startProcess(process: Process) {
        process.start()
        logger.debug("started $process")
    }

    internal suspend fun awaitProcess(process: Process, timeout: Long = 0) {
        logger.debug("awaiting process $process")
        if (!processes.contains(process)) throw Exception("unknown process")
        process.await(timeout)
        logger.debug("awaited process $process")
    }

    internal suspend fun awaitAll() {
        logger.debug("awaiting all processes")
        processes.forEach { awaitProcess(it) }
        logger.debug("all processes awaited")
    }

    internal suspend fun killProcess(process: Process) {
        if (!processes.contains(process)) throw Exception("unknown process")
//        try {
//            process.kill()
//        } catch (e: Exception) {
                val p = createProcess(
                    SystemProcessBuilder("kill", listOf("${(process as SystemProcess).pcb.systemPID}"), 1)
                        .withStdin(Channel())
                        .withStderr(Channel())
                        .withStdout(Channel())
                )
                startProcess(p)
                awaitProcess(p)
//        }
    }

    internal suspend fun killAll() {
        logger.debug("killing all processes")
        processes.forEach { killProcess(it) }
        logger.debug("all processes killed")
    }

    internal fun status() = processes.joinToString (
        separator = "\n",
        prefix = "PID\tTIME\t    CMD\n",
        postfix = "\n"
    ) { it.status }

    companion object {
        private var nextVirtualPID = 1
        private fun virtualPID() = nextVirtualPID++

        private val logger = LoggerFactory.getLogger(ProcessCommander::class.java)
    }
}
