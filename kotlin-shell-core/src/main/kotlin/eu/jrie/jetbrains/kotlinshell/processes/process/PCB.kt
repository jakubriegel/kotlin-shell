package eu.jrie.jetbrains.kotlinshell.processes.process

import java.time.Instant

abstract class PCB {
    var state = ProcessState.READY
        internal set
    var exitCode = -1
        internal set
    var startTime: Instant? = null
        internal set
    var endTime: Instant? = null
        internal set
}
