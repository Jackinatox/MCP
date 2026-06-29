# CLU

A Spring Boot (Kotlin) control plane that provisions and runs game servers as Docker
containers — a lightweight Pterodactyl/Pelican-style backend. It owns the server
lifecycle (install → start → stop → kill → delete), streams live console output over
WebSockets, and persists state to a local SQLite database.

The frontend (a SPA) lives in `webapp/` and is served by the backend at `/v1/webApp`.

## How it works

### Glyphs (server templates)

A **Glyph** is the template for a game server, modelled on the Pterodactyl/Pelican
`PTDL_v2` "egg" schema (`Glyph.kt`). On boot, `FileSystemGlyphProvider` scans
`scyed.eggs.directory` (`./eggs/*.json`), deserializes each file, and persists any new
ones as `GlyphEntity` rows. A Glyph carries the Docker image(s), the install script +
install container, the startup command, and a list of `{{VAR}}` environment variables.
`renderStartup()` substitutes those placeholders with the server's env overrides.

### Server lifecycle & provisioning

A `ServerEntity` is a configured instance of a Glyph (image, CPU%, memory, env, start
command, and a `ServerState`). `ServerService` is the entrypoint for all lifecycle
operations; the heavy lifting runs in `DockerProvisioner` and `ContainerService` (via
the `docker-java` client).

- **Create / Reinstall** — `ServerService` publishes a Spring `ServerReinstallRequested`
  application event. `DockerProvisioner.reInstallServer` handles it **asynchronously**
  (`@Async("provisioningExecutor")`, a dedicated `ThreadPoolTaskExecutor`): it writes the
  install script, spins up a throwaway install container (auto-removed), streams its logs
  to `install.log`, waits for exit, and forces the server to `STOPPED`.
- **Start** — creates (or restarts) the game container with resource limits, a read-only
  rootfs, `no-new-privileges`, a non-root user, and the game-files bind mount, then
  attaches to its stdio.
- **Stop / Kill / Delete** — detaches, kills/removes the container, and (on delete)
  recursively removes the server's storage directory.

Game files are bind-mounted from `scyed.gameserver.gameserver-storage/<serverId>/gameFiles`
into `/home/container`.

### The two event systems

CLU has **two distinct event layers** — don't confuse them:

1. **Spring application events** (`ApplicationEventPublisher`) — used only to kick off
   async work. `ServerReinstallRequested` is published by `ServerService` and consumed by
   `DockerProvisioner`. (`ServerDeleted` and `KillServerRequested` also exist here.)

2. **`ContainerEventBus`** — a custom in-memory pub/sub for runtime container signals
   (`ContainerEvent`: `ConsoleLine`, `ServerStats`, `ServerStatusChanged`, `Detached`).
   Listeners subscribe per-`serverId` **or** to a well-known `GLOBAL` id; `publish()`
   fans out to both. Two `@PostConstruct` handlers subscribe to `GLOBAL`:
   - `WSEventPump` — forwards `ConsoleLine` and `ServerStatusChanged` to connected
     WebSocket clients.
   - `ServerStatusChangedPersitence` — writes status changes back to the database.

   Events reach the bus from two places: `ServerStateTransitions` (which validates the
   state-machine transition in `ServerState` and then publishes `ServerStatusChanged`),
   and `ContainerAttachmentManager`.

### WebSocket console & container attachment

The console socket is registered at `/v1/ws/console/{serverId}` (`WebSocketConfig`).
`ConsoleWSHandler` extracts the `serverId` from the path and registers/unregisters the
session with `WSEventPump`, which keeps a `serverId → sessions` map.

The other half is `ContainerAttachmentManager`. When a container starts, it attaches to
the container's stdout/stderr stream and a `waitContainer` callback:

- The **stream callback** buffers bytes, splits them into lines, and publishes each line
  as a `ContainerEvent.ConsoleLine`. `WSEventPump` serializes these to `ConsoleMessage`
  JSON and pushes them to every session for that server.
- The **wait callback** fires when the container exits, publishing `ServerStatusChanged`
  with `STOPPED` (exit 0) or `CRASHED` (non-zero).
- `stopContainer` deliberately **detaches before killing**, so the explicit
  `STOPPED` transition from the caller isn't raced by the wait callback reporting
  `CRASHED` from the SIGKILL.

WebSocket messages are a discriminated union (`WsMessage`, Jackson `@JsonTypeInfo` on a
`type` field): `console_output`, `server_stats`, `status_changed`.

> **Note on event coverage:** event publishing is currently incomplete and the
> propagation is still being worked on. Notably, `StatsMessage`/`ContainerEvent.ServerStats`
> is defined but never published, the `STARTED` status is never emitted (no readiness
> listener exists yet), and some Spring events have no consumers. Treat the live status of
> a running server as best-effort until this is finished.

### Startup reconciliation

On boot, `StartupCheckRunner` runs all `StartupCheck`s. `ContainerStateStartupCheck`
reconciles every server in the DB against Docker reality: containers found mid-install are
removed and marked `ERROR`; running containers become `STARTED`; missing/stopped ones are
marked `STOPPED` with their `containerId` cleared.

## API

Served under the versioned path prefix `/v1` (configurable; `v1` is the default).

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/v1/server` | List all servers (with count) |
| `POST` | `/v1/server` | Create a server (triggers install) |
| `POST` | `/v1/server/{serverId}/power?action=START\|STOP\|KILL` | Power action |
| `POST` | `/v1/server/{serverId}/reinstall` | Reinstall (`forceStop` to stop first) |
| `DELETE` | `/v1/server/{serverId}` | Delete server + its files |
| `GET` | `/v1/glyph` | List glyph summaries |
| `GET` | `/v1/glyph/{glyphId}` | Get a glyph |
| `WS` | `/v1/ws/console/{serverId}` | Live console / status stream |
| `GET` | `/v1/webApp`, `/v1/webApp/**` | Frontend SPA |

OpenAPI/Swagger UI is available via springdoc, and the H2 console + Actuator endpoints are
exposed.

## Running

1. Import the project into IntelliJ IDEA and run the Spring Boot application
   (`McpApplication`). Requires a reachable Docker daemon.
2. Build the frontend so it's served at `/v1/webApp` — from the `webapp/` directory:

   ```sh
   bun i
   bun run build
   ```

## Configuration

Key properties (`src/main/resources/application.properties`):

| Property | Purpose |
| --- | --- |
| `spring.datasource.url` | SQLite database location (`./database.db`) |
| `scyed.eggs.directory` | Where Glyph/egg JSON files are loaded from |
| `scyed.gameserver.gameserver-storage` | Per-server game files root |
| `scyed.gameserver.installTemp` | Scratch dir for install scripts/logs |
| `scyed.gameserver.user-uid` / `user-gid` | UID/GID containers run as |

## Stack

Kotlin 2.2 · Spring Boot 4 (Web, Data JPA, WebSocket, Actuator) · docker-java · SQLite
(Hibernate community dialect) · springdoc-openapi. Frontend: see `webapp/`.
