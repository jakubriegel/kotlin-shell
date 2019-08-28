package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import eu.jrie.jetbrains.kotlinshell.shell.piping.AbstractPipingDSLShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

@ExperimentalCoroutinesApi
internal class PipingDSLShell (
    environment: Map<String, String>,
    variables: Map<String, String>,
    directory: File,
    scope: CoroutineScope,
    commander: ProcessCommander,
    stdout: ProcessSendChannel,
    stderr: ProcessSendChannel
) : AbstractPipingDSLShell, Shell(environment, variables, directory, scope, commander, stdout, stderr) {
    companion object {
        fun from(shell: ShellBase) = with(shell) { PipingDSLShell(environment, variables, directory, scope, commander, stdout, stderr) }
    }
}
