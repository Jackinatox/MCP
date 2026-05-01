package com.scyed.clu.docker.stuff

import com.scyed.clu.glyph.EggVariable
import org.springframework.stereotype.Service

@Service
class GlyphEnvVarValidator {
    fun validate(vars: List<EggVariable>, input: Map<String, String>){
        val defined = vars.associateBy { it.envVariable }

        val unknown = input.keys - defined.keys
        if (unknown.isNotEmpty()) {
            throw IllegalArgumentException("Unknown environment variables: ${unknown.joinToString()}")
        }

        val missingRequired = vars.filter { it.required && !input.containsKey(it.envVariable) }
        if (missingRequired.isNotEmpty()) {
            throw IllegalArgumentException("Missing required variables: ${missingRequired.joinToString { it.envVariable }}")
        }
    }
}