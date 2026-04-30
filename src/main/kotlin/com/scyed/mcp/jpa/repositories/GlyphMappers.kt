package com.scyed.mcp.jpa.repositories

import com.scyed.mcp.game.EggConfig
import com.scyed.mcp.game.EggInstallScript
import com.scyed.mcp.game.EggMeta
import com.scyed.mcp.game.EggScripts
import com.scyed.mcp.game.Glyph
import com.scyed.mcp.jpa.GlyphEntity

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
