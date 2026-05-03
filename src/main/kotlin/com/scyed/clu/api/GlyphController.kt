package com.scyed.clu.api

import com.scyed.clu.glyph.GlyphEntity
import com.scyed.clu.glyph.GlyphRepository
import com.scyed.clu.glyph.GlyphSummary
import com.scyed.clu.glyph.toSummary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/{version}/glyph", version = "v1")
class GlyphController(
    val glyphRepository: GlyphRepository
) {
    @GetMapping
    fun getAllGlyphs(): List<GlyphSummary> =
        glyphRepository.findAll().map(GlyphEntity::toSummary)
}
