package eu.jrie.jetbrains.kotlinshell.processes.process

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlinx.io.streams.inputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

typealias ProcessChannelUnit = ByteReadPacket
typealias ProcessChannel = Channel<ProcessChannelUnit>
typealias ProcessReceiveChannel = ReceiveChannel<ProcessChannelUnit>
typealias ProcessSendChannel = SendChannel<ProcessChannelUnit>

internal class ProcessChannelInputStream (
    private val channel: ProcessReceiveChannel,
    private val scope: CoroutineScope,
    bufferSize: Int
) : InputStream() {

    private val buffer = IoBuffer(ByteBuffer.allocate(bufferSize))

    private var stream: InputStream? = null

    override fun read(): Int {
        if (!buffer.canRead()) get()
        return buffer.readInt()
    }

    private fun get() {
        buffer.resetForWrite()
        try {
            receive()
        } catch (e: ClosedReceiveChannelException) {
            buffer.writeInt(-1)
        }
    }

    private fun receive() {
        if (stream == null) {
            val packet = runBlocking (scope.coroutineContext) { channel.receive() }
            stream = packet.inputStream()
        }
        readStream()
    }

    private fun readStream() {
        stream!!.let {
            while (buffer.canWrite()) {
                val b = it.read()
                if (b == -1) break
                else buffer.writeInt(b)
            }
            if (it.available() == 0) {
                stream = null
                it.close()
            }
        }
    }

    override fun available() = if (buffer.canRead()) 1 else 0
}

internal class ProcessChannelOutputStream (
    private val channel: ProcessSendChannel,
    private val scope: CoroutineScope,
    bufferSize: Int
) : OutputStream() {

    private val buffer = IoBuffer(ByteBuffer.allocate(bufferSize))

    override fun write(b: Int) {
        if (!buffer.canWrite()) flush()
        buffer.writeByte(b.toByte())
    }

    override fun flush() {
        val packet = buildPacket { this.writeFully(buffer.readBytes()) }
        runBlocking (scope.coroutineContext) { channel.send(packet) }
        buffer.resetForWrite()
    }

    override fun close() {
        flush()
        channel.close()
    }
}
