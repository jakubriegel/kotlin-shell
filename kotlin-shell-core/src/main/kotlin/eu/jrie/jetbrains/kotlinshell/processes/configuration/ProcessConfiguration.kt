package eu.jrie.jetbrains.kotlinshell.processes.configuration

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessBuilder
import java.io.File

abstract class ProcessConfiguration {
    var configureEnvironment: ProcessBuilder.() -> Unit = {}
        private set

    internal fun env(env: Map<String, String>) = apply {
        configureEnvironment = { withEnv(env) }
    }

    var configureDirectory: ProcessBuilder.() -> Unit = {}
        private set

    internal fun dir(dir: File) = apply {
        configureDirectory = { withDir(dir) }
    }

    /**
     * Converts this configuration to [ProcessBuilder]
     */
    fun builder() = createBuilder().configure()

    /**
     * Creates [ProcessBuilder] and applies custom configurations on it
     */
    protected abstract fun createBuilder(): ProcessBuilder

    /**
     * Contains configurations common for all builders
     */
    private fun ProcessBuilder.configure() = apply {
        configureEnvironment()
        configureDirectory()
    }

}
