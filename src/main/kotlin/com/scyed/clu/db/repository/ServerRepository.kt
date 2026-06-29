package com.scyed.clu.db.repository

import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.db.enums.ServerState
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ServerRepository : CrudRepository<ServerEntity, UUID> {
    fun existsByName(name: String): Boolean

    @Query("select s from ServerEntity s where s.status != :status")
    fun findByNotStatus(@Param("status") status: ServerState): List<ServerEntity>

    @EntityGraph(attributePaths = ["glyphEntity"])
    fun findWithGlyphById(id: UUID): ServerEntity?

    @Transactional
    @Modifying
    @Query("UPDATE ServerEntity s SET s.status = :status WHERE s.id = :id")
    fun updateStatus(id: UUID, status: ServerState)

    @Transactional
    @Modifying
    @Query("UPDATE ServerEntity s SET s.containerId = :containerId WHERE s.id = :id")
    fun updateContainerId(id: UUID, containerId: String?)

    fun findByPodEntityId(podEntityId: UUID): List<ServerEntity>

    fun <S : ServerEntity> save(entity: S, state: ServerState): S {
        return save(entity)
    }
}