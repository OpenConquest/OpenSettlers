# Open Settlers — Frontend

The web client for **Open Settlers**, a real-time settlement & conquest game
inspired by *The Settlers II (10th Anniversary)*. It renders the live game world
on an HTML canvas, lets you build an economy, lay roads, claim territory and wage
war against AI or human rivals, and talks to the Quarkus backend over REST (for
the lobby) and a WebSocket (for the live game loop).

Built with [Nuxt 4](https://nuxt.com), Vue 3 `<script setup>`, Tailwind CSS v4
and [shadcn-vue](https://www.shadcn-vue.com) UI primitives.

## Features

- **Canvas world renderer** — flat-top hex terrain, roads with moving carriers,
  flags, buildings (with construction progress), workers, soldiers, donkeys and
  ships, geologist signs, territory tint and fog of war, all viewport-culled.
- **Camera** — drag/Shift to pan, wheel to zoom, click the minimap to recentre,
  with a live viewport rectangle drawn on the minimap.
- **Build palette** — every building grouped by category, plus the general tools
  (inspect, flag, road, geologist, demolish, attack).
- **Road laying** — draw roads one tile at a time from flag to flag.
- **Selection panel** — inspect any building's productivity, garrison, coins or
  stored goods; demolish your own or attack an enemy's.
- **HUD** — connection status, current tick and a live stock summary.
- **Keyboard control** — `1`–`6` arm tools, `Esc` backs out of the current
  action, in the spirit of the original game.
- **Lobby** — create a game (human + AI counts), rejoin a running game, spectate,
  or restore a saved game.
- **Resilience** — the WebSocket reconnects automatically with backoff if the
  connection drops while the server loop is still running.

## Prerequisites

- Node.js 20+
- [pnpm](https://pnpm.io) (the repo ships a `pnpm-lock.yaml`)
- The Open Settlers backend running (see `../backend/README.md`). By default it
  listens on `http://localhost:8080`.

## Setup

```bash
pnpm install
```

## Development

```bash
pnpm dev
```

The dev server runs on `http://localhost:3000`. Start the backend first so the
lobby can create and list games.

## Configuration

The backend endpoints are read from runtime config and can be overridden with
environment variables (see `nuxt.config.ts`):

| Variable               | Default                 | Purpose                       |
| ---------------------- | ----------------------- | ----------------------------- |
| `NUXT_PUBLIC_API_BASE` | `http://localhost:8080` | REST base URL of the backend  |
| `NUXT_PUBLIC_WS_BASE`  | `ws://localhost:8080`   | WebSocket base URL of the API |

```bash
NUXT_PUBLIC_API_BASE=https://api.example.com \
NUXT_PUBLIC_WS_BASE=wss://api.example.com \
pnpm build
```

## Other scripts

```bash
pnpm build          # production build
pnpm preview        # preview the production build locally
pnpm generate       # static generation
npx nuxi typecheck  # type-check the whole app
```

## Project layout

```
app/
  pages/                 routes: lobby (/) and game (/game/[id])
  components/
    menu/                lobby screen
    game/                HUD, build palette, canvas, minimap, panels, dialogs
    ui/                  shadcn-vue primitives
  composables/           shared state (session, camera, UI tools, hotkeys, API)
  lib/                   pure helpers (hex maths, palette, building metadata)
  types/                 TypeScript mirror of the backend wire protocol
  assets/css/            Tailwind entry + theme
```

For the architecture and the conventions to follow when extending the client,
see [`CLAUDE.md`](./CLAUDE.md).
