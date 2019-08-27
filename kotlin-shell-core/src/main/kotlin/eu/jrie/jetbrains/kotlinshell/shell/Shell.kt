package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.process.NullSendChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.DEFAULT_PIPELINE_RW_PACKET_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.PIPELINE_CHANNEL_BUFFER_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.PIPELINE_RW_PACKET_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipelineConfig
import eu.jrie.jetbrains.kotlinshell.shell.piping.ShellPiping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File

@ExperimentalCoroutinesApi
open class Shell protected constructor (
    environment: Map<String, String>,
    variables: Map<String, String>,
    directory: File,
    final override val scope: CoroutineScope,
    final override val commander: ProcessCommander,
    final override val stdout: ProcessSendChannel,
    final override val stderr: ProcessSendChannel
) : ShellPiping, ShellProcess, ShellUtility {

    protected constructor (
        environment: Map<String, String>,
        variables: Map<String, String>,
        directory: File,
        scope: CoroutineScope,
        stdout: ProcessSendChannel,
        stderr: ProcessSendChannel
    ) : this(
        environment, variables, directory,
        scope, ProcessCommander(scope),
        stdout, stderr
    )

    final override val nullin: ProcessReceiveChannel = Channel<ProcessChannelUnit>().apply { close() }
    final override val nullout: ProcessSendChannel = NullSendChannel()

    final override val stdin: ProcessReceiveChannel = nullin

    private lateinit var stdoutJob: Job

    final override var environment: Map<String, String> = environment
        private set

    final override var variables: Map<String, String> = variables
        private set

    private val readOnlyEnvironment: MutableMap<String, String> = mutableMapOf()

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

    /**
     * List of detached jobs
     *
     * @see detachedPipelines
     * @see detachedProcesses
     */
    val jobs: ShellCommand get() = command {
        fun line(index: Int, name: String) = "[$index] $name\n"
        StringBuilder().let { b ->
            detachedProcessesJobs.forEach { b.append(line(it.first, "${it.second}")) }
            detachedPipelinesJobs.forEach { b.append(line(it.first, "${it.second}"))}
            b.toString()
        }
    }

    init {
        initEnv()
    }

    private fun initEnv() {
        environment =  systemEnv + environment

        export("PWD" to directory.absolutePath)

        exportIfNotPresent(SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE to "$DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE")
        exportIfNotPresent(PIPELINE_RW_PACKET_SIZE to "$DEFAULT_PIPELINE_RW_PACKET_SIZE")
        exportIfNotPresent(PIPELINE_CHANNEL_BUFFER_SIZE to "$DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE")
    }

    private fun exportIfNotPresent(variable: Pair<String, String>) {
        if (!environment.containsKey(variable.first)) export(variable)
    }

    override fun cd(dir: File): File {
        export("OLDPWD" to directory.absolutePath)
        directory = assertDir(dir).absoluteFile
        export("PWD" to directory.absolutePath)
        return directory
    }

    override fun variable(variable: Pair<String, String>) {
        if (readOnlyEnvironment.containsKey(variable.first)) throw Exception("read-only variable: ${variable.first}")
        else {
            if (environment.containsKey(variable.first)) export(variable)
            else variables = variables.plus(variable)
        }
    }

    override fun Readonly.variable(variable: Pair<String, String>) {
        this@Shell.variable(variable)
        readOnlyEnvironment[variable.first] = variable.second
    }

    override fun export(env: Pair<String, String>) {
        if (readOnlyEnvironment.containsKey(env.first)) println("readonly variable")
        else {
            variables = variables.minus(env.first)
            environment = environment.plus(env)
        }
    }

    override fun Readonly.export(env: Pair<String, String>) {
        this@Shell.export(env)
        readOnlyEnvironment[env.first] = env.second
    }

    override fun unset(key: String) {
        variables = variables.minus(key)
        environment = environment.minus(key)
    }

    override suspend fun detach(executable: ProcessExecutable) = runProcess(executable) {
        val job = scope.launch { executable.join() }
        detachedProcessesJobs.add(Triple(nextDetachedIndex, it, job))
        logger.debug("detached $it")
    }

    override suspend fun detach(pipelineConfig: PipelineConfig) = this.pipelineConfig()
        .apply { if (!closed) { toDefaultEndChannel(stdout) } }
        .also {
            val job = scope.launch { it.join() }
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
        finalizeDetached()
        stdout.close()
        stdoutJob.join()
        closeOut()
    }

    private suspend fun finalizeDetached() {
        joinDetached()
    }

    suspend fun shell(
        vars: Map<String, String> = emptyMap(),
        dir: File = directory,
        script: suspend Shell.() -> Unit
    ) = Shell(
        environment,
        vars,
        dir,
        scope,
        stdout,
        stderr
    )
        .also { it.readOnlyEnvironment.putAll(readOnlyEnvironment) }
        .apply { script() }
        .finalizeDetached()

    companion object {

        protected fun build(
            environment: Map<String, String>,
            variables: Map<String, String>,
            directory: File,
            scope: CoroutineScope
        ): Shell {
            val stdout = initOut(scope)
            return Shell(
                environment, variables, directory,
                scope, stdout.first, stdout.first
            ).apply { stdoutJob = stdout.second }
        }

        internal fun build(
            env: Map<String, String>?,
            dir: File?,
            scope: CoroutineScope
        ) =
            build(
                env ?: emptyMap(),
                emptyMap(),
                assertDir(dir?.absoluteFile ?: currentDir()),
                scope
            )

        private fun currentDir(): File {
            val path = System.getProperty("user.dir")
            return File(path)
        }

        private fun assertDir(dir: File) = dir.also { assert(it.isDirectory) }

        internal val logger = LoggerFactory.getLogger(Shell::class.java)
    }
}
