# CLU Container Lifecycle Implementation Plan

This repository should be treated as the **CLU** from the proposal, not the central MCP.

The CLU owns local runtime state and is responsible for:

- creating and destroying game containers
- attaching to container stdout/stderr
- sending input to container stdin
- persisting server config and runtime metadata
- updating port mappings
- exposing APIs for the frontend or the central MCP

## 1. Goal

Implement a Spring Boot service with a core API shaped roughly like:

```java
String createServer(ServerConfig serverConfig)
```

The returned `String` should be the CLU-local server ID or UUID, not the Docker container ID.

The CLU should:

1. save desired state in the database
2. create the Docker container asynchronously
3. stream stdout/stderr into a console pipeline
4. allow stdin writes while the container is running
5. persist runtime metadata such as container ID, status, and active port mappings
6. recreate or reconfigure the container when mutable settings like port mappings change

## 2. Architectural Direction

Use a layered design instead of putting Docker logic directly in the controller.

Recommended layers:

- `api`: REST controllers and request/response DTOs
- `application`: orchestration services and use cases
- `domain`: server aggregate, status model, validation rules
- `persistence`: JPA entities and repositories
- `runtime/docker`: Docker client integration
- `runtime/console`: stdout listeners, stdin writers, session registry
- `events`: async lifecycle events

## 3. Core Domain Model

The current `Server` entity is not enough for CLU runtime ownership. Extend the model.

### `Server`

Store persistent desired state and runtime state:

- `id: UUID`
- `name`
- `description`
- `image`
- `status`
- `memoryMb`
- `cpuPercent`
- `env`
- `startupCommand`
- `workingDirectory`
- `containerId`
- `nodeName` or `hostName`
- `createdAt`
- `updatedAt`
- `lastStartedAt`
- `lastStoppedAt`
- `lastError`

### `ServerPortMapping`

Move port mappings into a separate table.

Fields:

- `id`
- `serverId`
- `hostIp` nullable
- `hostPort`
- `containerPort`
- `protocol` (`TCP`/`UDP`)
- `enabled`

Reason: one server may expose multiple ports, and port mappings change independently of the rest of the server config.

### `ServerConsoleLine` or log sink

Do **not** persist every console line by default unless you need replay/history.

Recommended approach:

- live console data stays in memory and is streamed to clients
- optionally persist a capped rolling history later

### `ServerStatus`

Expand the enum to reflect real lifecycle states:

- `PROVISIONING`
- `CREATING_CONTAINER`
- `STOPPED`
- `STARTING`
- `RUNNING`
- `STOPPING`
- `RESTARTING`
- `ERROR`
- `DELETING`

## 4. Public CLU API

Expose CLU-local endpoints for lifecycle and console operations.

Suggested endpoints:

- `POST /servers`
- `GET /servers/{id}`
- `POST /servers/{id}/start`
- `POST /servers/{id}/stop`
- `POST /servers/{id}/restart`
- `POST /servers/{id}/console/input`
- `GET /servers/{id}/console/stream`
- `PUT /servers/{id}/config`
- `PUT /servers/{id}/ports`

The MCP, if used later, should call these APIs instead of touching Docker directly.

## 5. `createServer` Flow

### Recommended contract

```java
UUID createServer(CreateServerCommand command)
```

`CreateServerCommand` should include:

- name
- image
- env overrides
- startup command or egg-derived startup template
- memory
- cpu
- port mappings
- optional install script behavior

### Flow

1. Validate the command.
2. Check local port conflicts.
3. Persist the `Server` row with status `PROVISIONING`.
4. Persist `ServerPortMapping` rows.
5. Publish an async event such as `ServerProvisionRequested(serverId)`.
6. Return the `serverId` immediately.
7. Handle the actual Docker creation in a background executor.

This keeps the HTTP request fast and avoids holding a transaction open while waiting on Docker.

## 6. Provisioning Pipeline

Implement provisioning as an application service plus async event handler.

### Components

- `ServerLifecycleService`
- `ServerProvisioningHandler`
- `DockerRuntimeService`
- `ConsoleSessionManager`

### Background provisioning steps

