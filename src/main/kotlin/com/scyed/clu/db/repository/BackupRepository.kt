package com.scyed.clu.db.repository

import com.scyed.clu.db.entity.BackupEntity
import org.springframework.data.repository.CrudRepository

interface BackupRepository: CrudRepository<BackupEntity, Long> {
}