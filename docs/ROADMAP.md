# CLU Implementation Roadmap

Phases are ordered by dependency: each phase assumes the previous is done.
Phase 1 is a prerequisite for everything else — don't add new features before the cleanup.

---

## Phase 1 — Cleanup and Restructure

No new features. Get the existing code into a state worth building on.

### Tasks
- [ ] Rename root package `com.scyed.mcp` → `com.scyed.clu`
- [ ] Apply the full package structure from `PACKAGE_STRUCTURE.md`
- [ ] Delete `Provisioning` interface and `DockerProvisioningService`; rename `ServerProvisioner` → `DockerProvisioner`
- [ ] Fix `AsyncConfig`: delete the dead outer wrapper class, annotate only the inner `@Configuration`
- [ ] Move `GlyphEnvVarValidator` from `docker/stuff/` to `glyph/`
- [ ] Extract `InstallScriptBuilder` from `DockerProvisioner` (separate class, no logic change)
- [ ] Create `ServerService`: move orchestration logic out of `ServerController`
  - server creation (entity build, event publish)
  - reinstall (kill + event publish)
  - power action (event publish)
- [ ] Replace `TestController` with `GlobalExceptionHandler` (`@ControllerAdvice`)
- [ ] Implement `STOP`, `RESTART`, `KILL` power actions in `DockerProvisioner`
  - STOP: `docker stop <id>` (graceful, uses `EggConfig.stop` signal), set status STOPPED
  - KILL: `docker kill <id>`, set status STOPPED
  - RESTART: STOP → START
- [ ] Move event classes (`ServerReinstallRequested`, `ServerPowerRequested`, `PowerAction`) to `server/event/`
- [ ] Move `EggProperties` + `EggConfigurationFS` out of `FileSystemGlyphProvider.kt` into their own file
- [ ] Move `GameserverProperties` + `EggConfiguration` out of `ServerProvisioner.kt` into their own file
- [ ] Update `application.properties`: rename logging level key to `com.scyed.clu`

---

## Phase 2 — Core CLU Feature Completeness

After this phase the CLU is usable as a standalone single-node gameserver host.

### 2a — Port Allocation
- [ ] `PortEntity`: persisted port assignment (serverId, port number, protocol)
- [ ] `PortRepository`
- [ ] `PortAllocator`: allocate N ports from a configured range; release on server delete/stop
- [ ] Wire into server creation: assign ports before container start, pass as env vars / port bindings
- [ ] `NetworkMode` enum: `DEDICATED_IP`, `SHARED_IP`

### 2b — nftables DNAT (shared IP mode)
- [ ] `NftablesManager`: shell wrapper around `nft` commands
  - `addDnat(hostPort, containerIp, containerPort)`
  - `removeDnat(hostPort)`
  - `listRules(): List<NftRule>`
- [ ] Wire into server start/stop: add rules on start, remove on stop
- [ ] Startup check: verify `nft` binary is available and user has permission

### 2c — ZFS Dataset Management
- [ ] `ZfsManager`: shell wrapper around `zfs` commands
  - `createDataset(path: DatasetPath, refquotaGb: Long)`
  - `destroyDataset(path: DatasetPath)`
  - `setRefquota(path: DatasetPath, bytes: Long)`
  - `snapshot(path: DatasetPath, tag: String): SnapshotName`
  - `destroySnapshot(name: SnapshotName)`
  - `listSnapshots(path: DatasetPath): List<SnapshotName>`
- [ ] `DatasetPath` value type: `tank/gameservers/{pool}/{serverId}`
- [ ] Replace current plain-directory gamefiles with ZFS dataset mounts
- [ ] Startup check: verify ZFS pool exists and is healthy

### 2d — WebSocket Console
- [ ] Add Spring WebSocket dependency (`spring-boot-starter-websocket`)
- [ ] `ConsoleWebSocketConfig`: register handler at `/v1/server/{id}/console`
- [ ] `ConsoleWebSocketHandler`: on connect, attach Docker log stream to session; on disconnect, detach
- [ ] `DockerLogStreamer`: wraps `docker.logContainerCmd` + `docker.attachContainerCmd` for live I/O
- [ ] Input path: receive text frames from WS, write to container stdin
- [ ] Output path: stream container stdout/stderr to WS session

### 2e — File Management API
- [ ] `FileService`:
  - `list(serverId, relativePath): List<FileEntry>` — directory listing
  - `read(serverId, relativePath): InputStream`
  - `write(serverId, relativePath, content: InputStream)`
  - `delete(serverId, relativePath)`
  - `rename(serverId, from, to)`
- [ ] `DenylistFilter`: rejects paths matching the glyph's `fileDenylist` patterns
- [ ] `FileController`: REST endpoints under `/v1/server/{id}/files`
- [ ] Path traversal protection: normalize and assert path stays within server data dir

---

## Phase 3 — MCP Synchronization

Makes the CLU a participant in the two-tier architecture rather than a standalone node.

### 3a — Heartbeat
- [ ] `HeartbeatService`: `@Scheduled` every 30s
  - Collect: CPU usage %, memory usage MB/total, disk usage per dataset, running server count
  - POST to MCP `/v1/clu/{cluId}/heartbeat`
  - Track last-successful-heartbeat timestamp; log warnings if MCP unreachable
- [ ] `CluProperties`: configurable MCP base URL, CLU ID, API key / cert path

