package com.scyed.clu.glyph

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Configuration for filesystem-backed egg loading.
 *
 * `application.yml`:
 * ```yaml
 * scyed:
 *   eggs:
 *     directory: /opt/scyed/eggs
 * ```
 */
@ConfigurationProperties(prefix = "scyed.eggs")
data class EggProperties(
    val directory: Path = Paths.get("leck"),
)

@Configuration
@EnableConfigurationProperties(EggProperties::class)
class EggConfigurationFS

/**
 * Loads eggs from `*.json` files in [EggProperties.directory]. Each file's name
 * (minus the `.json` extension) becomes its ID.
 *
 * Eggs are read once at construction time. Call [refresh] to reload from disk —
 * useful behind a management endpoint or a file-watcher later.
 *
 * Malformed eggs are logged and skipped so one bad file doesn't prevent boot.
 */
@Component
class FileSystemGlyphProvider(
    private val properties: EggProperties, private val glyphRepository: GlyphRepository
) : GlyphProvider {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Volatile
    private var eggs: Map<String, Glyph> = load()

    override fun getById(id: String): Glyph? = eggs[id]

    override fun getAll(): List<Glyph> = eggs.values.toList()

    /** Re-scan the configured directory and replace the cached egg set. */
    fun refresh() {
        eggs = load()
    }

    private fun load(): Map<String, Glyph> {
        val dir = properties.directory
        log.info("Loading eggs from ${dir.toAbsolutePath()}")

        if (!Files.isDirectory(dir)) {
            log.warn("Egg directory {} does not exist; no eggs loaded", dir.toAbsolutePath())
            return emptyMap()
        }
        val files = Files.list(dir).use { stream ->
            stream.filter { it.extension.equals("json", ignoreCase = true) }.toList()
        }

        val existingNames = glyphRepository.findAllNames().toSet()

        val glyphs = files.mapNotNull(::tryLoad).toMap()

        glyphs.values.filter { it.name !in existingNames }.forEach { entity -> glyphRepository.save(entity.toEntity())}

        return glyphs
    }

    private fun tryLoad(path: Path): Pair<String, Glyph>? = runCatching {
        path.nameWithoutExtension to objectMapper.readValue(path.toFile(), Glyph::class.java)
    }.onFailure { log.error("Failed to load egg from {}: {}", path, it.message) }.getOrNull()
}
