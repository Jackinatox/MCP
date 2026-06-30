package com.scyed.clu.infra.zfs

import com.scyed.clu.db.entity.PodEntity
import com.scyed.clu.infra.properrties.EnvironmentProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Service
class ZFSManager(val ep: EnvironmentProperties) {
    private val log = LoggerFactory.getLogger(javaClass)


    fun createPodDataset(podEntity: PodEntity): ZFSCommandResult {
        val fullName = "${ep.gmStorageDataset}/${podEntity.datasetName}"
        log.info("Creating pod dataset '$fullName'")
        val poolCreation = execute(listOf("create", fullName))

        if (!poolCreation.isSuccess) return poolCreation

        return applyQuota(fullName, podEntity.diskMb)
    }

    private fun applyQuota(datasetName: String, sizeMb: Double): ZFSCommandResult {
        log.info("Setting Quota for $datasetName to ${sizeMb}mb")
        val applyQuota = execute(listOf("set", "quota=${sizeMb}m", datasetName))
        return applyQuota
    }


    private val streamReaderExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
    private fun execute(args: List<String>, timeout: Duration = 10.seconds): ZFSCommandResult {

        val command = listOf("/usr/bin/sudo", ep.zfsBinary) + args
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
        val result = ZFSCommandResult(command, process.exitValue(), stdout, stderr, (System.nanoTime() - start) / 1000000)

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


