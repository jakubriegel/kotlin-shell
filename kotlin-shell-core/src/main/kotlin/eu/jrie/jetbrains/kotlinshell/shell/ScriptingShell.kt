package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.execution.Executable
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.shell.piping.PipelineConfig
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
        File(System.getProperty("user.dir"))
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
        scope
    ).script()
}

@ExperimentalCoroutinesApi
fun shell(script: ShellScript) {
    script {
        shell(script)
    }
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
        scope: CoroutineScope = GlobalScope
    ) : this(
        Shell.build(
            environment,
            directory,
            scope
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
    fun pipeline(pipelineConfig: PipelineConfig) = blocking { shell.pipeline(ExecutionMode.ATTACHED, pipelineConfig) }

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
