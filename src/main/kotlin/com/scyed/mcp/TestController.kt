package com.scyed.mcp

import com.scyed.mcp.docker.Provisioning
import com.scyed.mcp.docker.ServerConfig
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.function.Consumer

//@RestController
class TestController {
    private final val provisioning: Provisioning

    constructor(provisioning: Provisioning) {
        this.provisioning = provisioning
    }

    @PostMapping("/")
    fun index(@Valid @RequestBody test: ServerConfig): String {
        val result: String = provisioning.createSerevr(test)
        return result
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