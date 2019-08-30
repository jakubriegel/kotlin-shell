package eu.jrie.jetbrains.kotlinshell.processes.process

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

@ExperimentalCoroutinesApi
abstract class Process @ExperimentalCoroutinesApi
protected constructor (
    val vPID: Int,
    val environment: Map<String, String>,
    val directory: File,
    protected val stdin: ProcessReceiveChannel,
    protected val stdout: ProcessSendChannel,
    protected val stderr: ProcessSendChannel,
    protected val scope: CoroutineScope
) {

    private val ioJobs = mutableListOf<Job>()

    abstract val pcb: PCB

    open val name: String
        get() = "[${this::class.simpleName} $vPID]"

    final override fun toString() = name

    val status: String
        get() = "$vPID\t${since(pcb.startTime)}    $statusCmd\t$statusOther state=${pcb.state}"

    protected abstract val statusCmd: String
    protected abstract val statusOther: String

    internal suspend fun start() {
        if (pcb.state != ProcessState.READY) {
            throw Exception("only READY process can be started")
        }
        else {
            pcb.state = ProcessState.RUNNING
            execute()
            pcb
        }
    }

    protected abstract suspend fun execute()

    fun isAlive() = pcb.state == ProcessState.RUNNING || isRunning()

    abstract fun isRunning(): Boolean

    lateinit var awaitJob: Job

    internal suspend fun await(timeout: Long) {
        if (isAlive()) {
            if (!::awaitJob.isInitialized) {
                awaitJob = scope.launch {
                    expect(timeout)
                    finalize()
                }
            }
            awaitJob.join()

        }
    }

    protected abstract suspend fun expect(timeout: Long)

    internal suspend fun kill() {
        destroy()
        finalize()
        logger.debug("killed $name")
    }

    protected abstract fun destroy()

    private suspend fun finalize() {
        ioJobs.forEach { it.join() }
        pcb.endTime = Instant.now()
        pcb.state = ProcessState.TERMINATED
    }

    companion object {
        @JvmStatic
        internal val logger: Logger = LoggerFactory.getLogger(Process::class.java)

        private val formatter = SimpleDateFormat("HH:mm:ss")

        private fun since(instant: Instant?): String {
            return if (instant == null) "n/a"
            else formatter.format(Date.from(instant))
        }
    }
}
