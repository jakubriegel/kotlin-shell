package eu.jrie.jetbrains.kotlinshell.processes.process

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause2
import kotlinx.coroutines.selects.SelectInstance

/**
 * Sending to [NullSendChannel] behaves like sending to `/dev/null`
 */
class NullSendChannel : ProcessSendChannel {
    @ExperimentalCoroutinesApi
    override val isClosedForSend = false

    @Suppress("OverridingDeprecatedMember")
    @ExperimentalCoroutinesApi
    override val isFull = false

    @InternalCoroutinesApi
    override val onSend: SelectClause2<ProcessChannelUnit, SendChannel<ProcessChannelUnit>> = object :
        SelectClause2<ProcessChannelUnit, SendChannel<ProcessChannelUnit>> {
        @InternalCoroutinesApi
        override fun <R> registerSelectClause2(
            select: SelectInstance<R>,
            param: ProcessChannelUnit,
            block: suspend (SendChannel<ProcessChannelUnit>) -> R
        ) {}
    }

    override fun close(cause: Throwable?) = true

    @ExperimentalCoroutinesApi
    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {}

    override fun offer(element: ProcessChannelUnit) = true

    override suspend fun send(element: ProcessChannelUnit) {}
}
