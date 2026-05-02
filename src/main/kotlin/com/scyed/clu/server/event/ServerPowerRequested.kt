package com.scyed.clu.server.event

import com.scyed.clu.server.PowerAction
import com.scyed.clu.server.TriggeredBy
import java.util.UUID

data class ServerPowerRequested(
    val serverId: UUID,
    val action: PowerAction,
    val triggeredBy: TriggeredBy,
)
