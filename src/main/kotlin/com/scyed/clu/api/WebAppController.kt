package com.scyed.clu.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WebAppController {

    @GetMapping("/{version}/webApp", "/{version}/webApp/**")
    fun spa(request: HttpServletRequest): ResponseEntity<Resource> {
        val subPath = request.requestURI.substringAfter("/webApp/", "")

        if (subPath.isNotEmpty()) {
            val static = ClassPathResource("static/$subPath")
            if (static.exists()) {
                val mediaType = MediaTypeFactory.getMediaType(static).orElse(MediaType.APPLICATION_OCTET_STREAM)
                return ResponseEntity.ok().contentType(mediaType).body(static)
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(ClassPathResource("static/index.html"))
    }
}
