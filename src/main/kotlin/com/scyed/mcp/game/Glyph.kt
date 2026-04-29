package com.scyed.mcp.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * Pterodactyl / Pelican egg descriptor (PTDL_v2 schema).
 *
 * Maps directly to the JSON exported by a panel. Unknown keys are ignored so
 * future schema additions don't break parsing.
 *
 * The `config.files`, `config.startup`, and `config.logs` fields are stored as
 * embedded JSON strings — parse them with your injected ObjectMapper when you
 * need their contents (`objectMapper.readTree(egg.config.files)`).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Glyph(
    @JsonProperty("_comment") val comment: String? = null,
    val meta: EggMeta,
    val exportedAt: String,
    val name: String,
    val author: String,
    val description: String,
    val features: List<String>? = null,
    val dockerImages: Map<String, String> = emptyMap(),
    val fileDenylist: List<String> = emptyList(),
    val startup: String,
    val config: EggConfig,
    val scripts: EggScripts,
    val variables: List<EggVariable> = emptyList(),
) {
    /** Pick a docker image by key, or fall back to the first one declared. */
    fun resolveDockerImage(key: String? = null): String =
        key?.let { dockerImages[it] }
            ?: dockerImages.values.firstOrNull()
            ?: error("Egg '$name' declares no docker images")

    /** Substitute `{{VAR}}` placeholders in the startup command using egg defaults + overrides. */
    fun renderStartup(overrides: Map<String, String> = emptyMap()): String {
        val values = variables.associate { it.envVariable to it.defaultValue } + overrides
        return PLACEHOLDER.replace(startup) { match ->
            values[match.groupValues[1]] ?: match.value
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("""\{\{\s*([A-Z0-9_]+)\s*}}""")
    }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EggMeta(
    val version: String,
    val updateUrl: String? = null,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EggConfig(
    val files: String = "{}",
    val startup: String = "{}",
    val logs: String = "{}",
    val stop: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EggScripts(
    val installation: EggInstallScript,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EggInstallScript(
    val script: String,
    val container: String,
    val entrypoint: String,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EggVariable(
    val name: String,
    val description: String,
    val envVariable: String,
    val defaultValue: String = "",
    val userViewable: Boolean = true,
    val userEditable: Boolean = true,
    val rules: String = "",
    val fieldType: String? = null,
)