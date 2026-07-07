package com.scyed.clu.db.entity

import com.scyed.clu.db.enums.BackupStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.time.LocalDateTime

@Entity
class BackupEntity(
    @Column(unique = false, nullable = false, length = 30)
    var name: String,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var server: ServerEntity,
    @Column(unique = true, nullable = false, length = 30)
    var snapshotName: String = "sb_" + LocalDateTime.now().toString(), // sb_ = serveerBackup
) {
    @Id
    var id: Long? = null;
    @CreationTimestamp
    lateinit var createdAt: Instant

    @Enumerated(EnumType.STRING)
    var status: BackupStatus = BackupStatus.CREATING
}