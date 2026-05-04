package com.scyed.clu.startup

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
        val osString = System.getProperty("os.name");
        val os = if (osString.lowercase().contains("windows")) {
            StartupCheck.OSType.WINDOWS
        } else {
            StartupCheck.OSType.UNIX
        }

        startupChecks.sortedBy { it.name }.forEach { check ->
            if (check.getOSType() == StartupCheck.OSType.BOTH || check.getOSType() == os) {
                log.info("Running startup check {}", check.name)
                check.runCheck()
            } else {
                log.debug("Not running startup check {}", check.name)
            }
            log.info("Startup check passed {}", check.name)
        }
    }
}
