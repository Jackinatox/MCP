package com.scyed.clu.infra.properrties

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.nio.file.Path

@ConfigurationProperties(prefix = "scyed.environment")
@Validated
data class EnvironmentProperties(

    @field:Size(max = 255)
    @field:Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9_.:\\- ]*$",
        message = "must be a valid ZFS pool name"
    )
    @field:NotBlank
    val gmStorageDataset: String,

    @field:NotNull
    val gmStoragePath: Path,
)