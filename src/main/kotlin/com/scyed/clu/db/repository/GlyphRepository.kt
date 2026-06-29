package com.scyed.clu.db.repository

import com.scyed.clu.db.entity.GlyphEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface GlyphRepository : CrudRepository<GlyphEntity, Long> {
    @Query("select g.name from GlyphEntity g")
    fun findAllNames(): List<String>
}