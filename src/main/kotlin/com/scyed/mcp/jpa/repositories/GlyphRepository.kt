package com.scyed.mcp.jpa.repositories

import com.scyed.mcp.jpa.GlyphEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface GlyphRepository : CrudRepository<GlyphEntity, Long> {
    @Query("select g.name from GlyphEntity g")
    fun findAllNames(): List<String>
}