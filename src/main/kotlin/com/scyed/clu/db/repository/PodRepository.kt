package com.scyed.clu.db.repository

import com.scyed.clu.db.entity.PodEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface PodRepository : CrudRepository<PodEntity, UUID> {

}