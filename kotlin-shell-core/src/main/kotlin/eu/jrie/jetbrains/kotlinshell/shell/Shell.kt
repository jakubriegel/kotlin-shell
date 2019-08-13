package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.process.NullSendChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipeConfig
import eu.jrie.jetbrains.kotlinshell.shell.piping.ShellPiping
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.io.streams.writePacket
import org.slf4j.LoggerFactory
import java.io.File

@ExperimentalCoroutinesApi
open class Shell protected constructor (
    environment: Map<String, String>,
    variables: Map<String, String>,
    directory: File,
    final override val commander: ProcessCommander,
    final override val SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE: Int,
    final override val PIPELINE_RW_PACKET_SIZE: Long,
    final override val PIPELINE_CHANNEL_BUFFER_SIZE: Int
) : ShellPiping, ShellProcess, ShellUtility {

    final override val nullin: ProcessReceiveChannel = Channel<ProcessChannelUnit>().apply { close() }
    final override val nullout: ProcessSendChannel = NullSendChannel()

    final override val stdin: ProcessReceiveChannel = nullin
    final override val stdout: ProcessSendChannel
    final override val stderr: ProcessSendChannel
    private lateinit var stdoutJob: Job

    final override var environment: Map<String, String> = environment
        private set

    final override var variables: Map<String, String> = variables
        private set

    final override var directory: File = directory
        private set

    private var nextDetachedIndex = 1
        get() = field++

    override val detachedProcesses: List<Pair<Int, Process>>
        get() = detachedProcessesJobs.map { it.first to it.second }

    private val detachedProcessesJobs = mutableListOf<Triple<Int, Process, Job>>()

    override val daemons: List<Process>
        get() = daemonsExecs.map { it.process }

    private val daemonsExecs = mutableListOf<ProcessExecutable>()

    override val detachedPipelines: List<Pair<Int, Pipeline>>
        get() = detachedPipelinesJobs.map { it.first to it.second }

    private val detachedPipelinesJobs = mutableListOf<Triple<Int, Pipeline, Job>>()

    val jobs: ShellCommand get() = command {
        fun line(index: Int, name: String) = "[$index] $name\n"
        StringBuilder().let { b ->
            detachedProcessesJobs.forEach { b.append(line(it.first, "${it.second}")) }
            detachedPipelinesJobs.forEach { b.append(line(it.first, "${it.second}"))}
            b.toString()
        }
    }

    init {
        stdout = initOut()
        stderr = stdout

        initEnv()
    }

    private fun initOut(): ProcessSendChannel = Channel<ProcessChannelUnit>().also {
        stdoutJob = commander.scope.launch /*(Dispatchers.IO)*/ {
            it.consumeEach { p ->
                System.out.writePacket(p)
                System.out.flush()
            }
        }
    }

    private fun initEnv() {
        environment =  systemEnv + environment
        export("PWD" to directory.absolutePath)
    }

    override fun cd(dir: File): File {
        export("OLDPWD" to directory.absolutePath)
        directory = assertDir(dir).absoluteFile
        export("PWD" to directory.absolutePath)
        return directory
    }

    override fun variable(variable: Pair<String, String>) {
        variables = variables.plus(variable)
    }

    override fun export(env: Pair<String, String>) {
        environment = environment.plus(env)
    }

    override fun unset(key: String) {
        variables = variables.without(key)
        environment = environment.without(key)
    }

    private fun Map<String, String>.without(key: String) = toMutableMap()
        .apply { remove(key) }
        .toMap()

    override suspend fun detach(executable: ProcessExecutable) = runProcess(executable) {
        val job = commander.scope.launch { executable.join() }
        detachedProcessesJobs.add(Triple(nextDetachedIndex, it, job))
        logger.debug("detached $it")
    }

    override suspend fun detach(pipeConfig: PipeConfig) = this.pipeConfig()
        .apply { if (!closed) { toDefaultEndChannel(stdout) } }
        .also {
            val job = commander.scope.launch { it.join() }
            detachedPipelinesJobs.add(Triple(nextDetachedIndex, it, job))
            logger.debug("detached $it")
        }

    override suspend fun joinDetached() {
        detachedProcessesJobs.forEach { it.second.join() }
        detachedPipelinesJobs.forEach { it.second.join() }
    }

    /**
     * Attaches job with given index
     */
    suspend fun fg(index: Int = 1) {
        val process = detachedProcessesJobs.find { it.first == index }
        if (process != null) fg(process.second)
        else {
            val pipeline = detachedPipelinesJobs.find { it.first == index }
            if (pipeline != null) fg(pipeline.second)
            else throw NoSuchElementException("no detached job with given index")
        }
    }

    override suspend fun fg(process: Process) {
        detachedProcessesJobs.first { it.second == process }
            .third.join()
    }

    override suspend fun fg(pipeline: Pipeline) {
        detachedPipelinesJobs.first { it.second == pipeline }
            .third.join()
    }

    override suspend fun daemon(executable: ProcessExecutable) = runProcess(executable) {
        daemonsExecs.add(executable)
        logger.debug("started daemon $it")
    }

    private suspend fun runProcess(executable: ProcessExecutable, afterExecute: (Process) -> Unit): Process {
        executable.init()
        executable.exec()
        return executable.process.also(afterExecute)
    }

    override suspend fun finalize() {
        joinDetached()
        stdout.close()
        stdoutJob.join()
        closeOut()
    }

    suspend fun shell(
        vars: Map<String, String> = emptyMap(),
        dir: File = directory,
        script: suspend Shell.() -> Unit
    ) = Shell(
        environment,
        vars,
        dir,
        commander,
        SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE,
        PIPELINE_RW_PACKET_SIZE,
        PIPELINE_CHANNEL_BUFFER_SIZE
    )
        .apply { script() }
        .finalize()

    companion object {

        internal fun build(
            env: Map<String, String>?,
            dir: File?,
            commander: ProcessCommander,
            systemProcessInputStreamBufferSize: Int,
            pipelineRwPacketSize: Long,
            pipelineChannelBufferSize: Int
        ) =
            Shell(
                env ?: emptyMap(),
                emptyMap(),
                assertDir(dir?.absoluteFile ?: currentDir()),
                commander,
                systemProcessInputStreamBufferSize,
                pipelineRwPacketSize,
                pipelineChannelBufferSize
            )

        private fun currentDir(): File {
            val path = System.getProperty("user.dir")
            return File(path)
        }

        private fun assertDir(dir: File) = dir.also { assert(it.isDirectory) }

        internal val logger = LoggerFactory.getLogger(Shell::class.java)
    }
}
