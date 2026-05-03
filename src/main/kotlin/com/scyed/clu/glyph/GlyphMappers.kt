package com.scyed.clu.glyph

data class GlyphSummary(
    val id: Long?,
    val name: String,
    val envVars: List<EggVariable>,
    val startup: String,
    val dockerImages: Map<String, String>,
)

fun GlyphEntity.toSummary() = GlyphSummary(
    id = id,
    name = name,
    envVars = envVars,
    startup = startup,
    dockerImages = dockerImages,
)

fun GlyphEntity.toDto(): Glyph {
    return Glyph(
        comment = comment,
        meta = EggMeta(
            version = metaVersion,
            updateUrl = metaUpdateUrl,
        ),
        exportedAt = exportedAt,
        name = name,
        author = author,
        description = description,
        features = features,
        dockerImages = dockerImages,
        fileDenylist = fileDenylist,
        startup = startup,
        config = EggConfig(
            files = configFiles,
            startup = configStartup,
            logs = configLogs,
            stop = configStop,
        ),
        scripts = EggScripts(
            installation = EggInstallScript(
                script = installScript,
                container = installContainer,
                entrypoint = installEntrypoint,
            ),
        ),
        variables = envVars,
    )
}

fun Glyph.toEntity(): GlyphEntity {
    return GlyphEntity(
        comment = comment,
        metaVersion = meta.version,
        metaUpdateUrl = meta.updateUrl,
        exportedAt = exportedAt,
        name = name,
        author = author,
        description = description,
        features = features ?: emptyList(),
        dockerImages = dockerImages,
        fileDenylist = fileDenylist,
        startup = startup,
        configFiles = config.files,
        configStartup = config.startup,
        configLogs = config.logs,
        configStop = config.stop,
        installScript = scripts.installation.script,
        installContainer = scripts.installation.container,
        installEntrypoint = scripts.installation.entrypoint,
        envVars = variables,
    )
}
