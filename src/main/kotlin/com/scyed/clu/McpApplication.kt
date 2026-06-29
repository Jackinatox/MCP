package com.scyed.clu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class McpApplication

fun main(args: Array<String>) {
	runApplication<McpApplication>(*args)
}
