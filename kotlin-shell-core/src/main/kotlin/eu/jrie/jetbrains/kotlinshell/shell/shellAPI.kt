@file:Suppress("EXPERIMENTAL_API_USAGE")

package eu.jrie.jetbrains.kotlinshell.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import java.io.File

typealias ShellScript = suspend Shell.() -> Unit

/**
 * Creates new [Shell] with given parameters and executes the [script]
 */
@ExperimentalCoroutinesApi
suspend fun shell(
    env: Map<String, String>? = null,
    dir: File? = null,
    script: ShellScript
) = coroutineScope {
    shell(
        env,
        dir,
        this,
        script
    )
}

/**
 * Creates new [Shell] with given parameters and executes the [script]
 */
@ExperimentalCoroutinesApi
suspend fun shell(
    env: Map<String, String>? = null,
    dir: File? = null,
    scope: CoroutineScope,
    script: ShellScript
) {
    Shell.build(
        env,
        dir,
        scope
    )
        .apply { script() }
        .finalize()
    Shell.logger.debug("shell end")
}
