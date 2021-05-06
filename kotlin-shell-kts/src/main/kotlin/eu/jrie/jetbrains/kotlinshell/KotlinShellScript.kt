package eu.jrie.jetbrains.kotlinshell

import eu.jrie.jetbrains.kotlinshell.processes.execution.ExecutionContext
import eu.jrie.jetbrains.kotlinshell.processes.execution.ProcessExecutable
import eu.jrie.jetbrains.kotlinshell.processes.pipeline.Pipeline
import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessState
import eu.jrie.jetbrains.kotlinshell.processes.process.system.SystemProcess
import eu.jrie.jetbrains.kotlinshell.shell.ScriptingShell
import eu.jrie.jetbrains.kotlinshell.shell.Shell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.kotlin.mainKts.Import
import org.jetbrains.kotlin.mainKts.MainKtsConfigurator
import java.io.File
import java.util.Collections.emptyMap
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223

/**
 * Script definition for writing shell scripts in Kotlin Script
 *
 * @see ScriptingShell
 */
@Suppress("unused")
@ExperimentalCoroutinesApi
@KotlinScript(
    fileExtension = "sh.kts",
    compilationConfiguration = KotlinShellScriptConfiguration::class,
    evaluationConfiguration = KotlinShellScriptEvaluationConfiguration::class
)
open class KotlinShellScript (
    val args: Array<String>
) : ScriptingShell(
    emptyMap(),
    File(System.getProperty("user.dir"))
)

@ExperimentalCoroutinesApi
class KotlinShellScriptConfiguration : ScriptCompilationConfiguration (
    {
        defaultImports(DependsOn::class, Repository::class, Import::class)
        defaultImports(*ESSENTIAL_KOTLIN_SHELL_CLASSES)
        defaultImports(*ESSENTIAL_KOTLIN_SHELL_IMPORTS)
        jvm {
            dependenciesFromClassContext(KotlinShellScriptConfiguration::class, "kotlin-shell-kts", "kotlin-stdlib", "kotlin-reflect")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, handler = MainKtsConfigurator())
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        jsr223 {
            importAllBindings(true)
        }
    }
) {
    companion object {
        private val ESSENTIAL_KOTLIN_SHELL_CLASSES = arrayOf(
            Shell::class,
            ExecutionContext::class,
            Process::class, SystemProcess::class, ProcessState::class, ProcessExecutable::class,
            Pipeline::class
        )
        private val ESSENTIAL_KOTLIN_SHELL_IMPORTS = arrayOf(
            "eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannel",
            "eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel",
            "eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel",
            "eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit",

            "eu.jrie.jetbrains.kotlinshell.shell.piping.PipelineContextLambda",
            "eu.jrie.jetbrains.kotlinshell.shell.piping.PipelinePacketLambda",
            "eu.jrie.jetbrains.kotlinshell.shell.piping.PipelineByteArrayLambda",
            "eu.jrie.jetbrains.kotlinshell.shell.piping.PipelineStringLambda",
            "eu.jrie.jetbrains.kotlinshell.shell.piping.PipelineStreamLambda",

            "eu.jrie.jetbrains.kotlinshell.shell.ShellCommand",

            "eu.jrie.jetbrains.kotlinshell.shell.up",
            "eu.jrie.jetbrains.kotlinshell.shell.pre",
            "eu.jrie.jetbrains.kotlinshell.shell.readonly",

            "kotlinx.coroutines.channels.Channel",
            "kotlinx.coroutines.delay",
            "kotlinx.coroutines.channels.consumeEach",
            "kotlinx.coroutines.delay",

            "java.io.OutputStream",
            "java.io.InputStream",

            "kotlinx.io.core.ByteReadPacket",
            "kotlinx.io.core.readBytes",
            "kotlinx.io.core.writeFully",
            "kotlinx.io.core.readText",
            "kotlinx.io.core.writeText",
            "kotlinx.io.streams.writePacket"
        )
    }
}

class KotlinShellScriptEvaluationConfiguration : ScriptEvaluationConfiguration (
    {
        scriptsInstancesSharing(true)
        refineConfigurationBeforeEvaluate(::configureProvidedPropertiesFromJsr223Context)
    }
)
