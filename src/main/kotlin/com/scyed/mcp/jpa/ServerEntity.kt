package com.scyed.mcp.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
class ServerEntity(
    @Column(unique = true)
    var name: String,
    var description: String?,
    var containerId: String?,
    var image: String = "",
    @Enumerated(EnumType.STRING) var status: ServerStatus = ServerStatus.PROVISIONING,
    var skip_scripts: Boolean = false,
    var memoryMb: Long,
    var cpuPercent: Long,
    @Column(columnDefinition = "TEXT") // or JSON if your DB supports it
    @Convert(converter = EnvMapConverter::class)
    var env: Map<String, String> = emptyMap(),
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    override fun toString(): String = "Server(id=$id, image='$image')"
}