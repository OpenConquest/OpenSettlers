<p align="center">
  <h1 align="center">OpenSettlers</h1>
  <p align="center">
    <strong>An open-source, web-based clone of <em>The Settlers II: 10th Anniversary Edition</em></strong>
  </p>
  <p align="center">
    <a href="#getting-started"><img alt="Get Started" src="https://img.shields.io/badge/Get_Started-blue?style=for-the-badge" /></a>
    <a href="LICENSE"><img alt="License: GPL-3.0" src="https://img.shields.io/badge/License-GPLv3-green?style=for-the-badge" /></a>
    <a href="#tech-stack"><img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" /></a>
    <a href="#tech-stack"><img alt="Quarkus" src="https://img.shields.io/badge/Quarkus-3.36-4695EB?style=for-the-badge&logo=quarkus" /></a>
    <a href="#tech-stack"><img alt="Nuxt 3" src="https://img.shields.io/badge/Nuxt-4-00DC82?style=for-the-badge&logo=nuxtdotjs" /></a>
  </p>
</p>

---

**OpenSettlers** faithfully recreates the classic *Settlers II: 10th Anniversary* experience as a modern web application. The project features procedural map generation, complete production chains, road logistics, military expansion, naval expeditions, and AI opponents — all powered by a **real-time, authoritative-server** architecture.

> **Note:** This is a fan-made, non-commercial project. It is not affiliated with or endorsed by Ubisoft or Blue Byte.

---

## Table of Contents

