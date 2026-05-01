package com.scyed.clu.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Converter
class EnvMapConverter : AttributeConverter<Map<String, String>, String> {

    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, String>?): String {
        return mapper.writeValueAsString(attribute ?: emptyMap<String, String>())
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, String> {
        return if (dbData.isNullOrBlank()) emptyMap()
        else mapper.readValue(dbData)
    }
}