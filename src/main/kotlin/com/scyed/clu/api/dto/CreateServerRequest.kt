package com.scyed.clu.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateServerRequest(
    @NotBlank val name: String,
    @NotBlank val imageName: String,
    val description: String? = null,
    @NotNull var cpuPercent: Long,
    @NotNull var memoryMb: Long,
    @NotNull var diskMb: Long,
    @NotNull var env: Map<String, String> = emptyMap(),
    @NotNull var glyphId: Long,
    var startCommand: String?,
    @NotNull var podId: UUID
)