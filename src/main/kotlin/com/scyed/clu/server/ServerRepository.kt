package com.scyed.clu.server

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface ServerRepository : CrudRepository<ServerEntity, UUID> {
    fun existsByName(name: String): Boolean

    @EntityGraph(attributePaths = ["glyphEntity"])
    fun findWithGlyphById(id: UUID): ServerEntity?
}