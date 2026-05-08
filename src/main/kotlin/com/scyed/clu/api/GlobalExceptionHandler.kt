package com.scyed.clu.api

import com.scyed.clu.server.ServerState
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): Map<String?, String?> =
        ex.bindingResult.allErrors.associate { error ->
            (error as FieldError).field to error.defaultMessage
        }

    @ExceptionHandler(ServerState.BadServerStateException::class)
    fun handleServerStatusException(ex: ServerState.BadServerStateException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(mapOf("message" to ex.message!!))
    }



}