1. Load the saved server and mappings.
2. Mark status `CREATING_CONTAINER`.
3. Pull image if needed.
4. Create required filesystem paths or volumes.
5. Build the Docker `HostConfig`.
6. Apply CPU, memory, env, working dir, and port bindings.
7. Create the container.
8. Persist `containerId`.
9. Start the container.
10. Attach stdout/stderr streaming.
11. Create stdin writer session.
12. Mark status `RUNNING`.
13. On failure, store `lastError` and mark status `ERROR`.

## 7. Docker Integration Strategy

Keep Docker-specific code isolated behind an interface.

Suggested interface:

```java
interface ContainerRuntime {
    ContainerCreationResult create(ServerRuntimeSpec spec);
    void start(String containerId);
    void stop(String containerId);
    void remove(String containerId);
    AttachSession attach(String containerId);
    void sendInput(String containerId, String input);
    ContainerInspection inspect(String containerId);
}
```

This makes the application layer testable and keeps `docker-java` out of controllers and JPA code.

## 8. Stdout / Stderr Handling

The CLU needs a long-lived listener per running container.

### Recommended model

- attach to the container after start
- consume stdout/stderr on a dedicated executor
- fan out console frames to subscribers
- optionally keep a small ring buffer in memory for late subscribers

### Implementation notes

- use a `ConsoleSessionManager` keyed by `serverId`
- each session owns:
  - `serverId`
  - `containerId`
  - stdout listener task
  - stderr listener task or multiplexed frame handler
  - optional ring buffer
  - stdin writer handle

### Why not a raw thread?

Do not create unmanaged threads manually. Use:

- Spring `TaskExecutor`
- optionally `@Async`
- a dedicated executor bean for console streams

This gives lifecycle control and makes shutdown/recovery manageable.

## 9. Stdin Handling

You need a way to send commands like `stop`, `say hello`, or game-specific console commands.

### Recommended design

- keep a live input stream handle for each running server session
- expose `sendConsoleInput(serverId, line)`
- append `\n` before writing unless the protocol requires raw bytes

### Important constraint

stdin is runtime state, not database state.

Persist:

- `serverId`
- `containerId`
- server status

Do not persist:

- open streams
- thread references
- live console subscriptions

On CLU restart, inspect running containers and rebuild console sessions in memory.

## 10. Updating Server Configuration

Split configuration updates into two categories.

### Hot-updatable

Usually safe to update in place:

- descriptive metadata
- some env metadata if not yet started
- internal CLU labels

### Recreate-required

Usually requires stop/recreate/start:

- Docker published port bindings
- environment variables used at container start
- image
- startup command
- volume mounts
- memory and CPU limits in some cases depending on your runtime policy

### Recommended update flow

1. validate requested config
2. persist desired state
3. decide whether change is hot-applicable or recreate-required
4. if recreate-required:
   - stop container
   - remove old container
   - create new container from desired state
   - persist new `containerId`
5. restore status and console session

## 11. Port Mapping Strategy

The proposal mentions two possible models:

- Docker published ports (`-p`)
- external network rules such as `nftables`

For a first working version, use Docker port bindings because they are simpler.

### First implementation

- save mappings in `ServerPortMapping`
- validate no duplicate `hostPort/protocol` on the CLU
- create container with those bindings
- treat port changes as recreate-required

### Later optimization

If you need live remaps without container recreation:

- move external exposure into `nftables`
- keep container ports stable internally
- update NAT rules without recreating the container

That is more flexible, but not the best first milestone.

## 12. Persistence Rules

The CLU should be authoritative for its local runtime state.

Persist at minimum:

- server metadata
- desired config
- env overrides
- port mappings
- status
- container ID
- last known error
- timestamps

Do not persist:

- executor futures
- stream objects
- Docker client instances
- WebSocket sessions

## 13. Recovery on CLU Restart

The CLU must rebuild runtime state from the database plus Docker inspection.

### Startup reconciliation job

On application startup:

1. load servers from the database
2. for each server with a `containerId`, inspect Docker
3. if container exists and is running:
   - mark `RUNNING`
   - rebuild console session
4. if container exists and is stopped:
   - mark `STOPPED`
5. if container is missing:
   - mark `ERROR` or `STOPPED` depending on policy
   - record a reconciliation error

This is critical. Without it, stdin/stdout support breaks after every CLU restart.

## 14. Concurrency and Safety

Protect lifecycle actions per server.

Recommended guard:

- a per-server lock or serialized command queue

Prevent these races:

- `start` while `recreate` is in progress
- `send input` after container died
- simultaneous port update and delete