### 3b — MCP Client
- [ ] `McpClient`: Spring `RestClient` wrapper
  - Auth: API key header (phase 3) or mTLS (phase 5)
  - `getCurrentRevision(): Long`
  - `getGlyphsSince(revision: Long): List<GlyphDto>`
  - `postHeartbeat(payload: HeartbeatPayload)`
- [ ] `SyncRevision`: persisted (SQLite/H2) current revision number

### 3c — Revision-Based Glyph Sync
- [ ] `SyncService`: called at startup and after each heartbeat
  - Fetch MCP revision; compare to local `SyncRevision`
  - If behind: fetch new/updated glyphs, upsert into local `GlyphRepository`
  - Update `SyncRevision` on success
- [ ] Replace `FileSystemGlyphProvider` as the authoritative source when MCP is configured
  (filesystem provider stays as fallback / dev mode)

### 3d — READY Gate
- [ ] CLU refuses CREATE and START operations until initial sync completes
- [ ] Implement as a `StartupCheck` or a flag checked in `ServerService`
- [ ] Config flag: `scyed.clu.sync.required=true` (default true; false allows dev mode without MCP)

---

## Phase 4 — Backups and Migration

### 4a — Snapshot Service
- [ ] `SnapshotEntity`: id, serverId, datasetPath, tag, createdAt, sizeBytes, status
- [ ] `SnapshotRepository`
- [ ] `SnapshotService`:
  - `create(serverId)`: ZFS snapshot, persist entity
  - `delete(snapshotId)`: ZFS destroy snapshot, remove entity
  - `list(serverId)`: query repository
  - `restore(snapshotId)`: ZFS rollback (server must be STOPPED)

### 4b — Pluggable Backup Targets
- [ ] `BackupTarget` interface:
  - `store(snapshotPath: Path, meta: BackupMeta): BackupId`
  - `restore(backupId: BackupId, targetPath: Path)`
  - `delete(backupId: BackupId)`
  - `list(serverId: UUID): List<BackupMeta>`
- [ ] `LocalBackupTarget`: ZFS send → compressed file in configured directory
- [ ] `S3BackupTarget`: ZFS send piped to S3 multipart upload (AWS SDK)
- [ ] Config: `scyed.clu.backup.target=local|s3` with target-specific properties

### 4c — Migration Support
- [ ] Implement `TRANSFERING_LOCKED` state handling in `ServerService`
  - Lock: set status, refuse power actions and config changes
  - Unlock: set status back (IDLE or STOPPED)
- [ ] `MigrationService`:
  - `sendStream(serverId): InputStream` — ZFS send stream for MCP to pipe to destination CLU
  - `receiveStream(serverId, stream: InputStream)` — ZFS receive from source CLU
- [ ] REST endpoints for MCP to call: `POST /internal/server/{id}/migrate/send` and `/receive`
- [ ] Separate auth for internal MCP↔CLU endpoints vs. frontend-facing endpoints

---

## Phase 5 — Security and Hardening

- [ ] **JWT validation**: validate tokens on all frontend-facing endpoints; MCP issues them, CLU validates
  - Add `spring-boot-starter-security` + `spring-security-oauth2-resource-server`
  - Configure JWT public key / JWKS endpoint from MCP
- [ ] **API key auth for MCP↔CLU internal endpoints**: separate filter chain on `/internal/**`
- [ ] **mTLS** (optional upgrade from API key): configure Spring Boot TLS with client cert
- [ ] **Rate limiting**: bucket4j or Spring filter; per-IP and per-server-id limits on file and console endpoints
- [ ] **Cgroup v2 startup check**: verify `--cgroup-parent` flag works with current Docker + kernel
  - Create a test container with `--cgroup-parent`, verify it starts; abort boot if not
- [ ] **File size limits**: `DenylistFilter` extended to reject writes above a configured max
- [ ] **Max nftables rules guardrail**: `NftablesManager` refuses to add rules beyond a configured cap
- [ ] **Readonly rootfs validation**: confirm game containers still work with `withReadonlyRootfs(true)`

---

## Phase 6 — MCP Service (separate project)

New Spring Boot project: `com.scyed.mcp`, PostgreSQL-backed. The CLU does not change here
except to consume the MCP's heartbeat and sync APIs.

### Scope
- CLU registry: receive heartbeats, track per-CLU health and resource usage
- Server slot management: create/delete/migrate with CLU selection (load + tier)
- Global port/IP uniqueness: allocate port ranges across CLUs, prevent conflicts
- Glyph distribution: authoritative egg store, monotonically increasing revision counter
- Frontend API: server location lookup, create/delete/migrate only
- JWT issuance: short-lived tokens for frontend → CLU direct access
- Migration orchestration: tell source CLU to send, tell destination CLU to receive, update routing

---

## Dependency Graph

```
Phase 1 (cleanup)
    └── Phase 2a (ports)
    └── Phase 2b (nftables) — depends on 2a
    └── Phase 2c (ZFS)
    └── Phase 2d (console)
    └── Phase 2e (files)
Phase 3a (heartbeat) — can start after Phase 1
Phase 3b (MCP client) — depends on 3a
Phase 3c (sync) — depends on 3b
Phase 3d (READY gate) — depends on 3c
Phase 4a (snapshots) — depends on 2c (ZFS)
Phase 4b (backup targets) — depends on 4a
Phase 4c (migration) — depends on 4a + 3b
Phase 5 (security) — can overlay any phase; JWT/mTLS needed before production
Phase 6 (MCP) — independent new project; CLU phases 3a-3d must be done first
```
