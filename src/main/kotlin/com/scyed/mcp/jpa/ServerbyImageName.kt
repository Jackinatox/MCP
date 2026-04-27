package com.scyed.mcp.jpa

import org.springframework.data.repository.CrudRepository

interface ServerbyImageName : CrudRepository<Server, Long> {
    fun findByImage(image: String): List<Server>
}