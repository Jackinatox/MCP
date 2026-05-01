package com.scyed.clu.startup

interface StartupCheck {
    val name: String

    fun runCheck()
}