Also wrap lifecycle transitions in transactions where appropriate, but do not hold DB transactions open across long Docker operations.

## 15. Suggested Package Structure

Example package layout:

```text
com.scyed.clu
  api
  application
  console
  docker
  jpa
  lifecycle
  startup
```

Suggested classes:

- `ServerController`
- `ServerLifecycleService`
- `ServerConfigurationService`
- `ServerProvisioningHandler`
- `DockerRuntimeService`
- `ConsoleSessionManager`
- `ConsoleStreamBroadcaster`
- `ServerRepository`
- `ServerPortMappingRepository`
- `StartupReconciliation`

## 16. Incremental Delivery Plan

Status snapshot based on the current repository state on 2026-04-28.

### Phase 1: Clean up foundations

- [x] Fix the `Provisioning` interface mismatch.
- [x] Switch repository ID types to `UUID`.
- [ ] Replace the single `port` field in config with a list of port mappings.
  Current status: `PortMapping` exists, but `ServerConfig` still has a single `port` field and mappings are not persisted.
- [ ] Add database migrations.
  Current status: schema is still managed via `spring.jpa.hibernate.ddl-auto=create`.
- [ ] Extend the server status enum.
  Current status: `STOPPED`, `STARTING`, `INSTALLING`, `ERROR`, and `PROVISIONING` exist, but `RUNNING`, `STOPPING`, `RESTARTING`, `DELETING`, and `CREATING_CONTAINER` are still missing.

### Phase 2: Persist desired state

- [ ] Expand `Server` entity.
  Current status: `containerId` has been added, but startup command, working directory, timestamps, last error, and node metadata are still missing.
- [ ] Add `ServerPortMapping` entity.
- [ ] Add create/get/update endpoints.
  Current status: create and list endpoints exist, but there is no `GET /servers/{id}` or update endpoint yet.
- [ ] Validate local port conflicts.

### Phase 3: Async container creation

- [ ] Add `ServerLifecycleService`.
  Current status: async provisioning is handled directly in `ServerProvisioner`; a dedicated lifecycle service does not exist yet.
- [x] Add background provisioning event.
- [ ] Create and start Docker containers.
  Current status: provisioning creates the container and stores its ID, but it does not start the container yet.
- [x] Persist `containerId`.

### Phase 4: Console runtime

- [ ] Add `ConsoleSessionManager`.
- [ ] Attach stdout/stderr.
- [ ] Implement stdin writes.
- [ ] Add streaming endpoint or WebSocket.

### Phase 5: Reconfiguration

- [ ] Implement config updates.
- [ ] Implement recreate-required flow for port changes.
- [ ] Rebuild console session after recreate.

### Phase 6: Recovery and hardening

- [ ] Add startup reconciliation.
- [ ] Add per-server locking.
- [ ] Add integration tests with Docker.
- [ ] Add failure handling and better error surfaces.
  Current status: provisioning failures are caught and persisted as `ERROR`, but there is no structured recovery or retry model yet.

## 17. Testing Strategy

### Unit tests

- config validation
- port conflict detection
- lifecycle state transitions
- recreate-required decision logic

### Integration tests

- create server persists state
- background provision creates Docker container
- stdout can be read
- stdin can be written
- port mapping update recreates container
- restart reconciliation rebuilds session state

Use Testcontainers where possible for integration testing around Docker behavior.

## 18. Recommended First Milestone

The first milestone should be deliberately narrow:

1. `POST /servers` persists server config and port mappings
2. async job creates a Docker container
3. server status becomes `RUNNING`
4. `POST /servers/{id}/console/input` writes to stdin
5. `GET /servers/{id}/console/stream` exposes live stdout
6. `PUT /servers/{id}/ports` saves new mappings and recreates the container

That is enough to prove the full CLU ownership model.

## 19. Short Answer to the Original Design Question

If you want a practical implementation:

- `createServer(serverConfig)` should save a `Server` aggregate and return its UUID
- actual Docker creation should happen asynchronously in a Spring-managed background handler
- stdout/stderr should be handled by a `ConsoleSessionManager` on a dedicated executor
- stdin should be exposed through a `sendConsoleInput(serverId, input)` method backed by a live attach session
- port mapping changes should be stored in the database and initially applied by container recreation

That is the simplest design that fits the CLU role and will still scale into the fuller proposal later.
