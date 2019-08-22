package eu.jrie.jetbrains.kotlinshell.shell

import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessChannelUnit
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessReceiveChannel
import eu.jrie.jetbrains.kotlinshell.processes.process.ProcessSendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.writeFully
import kotlinx.io.streams.writePacket
import java.io.OutputStream
import java.io.PrintStream

@ExperimentalCoroutinesApi
internal fun initOut(scope: CoroutineScope): Pair<ProcessSendChannel, Job> {
    val systemOut = System.out
    val out = Channel<ProcessChannelUnit>()
    val job = scope.launch { consumeOut(out, systemOut) }

    if (System.getenv("REDIRECT_PRINT")?.toUpperCase() != "NO") redirectPrint(out, job, scope, systemOut)
    return out to job
}

@ExperimentalCoroutinesApi
private suspend fun consumeOut(stdout: ProcessReceiveChannel, systemOut: PrintStream) {
    stdout.consumeEach { p ->
        systemOut.writePacket(p)
        systemOut.flush()
    }
}

private fun redirectPrint(stdout: ProcessSendChannel, job: Job, scope: CoroutineScope, systemOut: PrintStream) {
    val shellOut = ShellSystemOutStream(stdout, scope)
    val systemShellOut = PrintStream(shellOut)
    System.setOut(systemShellOut)
    job.invokeOnCompletion { System.setOut(systemOut) }
}

private class ShellSystemOutStream (
    private val stdout: ProcessSendChannel,
    private val scope: CoroutineScope
) : OutputStream() {

    override fun write(b: ByteArray, off: Int, len: Int) = write(b.sliceArray(off until off+len))
    override fun write(b: ByteArray) = send(buildPacket { writeFully(b) })
    override fun write(b: Int) = send(buildPacket { writeByte(b.toByte()) })

    private fun send(packet: ByteReadPacket) = runBlocking(scope.coroutineContext) {
        stdout.send(packet)
    }
}
