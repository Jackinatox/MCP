package com.scyed.mcp.startup

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(0)
class StartupCheckRunner(
    private val startupChecks: List<StartupCheck>,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        startupChecks.sortedBy { it.name }.forEach { check ->
            log.info("Running startup check {}", check.name)
            check.runCheck()
            log.info("Startup check passed {}", check.name)
        }
    }
}
