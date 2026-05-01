# CLU — Current Progress Overview

This codebase is the **CLU (Codified Likeness Utility)** implementation — the per-host worker node
in the two-tier gameserver hosting platform. Despite the project name `com.scyed.clu`, all
functionality here is CLU-scope: Docker container lifecycle, local gameserver management, and
egg/glyph definitions. The MCP (Master Control Program) does not exist yet and will be a
separate service.

---

## What Is Implemented

### Core REST API (`ServerController`)
- `GET /v1/server` — list all servers with count
- `POST /v1/server` — create server (validates name uniqueness, glyph existence, env vars)
- `POST /v1/server/{id}/reinstall` — force-stop + reinstall
- `POST /v1/server/{id}/power` — power action endpoint (partially working, see below)

### Glyph / Egg System
- Full Pterodactyl PTDL_v2 format parsed into `Glyph` domain model
- Filesystem-based loader (`FileSystemGlyphProvider`): scans `*.json` files at startup
- New glyphs auto-persisted to DB on load; existing ones skipped by name
- `{{VAR}}` placeholder substitution in startup commands (`Glyph.renderStartup`)
- `GlyphEnvVarValidator`: rejects unknown variables, rejects missing required ones
- Full JPA entity (`GlyphEntity`) with JSON-serialized complex fields

### Docker Provisioning (`ServerProvisioner`)
- Install flow: writes install script to temp dir → creates Docker container with resource limits
  (CPU %, memory MB) → runs install → streams logs to file → updates DB status
- Start flow: renders startup command → creates game container → starts it
- `killAndRemoveServer`: forcefully kills + removes container, clears `containerId`, sets STOPPED
- Security opts: `no-new-privileges` on all containers
- Configurable uid/gid for container user

### Data Persistence
- `ServerEntity`: name, status, containerId, image, env map, startCommand, resource limits, glyph FK
- `GlyphEntity`: full egg definition persisted to H2 embedded database
- `ServerStatus` enum: `PROVISIONING, INSTALLING, IDLE, RUNNING, STOPPED, ERROR, TRANSFERING_LOCKED`
- JSON converters for `Map<String, String>`, `List<String>`, `List<EggVariable>`

### Event-Driven Architecture
- `ServerReinstallRequested` → async `@EventListener` in `ServerProvisioner`
- `ServerPowerRequested` → async `@EventListener` in `ServerProvisioner`
- Thread pool executor: core=4, max=8, queue=100 (`provisioningExecutor`)

### Startup Validation
- `StartupCheckRunner`: runs all `StartupCheck` beans in order at boot, fails fast
- `GameserverFilesystemStartupCheck`: ensures `installTemp` and `gameserverStorage` dirs exist,
  are writable, and are owned by the configured uid/gid

---

## What Is Incomplete

| Area | Status | Notes |
|---|---|---|
| Power actions | Partial | Only `START` works; `STOP`, `RESTART`, `KILL` throw `RuntimeException` |
| Provisioner duplication | Broken | `DockerProvisioningService` is all stubs; `ServerProvisioner` has real logic; one must be deleted |
| Port allocation | Missing | No port range tracking or assignment |
| nftables DNAT | Missing | No shared-IP port forwarding rules |
| ZFS datasets | Missing | No dataset creation, refquota, or snapshot management |
| Backups | Missing | No backup target interface or implementation |
| Migration | Missing | `TRANSFERING_LOCKED` status exists but has no handler |
| WebSocket console | Missing | No live console endpoint |
| File management API | Missing | No file read/write/list/delete endpoints |
| MCP sync | Missing | No heartbeat, no revision-based glyph sync |
| Authentication | Missing | No JWT validation; all endpoints are open |
| `docker/stuff/` package | Bad | `GlyphEnvVarValidator` is misplaced here |
| `TestController` | Should be removed | Its exception handler should move to a global handler |
| `AsyncConfig` | Broken | Outer class wraps inner class with the same name — only the inner is a `@Configuration` |

---

## Structural Problems

1. **Two competing provisioner implementations** — `Provisioning` interface + `DockerProvisioningService`
   (all stubs/dummies) coexists with `ServerProvisioner` (actual logic). The interface and service
   should be deleted or merged.

2. **Package `docker/stuff/`** — not a meaningful package name. `GlyphEnvVarValidator` belongs in
   the `glyph` domain.

3. **`TestController`** — mixes a diagnostics endpoint with a global exception handler. The handler
   belongs in a dedicated `@ControllerAdvice` class; the diagnostics endpoint should be removed or
   kept only as a dev tool.

4. **Package root is `com.scyed.clu`** — since this is the CLU, the root should be
   `com.scyed.clu` to avoid confusion when the real MCP service is built.

5. **`AsyncConfig` nesting bug** — the outer `class AsyncConfig` is not annotated and wraps
   `@Configuration class AsyncConfig` (same name). Only the inner class matters; the outer is dead code.

6. **Business logic in controller** — `ServerController` directly calls `provisioner.killAndRemoveServer`
   and does entity construction. These belong in a service layer.
