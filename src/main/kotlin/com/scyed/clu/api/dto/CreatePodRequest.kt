package com.scyed.clu.api.dto

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

data class CreatePodRequest(
    @Positive val cpuPercent: Int,
    @Positive val memoryMb: Double,
    @Positive val diskMb: Double,
    @PositiveOrZero val backups: Int,
)
