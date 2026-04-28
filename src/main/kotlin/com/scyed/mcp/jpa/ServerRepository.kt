package com.scyed.mcp.jpa

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ServerRepository : CrudRepository<Server, UUID> {
    fun findByImage(image: String): List<Server>
}