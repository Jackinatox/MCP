package com.scyed.clu.docker

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ServerConfig(
    @NotBlank val name: String,
    @Min(1000) @Max(65536) val port: Int,
    @NotBlank val image: String,
    @NotNull val cpuPercent: Long,
    @NotNull val memoryMb: Long,
)

data class PortMapping(val hostPort: Int, val containerPort: Int)
