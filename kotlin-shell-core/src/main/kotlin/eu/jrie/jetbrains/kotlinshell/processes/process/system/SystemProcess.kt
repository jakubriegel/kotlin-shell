package eu.jrie.jetbrains.kotlinshell.processes.process.system

import eu.jrie.jetbrains.kotlinshell.processes.process.Process
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.streams.inputStream
import org.jetbrains.annotations.TestOnly
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.LogOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class SystemProcess @TestOnly internal constructor (
    vPID: Int,
    val command: String,
    val arguments: List<String>,
    environment: Map<String, String>,
    directory: File,
    stdin: ProcessReceiveChannel,
    stdout: ProcessSendChannel,
    stderr: ProcessSendChannel,
    scope: CoroutineScope,
    private val executor: ProcessExecutor,
    private val systemProcessInputStreamBufferSize: Int
) : Process(vPID, environment, directory, stdin, stdout, stderr, scope) {

    constructor(vPID: Int,
                command: String,
                arguments: List<String>,
                environment: Map<String, String>,
                directory: File,
                stdin: ProcessReceiveChannel,
                stdout: ProcessSendChannel,
                stderr: ProcessSendChannel,
                scope: CoroutineScope,
                systemProcessInputStreamBufferSize: Int
    ) : this(
        vPID, command, arguments, environment, directory, stdin, stdout, stderr,
        scope, ProcessExecutor(), systemProcessInputStreamBufferSize
    )

    override val pcb = SystemPCB()

    override val name: String
        get() = "[${super.name} ${pcb.systemPID} $command]"

    override val statusCmd: String
        get() = command

    override val statusOther: String
        get() = "pid=${pcb.systemPID}"

    init {
        executor
            .command(listOf(command).plus(arguments))
            .redirectInput()
            .redirectOutput()
            .environment(environment)
            .directory(directory)
    }

    @ObsoleteCoroutinesApi
    override suspend fun execute() = wrapThread("${this}_execution_thread") {
        val started = executor.start()!!
        pcb.startTime = started.process.info().startInstant().orElse(Instant.now())
        pcb.systemPID = started.process.pid()
        pcb.startedProcess = started
    } .join()

    @ObsoleteCoroutinesApi
    override suspend fun expect(timeout: Long) =
        if (command == "cat")  {println("CAT");  delay(5000) }
        else {
            wrapThread("${this}_expect_thread") {
                logger.debug("expecting $this")

                with(pcb.startedProcess!!) {
                    val result = if (timeout.compareTo(0) == 0) future.get()
                    else future.get(timeout, TimeUnit.MILLISECONDS)
                    pcb.exitCode = result.exitValue
                }
            }.join()
        }

    override fun isRunning() = if (pcb.startedProcess != null ) pcb.startedProcess!!.process.isAlive else false

    override fun destroy() {
        if(isAlive()) {
            with(pcb.startedProcess!!) {
                val result = future.cancel(true)
                if (!result) throw Exception("cannot kill process $name")
            }
        }
    }

    internal class SystemProcessLogOutputStream (
        private val out: ProcessSendChannel,
        private val scope: CoroutineScope
    ) : LogOutputStream() {
        override fun processLine(line: String?) {
            if (line != null) {
                BytePacketBuilder()
                    .append(line)
                    .append('\n')
                    .build()
                    .also { runBlocking(scope.coroutineContext) { out.send(it) } }
            }
        }
    }

    internal class SystemProcessInputStream (
        private val tap: ProcessReceiveChannel,
        private val scope: CoroutineScope,
        private val bufferSize: Int
    ) : InputStream() {

        private val buffer = ByteBuffer.allocate(bufferSize).apply { flip() }

        private var stream: InputStream? = null

        override fun read(): Int {
            if (!buffer.hasRemaining()) {
                try {
                    receive()
                } catch (e: ClosedReceiveChannelException) {
                    buffer.put(MINUS_ONE)
                    buffer.flip()
                }
            }

            val b = buffer.get().toInt()
            if (b == -1) {
                close()
            }
            return b
        }

        private fun receive() {
            buffer.clear()
            if (stream == null) {
                val packet = runBlocking (scope.coroutineContext) { tap.receive() }
                stream = packet.inputStream()
            }
            readStream()
            buffer.flip()
        }

        private fun readStream() {
            stream!!.let {
                while (buffer.position() < bufferSize) {
                    val b = it.read().toByte()
                    if (b == MINUS_ONE) break
                    else buffer.put(b)
                }
                if (it.available() == 0) {
                    stream = null
                    it.close()
                }
            }
        }

        companion object {
            private const val MINUS_ONE: Byte = -1
        }
    }

    private fun ProcessExecutor.redirectInput() = apply {
        redirectInput(SystemProcessInputStream(stdin, scope, systemProcessInputStreamBufferSize))
    }

    private fun ProcessExecutor.redirectOutput() = apply {
        redirectStdOut()
        redirectStdErr()
    }

    private fun ProcessExecutor.redirectStdOut() = redirectOutput(SystemProcessLogOutputStream(stdout, scope))

    private fun ProcessExecutor.redirectStdErr() = redirectError(SystemProcessLogOutputStream(stderr, scope))

    @ObsoleteCoroutinesApi
    private fun wrapThread(name: String, block: () -> Unit) = scope.launch(newSingleThreadContext(name)) { block() }

}
