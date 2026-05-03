package com.scyed.clu.api

import com.scyed.clu.glyph.GlyphEntity
import com.scyed.clu.glyph.GlyphRepository
import com.scyed.clu.glyph.GlyphSummary
import com.scyed.clu.glyph.toSummary
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/{version}/glyph", version = "v1")
class GlyphController(
    val glyphRepository: GlyphRepository
) {
    @GetMapping
    fun getAllGlyphs(): List<GlyphSummary> = glyphRepository.findAll().map(GlyphEntity::toSummary)


    @GetMapping("{glyphId}")
    fun getById(@PathVariable glyphId: Long): GlyphEntity {
        val glyph = glyphRepository.findById(glyphId)
        if (glyph.isPresent) {
            return glyph.get()
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
