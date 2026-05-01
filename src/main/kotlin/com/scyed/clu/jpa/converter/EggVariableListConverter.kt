package com.scyed.clu.jpa.converter

import com.scyed.clu.glyph.EggVariable
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Converter
class EggVariableListConverter : AttributeConverter<List<EggVariable>, String> {

    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<EggVariable>?): String {
        return mapper.writeValueAsString(attribute ?: emptyList<EggVariable>())
    }

    override fun convertToEntityAttribute(dbData: String?): List<EggVariable> {
        return if (dbData.isNullOrBlank()) emptyList()
        else mapper.readValue(dbData)
    }
}