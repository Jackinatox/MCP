package com.scyed.clu.infra.persistence.converter

import com.scyed.clu.db.enums.ServerState
import com.scyed.clu.db.enums.ServerStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class ServerStatusConverter : AttributeConverter<ServerState, String>{
    override fun convertToDatabaseColumn(attribute: ServerState?): String? {
        return attribute.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): ServerState? {
        return ServerState(ServerStatus.valueOf(dbData!!.uppercase()))
    }

}