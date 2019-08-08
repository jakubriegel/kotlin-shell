package eu.jrie.jetbrains.kotlinshell.shell.piping.from

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Interface joining all PipingFrom interfaces
 */
@ExperimentalCoroutinesApi
interface ShellPipingFrom :
    ShellPipingFromProcess,
    ShellPipingFromLambda,
    ShellPipingFromChannel,
    ShellPipingFromByteReadPacket,
    ShellPipingFromStream,
    ShellPipingFromFile,
    ShellPipingFromString