# Recommended Package Structure

The current structure groups files by technology (`docker/`, `jpa/`). The recommended structure
groups by **domain** instead. This makes it obvious where to add new code and avoids
the sprawl of stuffing all Docker logic in one package regardless of what it does.

The root package changes from `com.scyed.mcp` to `com.scyed.clu` to reflect that this is
the CLU service, not the MCP.

---

## Full Tree

```
com.scyed.clu/
│
├── CluApplication.kt
│
├── api/                                  REST layer only — no business logic
│   ├── ServerController.kt
│   ├── FileController.kt                 (phase 2)
│   ├── GlobalExceptionHandler.kt         absorbs TestController's @ExceptionHandler
│   └── dto/
│       ├── CreateServerRequest.kt
│       ├── ReinstallServerRequest.kt
│       ├── PowerActionRequest.kt
│       └── ServerResponse.kt
│
├── server/                               core domain: gameserver lifecycle
│   ├── ServerEntity.kt
│   ├── ServerStatus.kt
│   ├── ServerService.kt                  orchestration: create, reinstall, power — no direct repo calls from controller
│   ├── ServerRepository.kt
│   └── event/
│       ├── ServerReinstallRequested.kt
│       ├── ServerPowerRequested.kt
│       └── PowerAction.kt
│
├── glyph/                                game definitions (eggs)
│   ├── Glyph.kt                          domain model (unchanged)
│   ├── GlyphEntity.kt
│   ├── GlyphRepository.kt
│   ├── GlyphMappers.kt                   toDto() / toEntity() extension functions
│   ├── GlyphService.kt                   load, refresh, validate
│   ├── GlyphProvider.kt                  interface
│   ├── FileSystemGlyphProvider.kt
│   ├── GlyphEnvVarValidator.kt           moved from docker/stuff/
│   └── EggProperties.kt                  moved from game/FileSystemGlyphProvider.kt
│
├── provisioning/                         Docker container lifecycle
│   ├── DockerProvisioner.kt              merge of ServerProvisioner + DockerProvisioningService
│   └── InstallScriptBuilder.kt           extract: writes install.sh to temp dir
│
├── network/                              port allocation + nftables  (phase 2)
│   ├── PortAllocator.kt                  tracks which ports in a slot's range are assigned
│   ├── PortEntity.kt                     persisted port assignment
│   ├── PortRepository.kt
│   ├── NftablesManager.kt                shells out to `nft` for DNAT add/remove
│   └── NetworkMode.kt                    enum: DEDICATED_IP, SHARED_IP
│
├── storage/                              ZFS + backups  (phase 2)
│   ├── ZfsManager.kt                     shells out to `zfs` — create, quota, snapshot, send, recv
│   ├── DatasetPath.kt                    value type: tank/gameservers/{pool}/{server-id}
│   ├── backup/
│   │   ├── BackupTarget.kt               pluggable interface: store(snapshot, meta), restore(id, path)
│   │   ├── LocalBackupTarget.kt
│   │   └── S3BackupTarget.kt             (phase 4)
│   └── snapshot/
│       ├── SnapshotService.kt
│       └── SnapshotEntity.kt
│
├── console/                              WebSocket live console  (phase 2)
│   ├── ConsoleWebSocketConfig.kt
│   ├── ConsoleWebSocketHandler.kt        attaches to container stdout/stderr, pumps to WS session
│   └── DockerLogStreamer.kt
│
├── files/                                file management API  (phase 2)
│   ├── FileService.kt                    read/write/list/delete under gameserver data dir
│   └── DenylistFilter.kt                 enforces glyph fileDenylist
│
├── sync/                                 MCP communication  (phase 3)
│   ├── McpClient.kt                      REST client to MCP (RestClient + mTLS or API key)
│   ├── SyncService.kt                    pulls revision-based glyph updates from MCP
│   ├── HeartbeatService.kt               @Scheduled every 30s: POST health + resource usage to MCP
│   └── SyncRevision.kt                   value type holding current local revision number
│
├── startup/                              startup validation (keep existing shape)
│   ├── StartupCheck.kt
│   ├── StartupCheckRunner.kt
│   ├── FilesystemStartupCheck.kt         renamed from GameserverFilesystemStartupCheck
│   └── DockerStartupCheck.kt             new: verify Docker daemon reachable at boot
│
└── infra/                                cross-cutting infrastructure wiring
    ├── docker/
    │   └── DockerClientConfig.kt         renamed from DockerConfig
    ├── async/
    │   └── AsyncConfig.kt                fixed: remove dead outer class wrapper
    └── persistence/
        └── converter/
            ├── EnvMapConverter.kt
            ├── EggVariableListConverter.kt
            └── StringListConverter.kt
```

---

## Key Decisions and Reasons

### Domain packages instead of technology packages
`server/`, `glyph/`, `network/`, `storage/`, `console/`, `files/`, `sync/` — each owns its
entity, repository, service, and events. You know where to look and where to add.

### `api/` is a pure translation layer
Controllers only: parse HTTP request → call service → serialize response. No direct repository
calls, no event publishing, no entity construction. That all moves to `ServerService`.

### Merge `DockerProvisioningService` + `ServerProvisioner` into `DockerProvisioner`
The interface (`Provisioning`) and its stub implementation (`DockerProvisioningService`) exist
alongside the real implementation (`ServerProvisioner`). Delete the interface and stub; keep
and rename `ServerProvisioner` → `DockerProvisioner`.

### Events live next to their domain
`server/event/` rather than scattered inside `ServerProvisioner` as nested classes. Makes them
importable from anywhere without pulling in all of `ServerProvisioner`.

### `infra/` holds wiring-only code
Docker client factory, JPA converters, async executor — none of these are domain concepts.
Grouping them in `infra/` signals "plumbing, not business logic".

### Root package rename: `com.scyed.mcp` → `com.scyed.clu`
Avoids confusion when the actual MCP service is built as a separate project. Update
`application.properties` logging level accordingly.
