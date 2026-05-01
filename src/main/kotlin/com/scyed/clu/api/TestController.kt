package com.scyed.clu.api

import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.function.Consumer

@RestController
@RequestMapping("/", "v1")
class TestController {

    constructor() {
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException
    ): MutableMap<String?, String?> {
        val errors: MutableMap<String?, String?> = HashMap<String?, String?>()
        ex.getBindingResult().getAllErrors().forEach(Consumer { error: ObjectError? ->
            val fieldName = (error as FieldError).getField()
            val errorMessage = error.getDefaultMessage()
            errors.put(fieldName, errorMessage)
        })
        return errors
    }
}