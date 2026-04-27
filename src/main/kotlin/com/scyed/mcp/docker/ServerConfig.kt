package com.scyed.mcp.docker

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ServerConfig(
    @NotBlank val name: String,
    @Min(1000) @Max(65536) val port: Int,
    @NotBlank val image: String
)
