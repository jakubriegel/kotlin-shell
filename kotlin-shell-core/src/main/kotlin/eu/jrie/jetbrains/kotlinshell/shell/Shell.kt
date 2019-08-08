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

    override val detached: List<Process>
        get() = detachedJobs.map { it.first }

    private val detachedJobs = mutableListOf<Pair<Process, Job>>()

    override val daemons: List<Process>
        get() = daemonsExecs.map { it.process }

    private val daemonsExecs = mutableListOf<ProcessExecutable>()

    override val pipelines: List<Pipeline>
        get() = detachedPipelines.toList()

    private val detachedPipelines = mutableListOf<Pipeline>()

    init {
        stdout = initOut()
        stderr = stdout

        initEnv()
    }

    private fun initOut(): ProcessSendChannel = Channel<ProcessChannelUnit>(PIPELINE_CHANNEL_BUFFER_SIZE).also {
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

    override suspend fun detach(process: ProcessExecutable) {
        process.init()
        process.exec()
        val job = commander.scope.launch { process.join() }
        detachedJobs.add(process.process to job)
    }

    override suspend fun detach(pipeConfig: PipeConfig) = this.pipeConfig()
        .apply { if (!closed) { toDefaultEndChannel(stdout) } }
        .also {
            detachedPipelines.add(it)
        }

    override suspend fun joinDetached() {
        detachedJobs.forEach { it.second.join() }
        detachedPipelines.forEach { it.join() }
    }

    override suspend fun fg(process: Process) {
        detachedJobs.first { it.first == process }
            .apply {
                commander.awaitProcess(first)
                second.join()
            }
    }

    override suspend fun daemon(executable: ProcessExecutable) {
        executable.init()
        executable.exec()
        daemonsExecs.add(executable)
        logger.debug("started daemon ${executable.process}")
    }

    override suspend fun finalize() {
        joinDetached()
        closeOut()
    }

    override fun exec(block: Shell.() -> String) = ShellExecutable(this, block)

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
