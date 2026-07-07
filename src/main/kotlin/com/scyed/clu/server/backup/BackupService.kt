package com.scyed.clu.server.backup

import com.scyed.clu.db.entity.BackupEntity
import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.db.enums.BackupStatus
import com.scyed.clu.db.repository.BackupRepository
import com.scyed.clu.infra.zfs.ZFSManager
import org.springframework.stereotype.Service

@Service
class BackupService(private val zFSManager: ZFSManager, private val backupRepository: BackupRepository) {
    fun create(backupName: String, server: ServerEntity) {
        val backup = backupRepository.save(BackupEntity(
            name = backupName,
            server = server
        ))
        zFSManager.createSnapshot(backup)
        backup.status = BackupStatus.CREATED
        backupRepository.save(backup)
    }
}