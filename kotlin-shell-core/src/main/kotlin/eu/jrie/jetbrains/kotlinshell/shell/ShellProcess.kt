package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.configuration.KtsProcessConfiguration
import eu.jrie.jetbrains.kotlinshell.processes.configuration.ProcessConfiguration
import eu.jrie.jetbrains.kotlinshell.processes.configuration.SystemProcessConfiguration
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessState
import eu.jrie.jetbrains.kotlinshell.shell.ShellBase.Companion.SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

@ExperimentalCoroutinesApi
interface ShellProcess : ShellUtility {

    /**
     * All processes
     */
    val processes: List<Process>
        get() = commander.processes.toList()
    /**
     * List of detached processes
     */
    val detachedProcesses: List<Pair<Int, Process>>

    /**
     * List of daemon processes
     */
    val daemons: List<Process>

    /**
     * Dummy input channel. Behaves like `/dev/null`.
     *
     * @see nullout
     */
    val nullin: ProcessReceiveChannel

    /**
     * Dummy output channel. Behaves like `/dev/null`.
     *
     * @see nullout
     */
    val nullout: ProcessSendChannel

    /**
     * Creates executable system process
     */
    fun systemProcess(config: SystemProcessConfiguration.() -> Unit) = process(systemBuilder(config))

    /**
     * Creates builder for system process
     */
    fun systemBuilder(config: SystemProcessConfiguration.() -> Unit) = SystemProcessConfiguration()
        .apply(config)
        .apply {
            systemProcessInputStreamBufferSize = this@ShellProcess.env(SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE).toInt()
        }
        .configureBuilder()

    /**
     * Creates executable system process from this command line
     */
    fun String.process() = with(split(" ")) {
        when (size) {
            1 -> systemProcess { cmd = first() }
            else -> systemProcess { cmd { first() withArgs subList(1, size) } }
        }
    }

    /**
     * Creates executable system process from [cmd] command line
     */
    fun systemProcess(cmd: String) = cmd.process()

    /**
     * Executes system process from this command line
     */
    suspend operator fun String.invoke(mode: ExecutionMode = ExecutionMode.ATTACHED) = process().invoke(mode)

    /**
     * Creates executable system process from contents of this [File]
     */
    fun File.process(vararg args: String) = when (args.size) {
        0 -> systemProcess { cmd = canonicalPath }
        else -> systemProcess { cmd { canonicalPath withArgs args.toList() } }
    }

    /**
     * Creates executable system process from [file] contents
     */
    fun systemProcess(file: File, vararg arg: String) = file.process(*arg)

    /**
     * Executes system process from from contents of this [File]
     */
    suspend operator fun File.invoke(vararg args: String) = invoke(ExecutionMode.ATTACHED, *args)

    /**
     * Executes system process from from contents of this [File] in given [mode]
     */
    suspend operator fun File.invoke(mode: ExecutionMode, vararg args: String) = process(*args).invoke(mode)

    /**
     * Creates executable KotlinScript process
     */
    fun ktsProcess(config: KtsProcessConfiguration.() -> Unit) = process(ktsBuilder(config))

    /**
     * Creates builder for KotlinScript process
     */
    fun ktsBuilder(config: KtsProcessConfiguration.() -> Unit) = KtsProcessConfiguration()
        .apply(config)
        .configureBuilder()

    /**
     * Creates executable KotlinScript process from given script
     */
    fun String.kts(): ProcessExecutable { TODO("implement kts processes") }

    private fun process(builder: ProcessBuilder) = ProcessExecutable(this, builder)

    private fun ProcessConfiguration.configureBuilder(): ProcessBuilder {
        env(environment.plus(variables))
        dir(directory)
        return builder()
    }

    /**
     * Detaches process from the shell and executes it in the background
     *
     * @return detached [Process]
     */
    suspend fun detach(executable: ProcessExecutable): Process

    /**
     * Detaches processes from the shell and executes it in the background
     *
     * @return list of detached [Process]
     */
    suspend fun detach(vararg process: ProcessExecutable) = process.map { detach(it) }

    /**
     * Joins all detached processes
     */
    suspend fun joinDetached()

    /**
     * Attaches selected [Process]
     */
    suspend fun fg(process: Process)

    suspend fun daemon(executable: ProcessExecutable): Process

    suspend fun daemon(vararg executable: ProcessExecutable) = executable.map { daemon(it) }

    /**
     * Joins the [Process]
     */
    suspend fun Process.join() = commander.awaitProcess(this)

    /**
     * Joins given processes
     */
    suspend fun join(vararg process: Process) = process.forEach { it.join() }

    /**
     * Joins all running processes
     */
    suspend fun joinAll() = commander.awaitAll()

    /**
     * Kill the [Process]
     */
    suspend fun Process.kill() = kill(this)

    /**
     * Kills given processes
     */
    suspend fun kill(vararg process: Process) = process.forEach { killProcess(it) }

    private suspend fun killProcess(process: Process) = commander.killProcess(process)

    /**
     * Kills all running processes
     */
    suspend fun killAll() = commander.killAll()

    /**
     * Starts the [Process] in given [ExecutionMode]
     *
     * @return started [Process]
     */
    suspend operator fun ProcessExecutable.invoke(mode: ExecutionMode = ExecutionMode.ATTACHED): Process {
        when (mode) {
            ExecutionMode.ATTACHED -> this()
            ExecutionMode.DETACHED -> this@ShellProcess.detach(this)
            ExecutionMode.DAEMON -> this@ShellProcess.daemon(this)
        }
        return process
    }

    /**
     * Retrieves all process data
     */
    val ps: ShellCommand get() = command { commander.status() }

    /**
     * Retrieves [Process] by its vPID
     */
    fun List<Process>.byVPID(vPID: Int) = first { it.vPID == vPID }

    /**
     * Retrieves all running processes
     */
    fun List<Process>.running() = filter { it.pcb.state == ProcessState.RUNNING }

    /**
     * Retrieves all terminated processes
     */
    fun List<Process>.terminated() = filter { it.pcb.state == ProcessState.TERMINATED }

}

enum class ExecutionMode {
    /**
     * Runs process in foreground.
     * Will close the process when closing shell
     */
    ATTACHED,

    /**
     * Runs process in the background.
     * Will close the process when closing shell
     */
    DETACHED,

    /**
     * Runs process in the background.
     * Will **not** close the process when closing shell
     */
    DAEMON
}
