package com.scyed.mcp.game

interface GlyphProvider {
    fun getById(id: String): Glyph?
    fun getAll() : List<Glyph>
}