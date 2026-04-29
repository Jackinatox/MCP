package com.scyed.mcp.jpa.repositories

import com.scyed.mcp.jpa.ServerEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ServerRepository : CrudRepository<ServerEntity, UUID> {
    fun existsByName(name: String): Boolean
}