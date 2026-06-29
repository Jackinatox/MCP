package com.scyed.clu.api.ws.message

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.scyed.clu.db.enums.ServerStatus


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ConsoleMessage::class, name = "console_output"),
    JsonSubTypes.Type(StatsMessage::class, name = "server_stats"),
    JsonSubTypes.Type(ServerStatusChanged::class, name = "status_changed"),
)
sealed class WsMessage

data class ConsoleMessage(val message: String) : WsMessage()

data class StatsMessage(val cpuPercentage: Double, val memoryMIB: Double, val diskMIB: Double) : WsMessage()
data class ServerStatusChanged(val newStatus: ServerStatus) : WsMessage()