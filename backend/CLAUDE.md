# CLAUDE.md — OpenSettlers backend

Guidance for working in the OpenSettlers backend, a Quarkus (Java 21) server that
runs an authoritative, real-time clone of *The Settlers II (10th Anniversary)*.

## Build, run, test

The repo uses the Maven wrapper. **`JAVA_HOME` must point at a JDK 21+** before
any `mvnw` command (the project targets Java 21; a Homebrew `openjdk` works):

```bash
export JAVA_HOME=$(brew --prefix openjdk)
export PATH="$JAVA_HOME/bin:$PATH"

./mvnw quarkus:dev      # live-coding dev mode on :8080 (starts a Dev Services Postgres in Docker)
./mvnw test             # run the unit test suite
./mvnw -q compile       # compile only
./mvnw package          # build target/quarkus-app/
```

Docker is required for `quarkus:dev` and any DB-backed run because Quarkus Dev
Services launches an ephemeral PostgreSQL container. The **unit tests do not need
Docker** — persistence is unit-tested through the pure converter
(`GameSnapshotMapper`), not the database.

## Architecture in one paragraph

The server runs a fixed-rate **game loop** (`GameEngine`, 10 ticks/second). Each
tick it drains queued player commands, then runs an ordered list of **systems**
(`ISystem`) over a single mutable **`GameState`**, then broadcasts a
fog-of-war-filtered JSON snapshot to every connected client. All state mutation
happens on the loop thread, so the simulation is single-threaded and lock-free;
WebSocket handlers only enqueue commands.

## Package map (`fr.opensettlers.*`)

- **`utils`** — the central `GameConfig` (every balance/timing constant lives here
  as a `public static final`, **not** in `application.properties`) and the
  `Coordinates` value object. All enums live in **`utils.enums`** (`BuildingName`,
  `ResourceType`, `TileType`, the unit `*State` enums, etc.).
- **`entities`** — the game model POJOs, split into subpackages:
  **`entities.building`** (`Building`, `BuildingFactory`, the concrete building
  types, `ConstructionSite`, plus the `Garrisoned`/`IProducer` contracts),
  **`entities.unit`** (`Worker`, `Carrier`, `Donkey`, `Soldier`, `Ship`),
  **`entities.world`** (`MapTile`, `Flag`, `Road`, `NaturalResourceNode`) and
  **`entities.resource`** (`Recipe`, `ResourceSlot`, `ResourceStack`).
  `BuildingFactory` builds every `BuildingName`; `Building.buildingCosts` holds
  construction costs.
- **`state`** — `GameState` (single source of truth), `GameSession` (state +
  connections + command queue), `RoadNetwork` (flag/road graph + Dijkstra
  pathfinding), `TerritoryManager`, `FogOfWarManager`.
- **`systems`** — one class per simulation concern, each an `ISystem`. The tick
  order is defined in `GameEngine.tick()`. The `ISystem` interface plus the
  cross-cutting `AiSystem` and `VictorySystem` sit at the package root; the
  concrete systems are grouped by domain: **`systems.military`** (`MilitarySystem`,
  `CombatSystem`, `MovementSystem`, `CatapultSystem`), **`systems.transport`**
  (`TransportSystem`, `TransportManager`, `DonkeySystem`), **`systems.economy`**
  (`EconomySystem`, `ProductionSystem`, `ConstructionSystem`, `WorkerSystem` + the
  `Supply`/`Demand`/`SupplySource` records), **`systems.exploration`**
  (`GeologistSystem`, `ScoutSystem`, `NavalSystem`) and **`systems.world`**
  (`GrowthSystem`, `VisionSystem`).
- **`service`** — `GameEngineService` (session lifecycle + command dispatch),
  `GameEngine` (the loop), `GameActions` (validated operations shared by humans
  **and** the AI), `GameStateSerializer`, and `mapgen/` (procedural maps).
- **`controller`** — REST (`GameController`, `SaveController`) and the WebSocket
  endpoint (`GameWebSocket`); `dto/` holds the wire records.
- **`persistence`** — Panache `GameSaveEntity`, the `GameSnapshot` model, the
  `GameSnapshotMapper` converter, and `GamePersistenceService`.

## Conventions to follow

- **Add a new game rule as a system.** Implement `ISystem`, add it to the ordered
  list in `GameEngine.tick()`. Keep systems stateless; read/write only `GameState`.
- **All validated player actions go through `GameActions`.** Both the WebSocket
  message handler and `AiSystem` call it, so a rule written there applies to
  humans and the AI alike. Don't duplicate placement/attack logic.
- **Balance values belong in `GameConfig`.** Don't scatter magic numbers in
  systems.
- **A new building type** means: add to `BuildingName` (+ `toString`), add a cost
  to `Building.buildingCosts`, add a `case` in `BuildingFactory.createBuilding`
  (the switch is exhaustive — the compiler will tell you), and, if it has a
  specialist occupant, map it in `BuildingFactory.occupantRoleFor`.
- **Coordinates are doubled-height hex coordinates** (see `Coordinates` /
  redblobgames). Use `Coordinates.neighbor(Direction)` and `distanceTo`.
- **Lombok** is used heavily (`@Data`, `@Getter`/`@Setter`). Generated
  getters/setters are not visible in source; assume they exist for fields.
- **Javadoc every public type and method.** The codebase keeps complete Javadoc;
  match that.
- Keep the engine off the DB hot path: persistence is invoked only from REST
  endpoints, never from a system.

## Subsystem notes

- **Win conditions** — `VictorySystem` eliminates a player when they own no
  non-destroyed `StorageBuilding` (HQ/warehouse/harbor); the last one standing
  wins. The engine broadcasts a terminal `GAME_OVER` message and stops the loop.
- **AI** — players listed in `GameState.aiPlayers` are driven by `AiSystem`
  (one decision every `GameConfig.AI_DECISION_INTERVAL` ticks). Set the count via
  `createGame(playerCount, aiPlayers)` / the `aiPlayers` field of the REST body.
- **Naval** — `HARBOR` (coastal warehouse) + `SHIPYARD` enable `NavalSystem`
  expeditions: a `Ship` BFS-pathfinds over water tiles and founds a new harbor on
  an unclaimed shore.
- **Persistence** — `POST /games/{id}/save`, `GET /saves`, `POST /saves/{id}/load`.
  Snapshots capture terrain, buildings (stock + garrison), flags and roads; units
  in motion and construction-in-progress are not persisted (sites reload as
  finished buildings, production buildings get their specialist back).

## Network protocol (quick reference)

- REST: `POST /games {playerCount, aiPlayers}` → `gameId`; `GET /games`;
  `DELETE /games/{id}`; save/load under `/games/{id}/save`, `/saves`.
- WebSocket: `ws://host/game/{gameId}?playerId=N` (omit `playerId` for a
  spectator). Server pushes one `MAP` message on connect, then a `STATE` message
  every tick, and a final `GAME_OVER`. Client → server messages are
  `GameMessage`s (`BUILD_BUILDING`, `DESTROY_BUILDING`, `PLACE_FLAG`,
  `LINK_FLAGS`, `ATTACK_BUILDING`, `SEND_GEOLOGIST`).
