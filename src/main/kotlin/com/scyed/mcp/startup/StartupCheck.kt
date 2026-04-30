package com.scyed.mcp.startup

interface StartupCheck {
    val name: String

    fun runCheck()
}
