package com.scyed.clu.server.event

import com.scyed.clu.db.entity.ServerEntity

data class KillServerRequested(val server: ServerEntity)
