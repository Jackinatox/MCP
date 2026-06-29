package com.scyed.clu.infra.event

import com.scyed.clu.db.enums.ServerStatus
import java.util.UUID

sealed class ContainerEvent {
    abstract val serverId: UUID

    data class ConsoleLine(override val serverId: UUID, val line: String) : ContainerEvent()
    data class ServerStats(override val serverId: UUID, val cpuPercent: Double, val memoryMIB: Int) : ContainerEvent()
    data class Detached(override val serverId: UUID, val containerId: String) : ContainerEvent()
    data class ServerStatusChanged(override val serverId: UUID, val newStatus: ServerStatus) : ContainerEvent()
}