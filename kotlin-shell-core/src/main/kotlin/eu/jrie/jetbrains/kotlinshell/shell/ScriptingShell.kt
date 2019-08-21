package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.execution.Executable
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Executes basic non suspend [Shell] script
 *
 * @see ScriptingShell
 */
@ExperimentalCoroutinesApi
fun script(script: ScriptingShell.() -> Unit) {
    ScriptingShell(
        emptyMap(),
        File(System.getProperty("user.dir")),
        DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE,
        DEFAULT_PIPELINE_RW_PACKET_SIZE,
        DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE
    ).script()
}

/**
 * Executes basic non suspend [Shell] script in given [scope]
 *
 * @see ScriptingShell
 */
@ExperimentalCoroutinesApi
fun script(scope: CoroutineScope, script: ScriptingShell.() -> Unit) {
    ScriptingShell(
        emptyMap(),
        File(System.getProperty("user.dir")),
        DEFAULT_SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE,
        DEFAULT_PIPELINE_RW_PACKET_SIZE,
        DEFAULT_PIPELINE_CHANNEL_BUFFER_SIZE,
        scope
    ).script()
}

/**
 * A class exposing basic non suspend API for shell scripting.
 *
 * To use all [Shell] members call [ScriptingShell.shell] or [shell].
 *
 * @see Shell
 */
@ExperimentalCoroutinesApi
open class ScriptingShell internal constructor (
    private val shell: Shell
) {

    constructor(
        environment: Map<String, String>,
        directory: File,
        SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE: Int,
        PIPELINE_RW_PACKET_SIZE: Long,
        PIPELINE_CHANNEL_BUFFER_SIZE: Int,
        scope: CoroutineScope = GlobalScope
    ) : this(
        Shell.build(
            environment,
            directory,
            scope,
            SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE,
            PIPELINE_RW_PACKET_SIZE,
            PIPELINE_CHANNEL_BUFFER_SIZE
        )
    )

    /**
     * @see Shell.process
     */
    fun String.process() = blocking { shell.systemProcess(this) }

    /**
     * @see Shell.process
     */
    fun File.process(vararg arg: String) = blocking { shell.systemProcess(this, *arg) }

    /**
     * @see Executable.invoke
     */
    fun Executable.run() = blocking { invoke() }

    /**
     * @see Shell.detach
     */
    fun detach(vararg process: ProcessExecutable) = blocking { shell.detach(*process) }

    /**
     * @see Shell.fg
     */
    fun fg(index: Int = 1) = blocking { shell.fg(index) }

    /**
     * @see Shell.join
     */
    fun join(vararg process: Process) = blocking { shell.join(*process) }

    /**
     * @see Shell.joinAll
     */
    fun joinAll() = blocking { shell.joinAll() }

    /**
     * @see Shell.kill
     */
    fun kill(vararg process: Process) = blocking { shell.kill(*process) }

    /**
     * @see Shell.killAll
     */
    fun killAll() = blocking { shell.killAll() }

    /**
     * @see Shell.pipeline
     */
    fun pipeline(pipeConfig: PipeConfig) = blocking { shell.pipeline(ExecutionMode.ATTACHED, pipeConfig) }

    /**
     * Executes suspending script with all [Shell] API available
     *
     * @see Shell
     * @see Shell.shell
     */
    fun shell(script: ShellScript) = blocking {
        shell.shell(script = script)
    }

    private fun <T> blocking(block: suspend () -> T) = runBlocking(shell.scope.coroutineContext) { block() }
}
