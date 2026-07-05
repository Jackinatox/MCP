package com.scyed.clu.infra.zfs

import com.scyed.clu.db.entity.PodEntity
import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.infra.properrties.EnvironmentProperties
import com.scyed.clu.infra.properrties.GameserverProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Service
class ZFSManager(val ep: EnvironmentProperties, private val gameserverProperties: GameserverProperties) {
    private val log = LoggerFactory.getLogger(javaClass)


    fun createPodDataset(podEntity: PodEntity): ZFSCommandResult {
        val fullName = buildPodDataset(podEntity)
        log.info("Creating pod dataset '$fullName'")
        val result = execute(listOf("create", "-o", "quota=${podEntity.diskMb}m", fullName))
        if (result.isSuccess) chownDataset(fullName)
        return result
    }

    fun createServerDataset(server: ServerEntity): ZFSCommandResult {
        val fullName = buildServerDatasetName(server)
        log.info("Create server dataset '$fullName'")

        val newCall = buildList {
            add("create")
            if (server.diskMb > 0) addAll(listOf("-o", "quota=${server.diskMb}m"))
            add(fullName)
        }

        val result = execute(newCall)
        if (result.isSuccess) chownDataset(fullName)
        return result
    }

    /**
     * `zfs create` mounts the new dataset as root regardless of what's in the process
     * environment (sudo only honors `-u`/sudoers, not SUDO_UID/SUDO_GID), so the mountpoint
     * always comes back owned by root. Fix it up explicitly afterwards.
     */
    private fun chownDataset(fullName: String) {
        val mountpoint = execute(listOf("get", "-H", "-o", "value", "mountpoint", fullName)).stdout.trim()
        if (mountpoint.isBlank() || mountpoint == "none" || mountpoint == "legacy" || mountpoint == "-") {
            log.warn("Dataset '$fullName' has no mountpoint ('$mountpoint'), skipping chown")
            return
        }

        val owner = "${gameserverProperties.userUid}:${gameserverProperties.userGid}"
        log.info("Chowning '$mountpoint' to $owner")
        val chown = runCommand(listOf("/usr/bin/sudo", "chown", owner, mountpoint))
        if (!chown.isSuccess) {
            log.error("Failed to chown '$mountpoint' to $owner: {}", chown.stderr.trim())
        }
    }

    fun destroyServerDataset(serverEntity: ServerEntity): ZFSCommandResult {
        val fullName = buildServerDatasetName(serverEntity)
        log.info("Destroying server dataset '$fullName'")
        val softDelete = execute(listOf("destroy", "-f", fullName))

        if (softDelete.isSuccess) return softDelete
        log.warn("Soft deletion for dataset '$fullName' failed, using force")

        return execute(listOf("destroy", "-f", fullName))
    }

    private fun buildServerDatasetName(serverEntity: ServerEntity): String =
        "${buildPodDataset(serverEntity.podEntity)}/${serverEntity.id}"

    private fun buildPodDataset(pod: PodEntity): String =
        "${ep.gmStorageDataset}/${pod.datasetName}"


    private fun applyQuota(datasetName: String, sizeMb: Double): ZFSCommandResult {
        log.info("Setting Quota for $datasetName to ${sizeMb}mb")
        val applyQuota = execute(listOf("set", "quota=${sizeMb}m", datasetName))
        return applyQuota
    }


    private val streamReaderExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
    private fun execute(args: List<String>, timeout: Duration = 10.seconds): ZFSCommandResult =
        runCommand(listOf("/usr/bin/sudo", ep.zfsBinary) + args, timeout)

    private fun runCommand(command: List<String>, timeout: Duration = 10.seconds): ZFSCommandResult {
        log.info("Executing: {}", command.joinToString(" "))

        val start = System.nanoTime()
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        val stdoutFuture = streamReaderExecutor.submit<String> { drain(process.inputStream) }
        val stderrFuture = streamReaderExecutor.submit<String> { drain(process.errorStream) }

        val finished = process.waitFor(timeout.toLong(DurationUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            val stdout = try {
                stdoutFuture.get(500, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                "<not captured>"
            }
            val stderr = try {
                stderrFuture.get(500, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                "<not captured>"
            }

            log.error("ZFS command timed out: {}; {}", command.joinToString(" "), stderr)

            return ZFSCommandResult(command, process.exitValue(), stdout, stderr, (System.nanoTime() - start) / 1000000)
        }

        val stdout = stdoutFuture.get()
        val stderr = stderrFuture.get()
        val result =
            ZFSCommandResult(command, process.exitValue(), stdout, stderr, (System.nanoTime() - start) / 1000000)

        if (result.isSuccess) {
            log.debug("OK ({}ms): {}", result.durationMs, result.commandLine())
        } else {
            log.error(
                "FAILED exit={} ({}ms): {} | stderr: {}",
                result.exitCode, result.durationMs, result.commandLine(), stderr.trim()
            )
        }
        return result
    }

    private fun drain(stream: InputStream): String =
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
}


