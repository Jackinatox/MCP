package com.scyed.clu.glyph

import com.scyed.clu.infra.persistence.converter.EggVariableListConverter
import com.scyed.clu.infra.persistence.converter.EnvMapConverter
import com.scyed.clu.infra.persistence.converter.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class GlyphEntity(
    @Column(columnDefinition = "TEXT")
    var comment: String? = null,
    var metaVersion: String,
    var metaUpdateUrl: String? = null,
    var exportedAt: String,
    @Column(unique = true)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var author: String,
    @Column(columnDefinition = "TEXT")
    var description: String,
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    var features: List<String> = emptyList(),
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EnvMapConverter::class)
    var dockerImages: Map<String, String> = emptyMap(),
    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter::class)
    var fileDenylist: List<String> = emptyList(),
    @Column(columnDefinition = "TEXT")
    var startup: String,
    @Column(columnDefinition = "TEXT")
    var configFiles: String = "{}",
    @Column(columnDefinition = "TEXT")
    var configStartup: String = "{}",
    @Column(columnDefinition = "TEXT")
    var configLogs: String = "{}",
    @Column(columnDefinition = "TEXT")
    var configStop: String,
    @Column(columnDefinition = "TEXT")
    var installScript: String,
    var installContainer: String,
    var installEntrypoint: String,
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EggVariableListConverter::class)
    var envVars: List<EggVariable> = emptyList(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}