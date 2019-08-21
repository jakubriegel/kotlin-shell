package eu.jrie.jetbrains.kotlinshell

import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.KotlinJars
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

class ShellCacheTest : CompiledJvmScriptsCache {

    init {
        println("init cache")
    }

    override fun get(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): CompiledScript<*>? {
        println("getting from cache")
        return null
    }

    override fun store(
        compiledScript: CompiledScript<*>,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        println("storing in cache")
    }
}

private fun checkWithCache(
    cache: ShellCacheTest, script: String, expectedOutput: List<String>,
    configurationBuilder: ScriptCompilationConfiguration.Builder.() -> Unit = {}
) {
//    val hostConfiguration = defaultJvmScriptingHostConfiguration.with {
//        jvm {
//            baseClassLoader.replaceOnlyDefault(null)
//            compilationCache(cache)
//        }
//    }
    val compiler = JvmScriptCompiler()//hostConfiguration)
    val evaluator = BasicJvmScriptEvaluator()

    val scriptCompilationConfiguration = ScriptCompilationConfiguration(body = configurationBuilder).with {
        updateClasspath(KotlinJars.kotlinScriptStandardJarsWithReflect)
        hostConfiguration(
            hostConfiguration().with {
                jvm {
                    baseClassLoader.replaceOnlyDefault(null)
                    compilationCache(ShellCacheTest())
                }
            }
        )
    }

    var compiledScript: CompiledScript<*>? = null
    runBlocking {
        compiler(script.toScriptSource(), scriptCompilationConfiguration).onSuccess {
            compiledScript = it
            evaluator(it)
        }
    }

    val cachedScript = cache.get(script.toScriptSource(), compiledScript!!.compilationConfiguration)
}

fun main() {
    checkWithCache(ShellCacheTest(), "println(\"welcome\")", listOf("hello"))
}


