package com.scyed.mcp.jpa

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ServerRepository : CrudRepository<ServerEntity, UUID> {
    fun findByImage(image: String): List<ServerEntity>
    fun existsByName(name: String): Boolean
}