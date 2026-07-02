package com.scyed.clu.infra.properrties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "scyed.gameserver")
data class GameserverProperties(
    val installTemp: Path = Paths.get("leck"),
    val userUid: Long = 1001,
    val userGid: Long = 1001,
)
