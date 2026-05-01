package com.scyed.clu.glyph

interface GlyphProvider {
    fun getById(id: String): Glyph?
    fun getAll() : List<Glyph>
}