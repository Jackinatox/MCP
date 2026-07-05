package com.scyed.clu.infra.zfs

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader

//TODO: 100% AI: A fix so  zfs dataset that gets created form java is woned by the user executing it. by default it was owned by root

/**
 * Manual diagnostic test for the "datasets created with the wrong owner" bug.
 *
 * ZFSManager.execute() runs `sudo <zfsBinary> ...` and sets SUDO_UID/SUDO_GID/SUDO_USER
 * on the child process environment, apparently hoping this makes sudo run the command as
 * that uid/gid. It does not: those variables are only ever *set by* sudo for the process
 * it launches (so the launched process can tell who invoked sudo) - sudo never reads them
 * to decide which user to run as. The target user is decided solely by `sudo -u ...` or by
 * the matching sudoers rule (here: `(root) NOPASSWD: /usr/sbin/zfs`), so the zfs command
 * - and therefore the dataset/mountpoint it creates - ends up owned by root, not by
 * scyed.gameserver.user-uid/gid.
 *
 * This is not a normal CI test: it shells out to real `sudo`/`zfs`/`stat` on the host and
 * (in the last test) creates and destroys a real dataset under the configured storage pool.
 * Remove @Disabled to run it manually against a dev box that has zfs + the sudoers rule
 * from application.properties (scyed.environment.zfs-binary) set up.
 */
@Disabled("Manual diagnostic - runs real sudo/zfs commands against the host, not part of CI")
class ZFSCommandEnvironmentDiagnosticTest {

    private val zfsBinary = "/usr/sbin/zfs"
    private val testDataset = "blink/clu/zfs-diag-test"

    private data class Result(val exitCode: Int, val stdout: String, val stderr: String)

    /** Mirrors ZFSManager.execute(): same ProcessBuilder setup, same env vars. */
    private fun run(command: List<String>, env: Map<String, String> = emptyMap()): Result {
        println("\$ ${command.joinToString(" ")}  (env: $env)")
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .apply { environment().putAll(env) }
            .start()
        val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
        process.waitFor()
        val result = Result(process.exitValue(), stdout.trim(), stderr.trim())
        println("  exit=${result.exitCode} stdout='${result.stdout}' stderr='${result.stderr}'")
        return result
    }

    @Test
    fun `jvm process itself runs as`() {
        run(listOf("id"))
    }

    @Test
    fun `sudo id with SUDO_UID SUDO_GID SUDO_USER env vars set beforehand - exactly what ZFSManager execute() does`() {
        val result = run(
            listOf("/usr/bin/sudo", "id"),
            mapOf("SUDO_UID" to "1001", "SUDO_GID" to "1001", "SUDO_USER" to "scyed")
        )
        // This is expected to print uid=0(root): the env vars have no effect on sudo's
        // choice of target user, which is why datasets end up owned by root.
        println("^ note: still uid=0 despite SUDO_UID=1001 - this is the bug")
        check(result.stdout.contains("uid=0")) { "expected sudo to still run as root here" }
    }

    @Test
    fun `full environment actually visible inside the sudo child process`() {
        run(
            listOf("/usr/bin/sudo", "env"),
            mapOf("SUDO_UID" to "1001", "SUDO_GID" to "1001", "SUDO_USER" to "scyed", "MY_CUSTOM_VAR" to "hello")
        )
        // Ubuntu/Debian sudoers ships with `env_reset` (see: sudo -l), so most of the parent
        // environment - including any custom vars - is stripped before the command runs.
    }

    @Test
    fun `sudo -u target user - the correct way to run zfs as a specific uid_gid`() {
        run(listOf("/usr/bin/sudo", "-u", "#1001", "-g", "#1001", "id"))
    }

    @Test
    fun `create a real dataset the way ZFSManager does and inspect who ends up owning it`() {
        run(listOf("/usr/bin/sudo", zfsBinary, "destroy", "-f", testDataset)) // best-effort cleanup from a previous run
        try {
            run(
                listOf("/usr/bin/sudo", zfsBinary, "create", testDataset),
                mapOf("SUDO_UID" to "1001", "SUDO_GID" to "1001", "SUDO_USER" to "scyed")
            )
            val mountpoint = run(
                listOf("/usr/bin/sudo", zfsBinary, "get", "-H", "-o", "value", "mountpoint", testDataset)
            ).stdout
            val owner = run(listOf("stat", "-c", "%U:%G", mountpoint))
            println("dataset '$testDataset' mounted at '$mountpoint' is owned by: ${owner.stdout}")
        } finally {
            run(listOf("/usr/bin/sudo", zfsBinary, "destroy", "-f", testDataset))
        }
    }
}
