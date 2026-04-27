package com.scyed.mcp.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

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