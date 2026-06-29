package com.scyed.clu.db.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class PodEntity (
    var cpuPercent: Int,
    var memoryMb: Double,
    var diskMb: Double,
    var backups: Int,

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}