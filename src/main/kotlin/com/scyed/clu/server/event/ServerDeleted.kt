package com.scyed.clu.server.event

import java.util.UUID

data class ServerDeletionStarted(val serverId: UUID)
data class ServerDeleted(val serverId: UUID)
