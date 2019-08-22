package eu.jrie.jetbrains.kotlinshell.shell

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

@ExperimentalCoroutinesApi
interface ShellUtility : ShellBase {

    /**
     * Changes directory to user root
     *
     * @return new current [directory]
     */
    fun cd() = cd(env("HOME"))

    /**
     * Changes directory to its parent
     *
     * @return new current [directory]
     */
    fun cd(up: Up) = cd(directory.parentFile)

    /**
     * Changes directory to previous
     *
     * @return new current [directory]
     */
    fun cd(pre: Pre) = cd(env("OLDPWD"))

    /**
     * Changes directory to given [path]
     *
     * @return new current [directory]
     */
    fun cd(path: String): File {
        val absolutePath = if (path.startsWith('/')) path else "${env("PWD")}/$path"
        return cd(File(absolutePath))
    }

    /**
     * Changes directory to given [dir]
     *
     * @return new current [directory]
     */
    fun cd(dir: File): File

    /**
     * Adds new shell variable
     */
    fun variable(variable: Pair<String, String>)

    infix fun Readonly.variable(variable: Pair<String, String>)

    /**
     * Adds new environment variable
     */
    fun export(env: Pair<String, String>)

    infix fun Readonly.export(env: Pair<String, String>)

    /**
     * Removes shell or environmental variable matching given key
     */
    fun unset(key: String)

    /**
     * Retrieves environment variables
     *
     * @see [set]
     * @see [shellEnv]
     */
    val env: ShellCommand get() = command { environment.toEnvString() }

    /**
     * Retrieves environment or shell variable matching given key or `""`
     *
     * @see [env]
     * @see [shellEnv]
     * @see [variable]
     * @see [variables]
     */
    fun env(key: String) = environment[key] ?: variables[key] ?: ""

    /**
     * Retrieves shell environment variables
     */
    val set: ShellCommand get() = command { shellEnv.toEnvString() }

    /**
     * Retrieves all environment variables and returns them as a [Map]
     *
     * @see env
     */
    val shellEnv: Map<String, String>
        get() = environment.plus(variables)

    /**
     * System environment variables
     *
     * @see [set]
     * @see [env]
     */
    val systemEnv: Map<String, String>
        get() = System.getenv().toMap()

    private fun Map<String, String>.toEnvString() = StringBuilder().let { b ->
        forEach { (k, v) -> b.append("$k=$v\n") }
        b.toString()
    }

    private fun getFile(name: String) = File("${env("PWD")}/$name")

    /**
     * Gets file with [name] relative to current [directory].
     * If file don't exist it creates it.
     */
    fun file(name: String) = getFile(name).apply { if (!exists()) createNewFile() }

    /**
     * Creates new file with given [name] relative to current [directory].
     * If file already existed it overrides it.
     */
    fun file(name: String, content: String = "") = file(name).apply { writeText(content) }

    /**
     * Creates directory with [name] relative to current [directory]
     */
    fun mkdir(name: String) = getFile(name).apply {
        if (exists()) throw Exception("file exists")
        mkdirs()
    }
}
/**
 * Object for [up] alias
 */
object Up
/**
 * Alias to be used when changing directory up
 *
 * Ex: `cd(up)` equals `cd ..`
 *
 * @see ShellUtility.cd
 */
typealias up = Up

/**
 * Object for [pre] alias
 */
object Pre
/**
 * Alias to be used when changing directory to previous one
 *
 * Ex: `cd(pre)` equals `cd -`
 *
 * @see ShellUtility.cd
 */
typealias pre = Pre

/**
 * Object for [readonly] keyword
 */
object Readonly
/**
 * Keyword for making variables readonly
 *
 * Ex: `readonly export("VAR" to "VAL")`
 * Ex: `readonly variable("VAR" to "VAL")`
 */
typealias readonly = Readonly
