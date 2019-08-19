package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.ProcessBaseIntegrationTest
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@ExperimentalCoroutinesApi
class ShellIntegrationTest : ProcessBaseIntegrationTest() {
    @Test
    fun `should set given directory for processes`() {
        // given
        val givenDir = testDir("givenDir")
        val file1 = testFile("${givenDir.name}/file1")
        val file2 = testFile("${givenDir.name}/file2")
        var pwd = ""

        // when
        shell (
            dir = givenDir
        ) {
            pipeline { "ls".process() pipe storeResult }
            pwd = env("PWD")
        }

        // then
        assertEquals("${file1.name}\n${file2.name}\n", readResult())
        assertEquals(givenDir.absolutePath, pwd)
    }

    @Test
    fun `should change the directory and set PWD and OLDPWD`() {
        // when
        shell {
            mkdir("newDir")
            cd("newDir")
            val file = file("newFile")

            // then
            assertEquals("$testDirectoryPath/newDir/newFile", file.canonicalPath)
        }
    }

    @Test
    fun `should set given environment`() {
        // given
        val variable = "VARIABLE"
        val value = "value"
        val env = mapOf(variable to value)

        val code = "echo \$$variable"
        val file = testFile(content = code)

        // when
        shell (
            env = env
        ) {
            "chmod +x ${file.name}"()
            "./${file.name}".process() pipe storeResult
        }

        // then
        assertEquals("$value\n", readResult())
    }

    @Test
    fun `should apply environment for arguments`() {
        // given
        val variable = "USER"
        val value = "value"
        val env = mapOf(variable to value)

        // when
        shell (
            env = env
        ) {
            val echo = systemProcess { cmd { "echo" withArg env(variable) } }
            echo pipe storeResult
        }

        // then
        assertEquals("$value\n", readResult())
    }

    @Test
    fun `should create sub shell`() {
        // when
        shell {
            shell {
                pipeline { "echo hello".process() pipe storeResult }
            }
        }

        // then
        assertEquals("hello\n", readResult())
    }

    @Test
    fun `should create sub shell with new directory`() {
        // given
        val givenDir = testDir("givenDir")
        testFile("${givenDir.name}/file1")
        testFile("${givenDir.name}/file2")
        val subDir = testDir("${givenDir.name}/subDir")
        val subFile1 = testFile("${givenDir.name}/${subDir.name}/subFile1")
        val subFile2 = testFile("${givenDir.name}/${subDir.name}/subFile2")

        // when
        shell (
            dir = givenDir
        ) {
            shell (dir = subDir) {
                "ls".process() pipe storeResult
            }
        }

        // then
        assertEquals("${subFile1.name}\n${subFile2.name}\n", readResult())
    }

    @Test
    fun `should create sub shell with new environment`() {
        // given
        val variable = "VARIABLE"
        val value = "value"
        val newValue = "newValue"
        val env = mapOf(variable to value)
        val newEnv = mapOf(variable to newValue)

        val code = "echo \$$variable"
        val file = testFile(content = code)

        // when
        shell (
            env = env
        ) {

            "chmod +x ${file.name}"()

            shell (
                env = newEnv
            ) {
                "./${file.name}".process() pipe storeResult
            }
        }

        // then
        assertEquals("$newValue\n", readResult())
    }

    @Test
    fun `should create sub shell with inherited environment`() {
        // given
        val variable = "VARIABLE"
        val value = "value"
        val env = mapOf(variable to value)

        val code = "echo \$$variable"
        val file = testFile(content = code)

        // when
        shell (
            env = env
        ) {

            "chmod +x ${file.name}"()

            shell {
                "./${file.name}".process() pipe storeResult
            }
        }

        // then
        assertEquals("$value\n", readResult())
    }

    @Test
    fun `should create sub shell with inherited stdout and stderr`() {
        // given
        var fatherOut: ProcessSendChannel? = null
        var childOut: ProcessSendChannel? = null

        // when
        shell {
            fatherOut = stdout
            shell {
                childOut = stdout
            }
        }

        // then
        assertNotEquals(null, fatherOut)
        assertNotEquals(null, childOut)
        assertEquals(fatherOut, childOut)
    }

    @Test
    fun `should create sub shell with empty variables`() {
        // given
        val variable = "VARIABLE"
        val value = "value"

        var empty: Boolean? = null

        // when
        shell {
            variable(variable to value)
            shell {
                empty = variables.isEmpty()
            }
        }

        // then
        assertTrue(empty!!)
    }

    @Test
    fun `should add environment variable`() {
        // given
        val variable = "VARIABLE"
        val value = "value"

        val code = "echo \$$variable"
        val file = testFile(content = code)

        // when
        shell {

            export(variable to value)

            "chmod +x ${file.name}"()

            shell {
                "./${file.name}".process() pipe storeResult
            }
        }

        // then
        assertEquals("$value\n", readResult())
    }

    @Test
    fun `should add shell variable`() {
        // given
        val variable = "VARIABLE"
        val value = "value"

        val code = "echo \$$variable"
        val file = testFile(content = code)

        // when
        shell {

            variable(variable to value)

            "chmod +x ${file.name}"()

            "./${file.name}".process() pipe storeResult
        }

        // then
        assertEquals("$value\n", readResult())
    }

    @Test
    fun `should throw exception when given dir is not directory`() {
        assertThrows<AssertionError> {
            shell (
                dir = testFile()
            ) {  }
        }
    }
}