1. [Highlights](#highlights)
2. [Architecture Overview](#architecture-overview)
3. [Tech Stack](#tech-stack)
4. [Implemented Game Mechanics](#implemented-game-mechanics)
5. [Project Structure](#project-structure)
6. [Getting Started](#getting-started)
7. [Network API Reference](#network-api-reference)
8. [Game Loop & Systems](#game-loop--systems)
9. [Docker](#docker)
10. [License](#license)

---

## Highlights

- **Procedural hex maps** — Perlin noise elevation & humidity with Poisson-disk resource seeding
- **32 building types & ~20 resources** — Full Settlers II production chains (wood → planks, ore → steel → weapons, wheat → flour → bread, brewing, livestock, and more)
- **Road logistics** — Flags, carriers, donkeys, Dijkstra pathfinding, and level-2 main roads
- **Military system** — Recruitment, garrisons, territory projection, 1v1 duels, building capture, catapult sieges, gold promotions
- **Naval gameplay** — Shipyards, coastal harbors, and maritime colonization expeditions
- **AI opponents** — Computer-controlled players with economy and offense automation
- **Fog of war** — Per-player visibility filtering on every state broadcast
- **Save & Load** — Full game state persistence to PostgreSQL
- **Victory conditions** — Player elimination when all warehouses are lost

---

## Architecture Overview

OpenSettlers follows a strict **client-server model** where the server is the single source of truth:

```
┌──────────────────────────────────┐     WebSocket (JSON)     ┌───────────────────────────┐
│         Backend (Quarkus)        │◄────────────────────────►│    Frontend (Nuxt 3)      │
│                                  │                           │                           │
│  ┌────────────────────────────┐  │   REST (Lobby, Saves)    │  ┌───────────────────────┐ │
│  │  Game Engine (10 TPS)      │  │◄────────────────────────►│  │  WebGL Map Renderer   │ │
│  │  ┌──────────────────────┐  │  │                           │  │  Vue 3 HUD & Lobby    │ │
│  │  │ Ordered Systems      │  │  │                           │  │  Composable State     │ │
│  │  │ (AI → Combat → ...   │  │  │                           │  └───────────────────────┘ │
│  │  │  → Victory)          │  │  │                           │                           │
│  │  └──────────────────────┘  │  │                           └───────────────────────────┘
│  │  Single-threaded, lockless │  │
│  └────────────────────────────┘  │
│                                  │
│  PostgreSQL (Hibernate Panache)  │
└──────────────────────────────────┘
```

- **Server (Quarkus / Java 21)** — The game engine runs a fixed-rate game loop at **10 TPS** (ticks per second). Each tick, it dequeues player commands, executes an ordered sequence of simulation **systems** against a single `GameState`, then broadcasts a fog-of-war-filtered JSON snapshot to each connected client. All state mutations happen on the loop thread — the simulation is **single-threaded and lock-free**. WebSocket handlers only enqueue commands.

- **Client (Nuxt 3 + WebGL)** — Manages the lobby, in-game HUD, and map rendering. It receives server snapshots, interpolates unit movement for smooth 60 FPS rendering, and transmits player intentions as commands.

---

## Tech Stack

| Layer | Technology | Role |
|---|---|---|
| **Backend** | Quarkus 3.36 (Java 21) | Game engine, dependency injection (ArC), scheduler |
| **Persistence** | PostgreSQL + Hibernate Panache | Game save/load via Active Record pattern |
| **Real-time** | Quarkus WebSockets Next | Non-blocking reactive WebSocket for state broadcasting |
| **Serialization** | Jackson | JSON serialization for messages and snapshots |
| **Frontend** | Nuxt 4 (Vue 3) | Reactive UI, HUD, shared client state via composables |
| **UI Components** | shadcn-nuxt + Radix Vue | Accessible, composable UI primitives |
| **Styling** | Tailwind CSS 4 | Utility-first styling |
| **Code Gen** | Lombok | Boilerplate reduction (`@Data`, `@Builder`, etc.) |
| **Build Tools** | Maven (backend) · pnpm (frontend) | Build and dependency management |
| **CI/CD** | GitHub Actions | Automated frontend deployment to GitHub Pages |

---

## Implemented Game Mechanics

The backend is a full-fledged game engine (`fr.opensettlers.*`) implementing the core Settlers II gameplay loop:

### Procedural Hex Map
- **Doubled-height** hex coordinate system
- Perlin noise-based terrain generation (elevation + humidity biomes)
- Poisson-disk resource seeding (`service/mapgen`)
- Configurable map size (default 64×64)

### Logistics & Transport
- **Flags, roads, and road networks** with Dijkstra shortest-path routing
- **Carriers** autonomously transporting goods between flags
- **Donkeys** for heavy-traffic main roads
- **Level-2 main roads** for upgraded throughput

### Construction & Building
- Construction site lifecycle: material delivery → earthworks → masonry → commissioning
- Specialized worker assignment upon completion
- **Graduated placement rules** — building size (hut / house / castle) depends on terrain slope and spacing from neighbors
- Flags placed only on valid unoccupied tiles

### Production Chains
- **32 building types** and **~20 resource types** replicating Settlers II economy:
  - Woodcutter → Sawmill (logs → planks)
  - Mines (granite, coal, iron, gold) — each requires the matching ore vein and consumes food
  - Smelter → Armory / Metalworks (ore → steel → weapons / tools / gold coins)
  - Farm → Windmill → Bakery (wheat → flour → bread)
  - Pig Farm + Slaughterhouse (grain → pigs → meat)
  - Brewery (wheat + water → beer for soldiers)
- Each building has its own **production timer** tuned to original Settlers II pacing
- **Resource growth** — planted trees mature into harvestable lumber; wheat fields are sown, grow, and are reaped

### Geologists & Scouts
- **Geologists** prospect mountain tiles, placing mineral signs
- **Scouts** explore fog of war around a flag

### Military
- **Recruitment** requires sword + shield + beer
- **Garrison sizes**: Barracks (2) · Guardhouse (3) · Watchtower (6) · Fortress (9)
- **Headquarters** is also a defensible garrison
- **Lookout Tower** — provides visibility without claiming territory
- **Territory projection** based on military building placement
- **Gold-coin promotions** to upgrade soldier rank
- **Adjustable attack force** — choose how many soldiers to commit
- **1v1 sequential duels** to resolve battles
- **Building capture** on successful attack
- **Catapult sieges** for ranged bombardment

### Fog of War
- Per-player explored area tracking
- State broadcasts filtered to each player's visible area

### Naval
- **Shipyard** buildings to construct vessels
- **Coastal harbors** as naval logistics nodes
- **Maritime expeditions** to colonize unexplored shorelines

### AI Opponents
- Computer-controlled players that autonomously:
  - Develop their economy and production chains
  - Connect buildings with roads
  - Launch military attacks

### Victory & Persistence
- **Player elimination** when all warehouses/headquarters are destroyed
- **Game over** broadcast with winner designation
- **Full game-state save/load** to PostgreSQL database

---

## Project Structure

```
OpenSettlers/
├── backend/                          # Quarkus server (Java 21)
│   ├── src/main/java/fr/opensettlers/
│   │   ├── utils/                    # Enums, GameConfig (all tuning constants)
│   │   ├── entities/                 # Game model (buildings, units, flags, roads, tiles)
│   │   │   ├── Building.java         #   Base building + specialized subtypes
│   │   │   ├── Carrier.java          #   Road transport unit
│   │   │   ├── Soldier.java          #   Military unit with rank & combat
│   │   │   ├── Ship.java             #   Naval vessel
│   │   │   ├── Flag.java             #   Logistics node on roads
│   │   │   ├── Road.java             #   Connection between two flags
│   │   │   └── MapTile.java          #   Hex tile with terrain & resources
│   │   ├── state/                    # GameState, GameSession, RoadNetwork,
│   │   │                             #   TerritoryManager, FogOfWar
│   │   ├── systems/                  # Simulation systems (one per mechanic)
│   │   │   ├── ISystem.java          #   Common system interface
│   │   │   ├── AiSystem.java         #   AI decision-making
│   │   │   ├── CombatSystem.java     #   1v1 duel resolution
│   │   │   ├── ProductionSystem.java #   Building production cycles
│   │   │   ├── TransportSystem.java  #   Carrier dispatching
│   │   │   ├── NavalSystem.java      #   Ship & expedition logic
│   │   │   └── ...                   #   14 more systems
│   │   ├── service/                  # GameEngine (loop), GameActions, mapgen/
│   │   │   ├── GameEngine.java       #   Fixed-rate game loop
│   │   │   ├── GameActions.java      #   Player command handlers
│   │   │   └── mapgen/               #   Perlin noise, Poisson-disk, map gen
│   │   ├── controller/               # REST endpoints + WebSocket + DTOs
│   │   └── persistence/              # Panache entities, snapshots, save service
│   ├── src/test/java/                # JUnit 5 test suite
│   ├── src/main/resources/           # application.properties (Dev Services PostgreSQL)
│   └── pom.xml
│
├── frontend/                         # Nuxt 4 client
│   ├── app/
│   │   ├── components/
│   │   │   ├── game/                 # In-game UI components
│   │   │   │   ├── GameCanvas.vue    #   WebGL hex map renderer
│   │   │   │   ├── GameHud.vue       #   Heads-up display overlay
│   │   │   │   ├── BuildPalette.vue  #   Building selection panel
│   │   │   │   ├── Minimap.vue       #   Overview minimap
│   │   │   │   ├── SelectionPanel.vue#   Selected entity details
│   │   │   │   └── ...              #   Military, inventory, distribution dialogs
│   │   │   ├── menu/                 #   Main menu & lobby
│   │   │   └── ui/                   #   Reusable UI primitives (shadcn)
│   │   ├── composables/              # Vue composables
│   │   │   ├── useGameSession.ts     #   WebSocket connection & state sync
│   │   │   ├── useGameApi.ts         #   REST API client
│   │   │   ├── useCamera.ts          #   Map camera controls
│   │   │   └── useHotkeys.ts         #   Keyboard shortcuts
│   │   ├── pages/                    # Nuxt file-based routing
│   │   └── types/                    # TypeScript type definitions
│   ├── nuxt.config.ts
│   └── package.json
│
├── .github/workflows/deploy.yml      # GitHub Actions: frontend → GitHub Pages
└── LICENSE                           # GNU GPLv3
```

---

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| **Java** | 21+ | GraalVM recommended for native builds |
| **Node.js** | 20+ | With pnpm |
| **Docker** | Latest | Required for PostgreSQL via Quarkus Dev Services |

### Backend

```bash
cd backend

# Set JAVA_HOME to a JDK 21+ (macOS example)
export JAVA_HOME=$(brew --prefix openjdk)

# Start in dev mode (auto-provisions PostgreSQL via Docker)
./mvnw quarkus:dev
```

The backend will be available at:

| Endpoint | URL |
|---|---|
| Quarkus Dev UI | `http://localhost:8080/q/dev/` |
| REST API (Lobby) | `http://localhost:8080/games` |
| WebSocket (Game) | `ws://localhost:8080/game/{gameId}` |

> Unit tests do not require Docker: `./mvnw test`

### Frontend

```bash
cd frontend
pnpm install
pnpm dev        # → http://localhost:3000
```

---

## Network API Reference

### REST — Lobby & Saves

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/games` | Create a new game. Body: `{"playerCount": 2, "aiPlayers": 1}`. Returns `gameId`. |
| `GET` | `/games` | List all active games. |
| `DELETE` | `/games/{gameId}` | Stop and remove a game. |
| `POST` | `/games/{gameId}/save` | Save a running game. Body: `{"name": "..."}`. |
| `GET` | `/saves` | List all saved games. |
| `POST` | `/saves/{saveId}/load` | Restore a saved game as a new active game. |

### WebSocket — Real-time Game State

Connect to `ws://localhost:8080/game/{gameId}?playerId=N` (omit `playerId` to join as spectator).

**Server → Client messages:**

| Type | When | Payload |
|---|---|---|
| `MAP` | On connect | Terrain tiles, elevation, natural resources |
| `STATE` | Every tick | Buildings, flags, roads, carriers, workers, soldiers, ships, territory (fog-filtered) |
| `GAME_OVER` | Game ends | Winner designation |

**Client → Server commands:**

| Command | Description |
|---|---|
| `BUILD_BUILDING` | Place a building at a hex coordinate |
| `DESTROY_BUILDING` | Demolish a building |
| `PLACE_FLAG` | Place a flag on a valid tile |
| `LINK_FLAGS` | Build a road between two flags |
| `ATTACK_BUILDING` | Attack an enemy building (optional `attackerCount`) |
| `SEND_GEOLOGIST` | Dispatch a geologist to prospect from a flag |
| `SEND_SCOUT` | Dispatch a scout to explore from a flag |
| `SET_PRODUCTION` | Toggle production on/off for a building |
| `SET_COIN_DELIVERY` | Enable/disable gold coin delivery to a military building |
| `SET_DISTRIBUTION` | Configure resource distribution priorities |
| `SET_MILITARY` | Adjust garrison occupation settings |

---

## Game Loop & Systems

At each tick, the `GameEngine` dequeues all pending player commands, then executes simulation systems in strict order:

```
AI → Military → Combat → Catapults → Movement → Geologists → Scouts
→ Workers → Growth → Economy → Construction → Production → Transport
→ Donkeys → Naval → Vision → Victory
```

Each system implements the `ISystem` interface and operates exclusively on the shared `GameState`. After all systems execute, a fog-of-war-filtered state snapshot is broadcast to each connected player.

All game balance constants — tick rate, distances, garrison sizes, production timers, AI parameters, naval settings — are centralized in `GameConfig.java` for easy tuning.

---

## Docker

The backend ships with multiple Dockerfiles in `backend/src/main/docker/`:

| Dockerfile | Description |
|---|---|
| `Dockerfile.jvm` | Standard JVM-based image |
| `Dockerfile.legacy-jar` | Legacy JAR packaging |
| `Dockerfile.native` | GraalVM native image |
| `Dockerfile.native-micro` | Minimal native image (micro base) |

> In **dev mode**, PostgreSQL is auto-launched via Quarkus Dev Services — no manual Docker setup needed.

---

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <sub>Built as a tribute to <em>The Settlers II: 10th Anniversary Edition</em> by Blue Byte</sub>
</p>
