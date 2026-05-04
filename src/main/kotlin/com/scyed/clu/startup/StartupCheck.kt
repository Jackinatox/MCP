package com.scyed.clu.startup

interface StartupCheck {
    val name: String
    fun getOSType(): OSType
    fun runCheck()

    enum class OSType {
        UNIX, WINDOWS, BOTH
    }
}
