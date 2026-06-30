package com.scyed.clu.infra.zfs

data class ZFSCommandResult(
    val command: List<String>,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
) {
    val isSuccess: Boolean get() = exitCode == 0

    fun commandLine(): String = command.joinToString(" ")
}