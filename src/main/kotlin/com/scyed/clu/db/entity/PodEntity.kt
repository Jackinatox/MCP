package com.scyed.clu.db.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
class PodEntity (
    var cpuPercent: Int,
    var memoryMb: Double,
    var diskMb: Double,
    var backups: Int,
    @Column(unique = true)
    var datasetName: String,
    var status: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
}