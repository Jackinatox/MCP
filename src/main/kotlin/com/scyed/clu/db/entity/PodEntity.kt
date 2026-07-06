package com.scyed.clu.db.entity

import com.scyed.clu.db.enums.PodStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType
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
    @Enumerated(EnumType.STRING)
    // Later migrate to actual enums, this is just a varchar
    var status: PodStatus = PodStatus.ACTIVE,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
}