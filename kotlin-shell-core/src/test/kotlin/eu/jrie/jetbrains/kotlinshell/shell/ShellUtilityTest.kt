package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.ProcessCommander
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

@ExperimentalCoroutinesApi
class ShellUtilityTest {

    private val shell = SampleShell()

    @BeforeEach
    fun prepareDir() {
        File(TEST_DIR_NAME).absoluteFile
            .apply { mkdirs() }
            .also { shell.environment = mapOf("PWD" to it.absolutePath, "OLDPWD" to it.parent) }
            .also { shell.directory = it }
    }

    @AfterEach
    fun cleanup() {
        File(TEST_DIR_NAME).deleteRecursively()
    }

    @Test
    fun `should change the directory to user root`() {
        // given
        val home = System.getProperty("user.home")
        val userRoot = File(home)
        shell.environment = mapOf("HOME" to home)

        // when
        shell.cd()

        // then
        assertEquals(userRoot, shell.cdArg)
    }

    @Test
    fun `should change the directory to its parent`() {
        // given
        val parent = File(shell.env("PWD")).parentFile

        // when
        shell.cd(up)

        // then
        assertEquals(parent, shell.cdArg)
    }

    @Test
    fun `should change the directory to previous one`() {
        // given
        val previous = File(shell.env("OLDPWD"))

        // when
        shell.cd(pre)

        // then
        assertEquals(previous, shell.cdArg)
    }

    @Test
    fun `should change the directory to given path`() {
        // given
        val path = "/some/path"

        // when
        shell.cd(path)

        // then
        assertEquals(File(path), shell.cdArg)
    }

    @Test
    fun `should change the directory to given one`() {
        // given
        val path = "/some/path"
        val file = File(path)

        // when
        shell.cd(file)

        // then
        assertEquals(file, shell.cdArg)
    }

    @Test
    fun `should retrieve environment variable`() {
        // given
        val key = "HOME"

        // when
        val result = shell.env(key)

        // then
        assertEquals("", result)
    }

    @Test
    fun `should retrieve all environment variables`() {
        // expect
        assertEquals(emptyMap<String, String>() + System.getenv(), shell.systemEnv)
    }

    @Test
    fun `should retrieve system environment variables`() {
        // expect
        assertEquals(System.getenv(), shell.systemEnv)
    }

    @Test
    fun `should create new empty file`() {
        // when
        val file = shell.file("newFile")

        // then
        assertTrue(file.exists())
        assertTrue(file.isFile)
    }

    @Test
    fun `should get existing file`() {
        // given
        val name = "existingFile"
        val existingFile = createFile(name)

        // when
        shell.file(name)

        // then
        assertTrue(existingFile.exists())
    }

    @Test
    fun `should create new file with given content`() {
        // given
        val content = "abc"

        // when
        val file = shell.file("file", content)

        // then
        assertTrue(file.exists())
        assertEquals(content, file.readText())
    }

    @Test
    fun `should override existing file`() {
        // given
        val name = "existingFile"
        createFile(name).writeText("abc")
        val newContent = "def"

        // when
        val file = shell.file(name, newContent)

        // then
        assertTrue(file.exists())
        assertEquals(newContent, file.readText())
    }

    @Test
    fun `should create directory`() {
        // when
        val dir = shell.mkdir("dir")

        // then
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun `should throw exception when trying to create existing directory`() {
        // given
        createDir()

        // expect
        assertThrows<Exception> {  shell.mkdir("dir") }
    }

    @Suppress("SameParameterValue")
    private fun createFile(name: String) = File("$TEST_DIR_NAME/$name").apply { createNewFile() }

    private fun createDir(name: String = "dir") = File("$TEST_DIR_NAME/$name").apply { mkdir() }

    companion object {
        private const val TEST_DIR_NAME = "shell_utility_test_dir"
    }

    @ExperimentalCoroutinesApi
    private class SampleShell : ShellUtility {
        override val scope: CoroutineScope = mockk()

        internal var cdArg: File? = null

        override fun cd(dir: File): File {
            cdArg = dir
            return directory
        }

        override fun variable(variable: Pair<String, String>) {}
        override fun export(env: Pair<String, String>) {}
        override fun unset(key: String) {}

        override var environment: Map<String, String> = emptyMap()
        override var variables: Map<String, String> = emptyMap()
        override var directory: File = File("")
        override val commander: ProcessCommander = mockk()

        override val stdout: ProcessSendChannel = Channel()
        override val stderr: ProcessSendChannel = Channel()
        override val stdin: ProcessReceiveChannel = Channel()

        override suspend fun finalize() {}

        override val SYSTEM_PROCESS_INPUT_STREAM_BUFFER_SIZE: Int = 1
        override val PIPELINE_RW_PACKET_SIZE: Long = 1
        override val PIPELINE_CHANNEL_BUFFER_SIZE: Int = 1
    }
}
