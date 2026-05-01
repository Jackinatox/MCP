package com.scyed.clu.startup

import com.scyed.clu.provisioning.GameserverProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class GameserverFilesystemStartupCheck(
    private val properties: GameserverProperties,
) : StartupCheck {
    override val name: String = "gameserver-filesystem"

    override fun runCheck() {
        val installTemp = ensureDirectory(properties.installTemp.toAbsolutePath().normalize(), "installTemp")
        val gameserverStorage = ensureDirectory(properties.gameserverStorage.toAbsolutePath().normalize(), "gameserverStorage")

        validateUnixOwnership(installTemp, "installTemp")
        validateUnixOwnership(gameserverStorage, "gameserverStorage")
    }

    private fun ensureDirectory(path: Path, label: String): Path {
        Files.createDirectories(path)
        require(Files.isDirectory(path)) { "$label path is not a directory: $path" }
        require(Files.isWritable(path)) { "$label path is not writable: $path" }
        return path
    }

    private fun validateUnixOwnership(path: Path, label: String) {
        val uid = Files.getAttribute(path, "unix:uid") as Number
        val gid = Files.getAttribute(path, "unix:gid") as Number
        require(uid.toLong() == properties.userUid) {
            "$label path $path must be owned by uid ${properties.userUid}, found ${uid.toLong()}"
        }
        require(gid.toLong() == properties.userGid) {
            "$label path $path must be owned by gid ${properties.userGid}, found ${gid.toLong()}"
        }
    }
}
