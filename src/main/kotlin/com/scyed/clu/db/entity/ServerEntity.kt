package com.scyed.clu.db.entity

import com.scyed.clu.infra.persistence.converter.EnvMapConverter
import com.scyed.clu.infra.persistence.converter.ServerStatusConverter
import com.scyed.clu.db.enums.ServerState
import com.scyed.clu.db.enums.ServerStatus
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.domain.AbstractAggregateRoot
import java.time.Instant
import java.util.UUID

// TODO: check enum creation in postgres server if @enumertaed.oridnal would create an enum type and set up enum for pod state to mark the pod as deleted


@Entity
class ServerEntity(
    @Column(unique = true) var name: String,
    var description: String?,
    var containerId: String?,
    var image: String = "",
    @Convert(converter = ServerStatusConverter::class) var status: ServerState = ServerState(ServerStatus.PROVISIONING),
    var skip_scripts: Boolean = false,
    var memoryMb: Long,
    var diskMb: Long,
    var cpuPercent: Long,
    @Column(columnDefinition = "TEXT") // or JSON if your DB supports it
    @Convert(converter = EnvMapConverter::class) var env: Map<String, String> = emptyMap(),
    var startCommand: String,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var glyphEntity: GlyphEntity,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var podEntity: PodEntity,

    @CreationTimestamp var createdAt: Instant = Instant.now(),
    @UpdateTimestamp var updatedAt: Instant = Instant.now(),
): AbstractAggregateRoot<ServerEntity>() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    override fun toString(): String = "Server(id=$id, image='$image')"

    fun toEnvList(): List<String> {
        return env.map { entry -> entry.key + "=" + entry.value }.toList()
    }
}