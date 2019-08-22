package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.testutils.TestDataFactory.ENV_VAR_1
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalCoroutinesApi
class ShellTest {
    private lateinit var shell: Shell

    @Test
    fun `should change the directory to given one`() = runTest {
        // given
        val dir = File("someDir").apply { mkdirs() }

        // when
        shell.cd(dir)

        // then
        assertEquals(dir.absoluteFile, shell.directory)
        assertEquals(dir.absolutePath, shell.env("PWD"))

        run<Unit> { dir.deleteRecursively() }
    }

    @Test
    fun `should update OLDPWD after changed directory`() = runTest {
        // given
        val dir1 = File("someDir1").apply { mkdirs() }
        val dir2 = File("someDir2").apply { mkdirs() }

        // when
        shell.cd(dir1)
        shell.cd(dir2)

        // then
        assertEquals(dir2.absoluteFile, shell.directory)
        assertEquals(dir2.absolutePath, shell.env("PWD"))
        assertEquals(dir1.absolutePath, shell.env("OLDPWD"))

        run<Unit> {
            dir1.deleteRecursively()
            dir2.deleteRecursively()
        }
    }

    @Test
    fun `should add new shell variable`() = runTest {
        // when
        shell.variable(ENV_VAR_1)

        // then
        assertEquals(ENV_VAR_1.second, shell.variables[ENV_VAR_1.first])
    }

    @Test
    fun `should add new environment variable`() = runTest {
        // when
        shell.export(ENV_VAR_1)

        // then
        assertEquals(ENV_VAR_1.second, shell.environment[ENV_VAR_1.first])
    }

    @Test
    fun `should unset shell variable`() = runTest {
        // when
        shell.variable(ENV_VAR_1)
        shell.unset(ENV_VAR_1.first)

        // then
        assertEquals(null, shell.variables[ENV_VAR_1.first])
    }

    @Test
    fun `should unset environment variable`() = runTest {
        // when
        shell.export(ENV_VAR_1)
        shell.unset(ENV_VAR_1.first)

        // then
        assertEquals(null, shell.environment[ENV_VAR_1.first])
    }

    private fun <T> runTest(test: suspend ShellTest.() -> T) = runBlocking {
        shell = Shell.build(null, null, this)
        val result = test()
        shell.finalize()
        result
    }

}
