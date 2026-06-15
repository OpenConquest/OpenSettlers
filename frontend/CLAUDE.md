# CLAUDE.md — Frontend

Guidance for working in the Open Settlers web client. Read this before changing
the rendering, networking or game-interaction code.

## Stack

- **Nuxt 4** (Vue 3, `<script setup lang="ts">`), pnpm.
- **Tailwind CSS v4** via `@tailwindcss/vite`; theme/entry in `app/assets/css/main.css`.
- **shadcn-vue** primitives under `app/components/ui` (generated — avoid hand-editing).
- Components are auto-imported **by file name, no path prefix** (`components: [{ path: "~/components", pathPrefix: false }]`), so `components/game/GameCanvas.vue` is `<GameCanvas>`. Keep component file names unique.
- Composables (`app/composables`) and `~/lib`, `~/types` are auto-imported too.

## How the app is wired

```
pages/index.vue ──> MainMenu            (REST: create / list / load games)
pages/game/[id].vue ──> GameHud + BuildPalette + GameCanvas + Minimap
                        + SelectionPanel + GameOverDialog
```

State is held in **module-level singletons inside composables** (not Pinia),
because only one game is viewed at a time and every component shares the same
refs:

| Composable          | Owns                                                      |
| ------------------- | -------------------------------------------------------- |
| `useGameSession`    | the WebSocket, decoded server state (`map`, `state`, `tick`, `gameOver`), and the client→server `actions`. |
| `useCamera`         | pan/zoom plus the published viewport size.               |
| `useGameUi`         | the active tool, selected building id, in-progress road draft. |
| `useHotkeys`        | installs the keyboard shortcuts for the game page.       |
| `useGameApi`        | thin typed wrapper over the REST endpoints.              |

Pure, dependency-free helpers live in `~/lib`:

- `hex.ts` — doubled-height hex maths (`hexToPixel`, `pixelToHex`, neighbours, distance). **The single source of grid truth** — never inline hex maths elsewhere.
- `palette.ts` — every colour and resource/terrain glyph.
- `buildings.ts` — building metadata, categories and the build-menu grouping.

## The wire protocol (`app/types/game.ts`)

`app/types/game.ts` is a **hand-maintained mirror** of the backend's Java
records:

- Server → client over WebSocket: `MapMessage` (once on open), `StateMessage`
  (every tick, fog-of-war filtered per player), `GameOverMessage`.
- Client → server: `ClientMessage` (mirror of `controller.GameMessage` /
  `MessageType`).
- REST payloads mirror `GameController` / `SaveController`.

**If you touch a backend DTO, enum or message, update this file in the same
change.** Backend sources to keep in sync:

- `backend/.../controller/dto/GameStateDto.java`, `MapDto.java`
- `backend/.../controller/GameMessage.java`, `MessageType.java`
- `backend/.../controller/GameController.java`, `SaveController.java`
- the enums in `backend/.../utils` (`TileType`, `ResourceType`, `BuildingName`).

Coordinates are **doubled-height, flat-top hexes**: a tile is valid when
`x + y` is even; the WS player slot is passed as `?playerId=N` (omitted =
spectator, who sees everything with no fog).

## Rendering (`GameCanvas.vue`)

- One `<canvas>` drawn in **world coordinates** through a camera transform;
  HiDPI-aware (`dpr`). `HEX_SIZE = 14` is the world-space tile radius and **must
  match the minimap's** `HEX_SIZE` so clicks map back correctly.
- Rendering is **on-demand**: `requestRedraw()` coalesces to a single
  `requestAnimationFrame`; it is triggered by watchers on map/state/camera/UI.
  Don't add an unconditional render loop.
- Terrain is **viewport-culled**; keep new layers inside the same cull window.
- All gameplay input funnels through `onClick(coord)`, which dispatches on the
  active `useGameUi` tool. Add new interactions as a new `ToolKind` + a case
  there, not as ad-hoc listeners.

## Conventions

- TypeScript only; prefer `type` unions matching the server's JSON strings.
- Keep server state (`useGameSession`) separate from local UI state
  (`useGameUi`) — the rendering and input layers stay decoupled.
- Match the surrounding style: concise JSDoc on exported functions/composables,
  no dead code, Tailwind utility classes (no bespoke CSS unless unavoidable).
- New colours/glyphs go in `lib/palette.ts`; new building data in
  `lib/buildings.ts`. Don't scatter literals through components.
- User-facing errors surface via `vue-sonner` toasts, never `alert()`.

## Checks before finishing

```bash
npx nuxi typecheck   # must pass
pnpm build           # for changes that could affect SSR/build
```

There is no test suite on the frontend yet; rely on the typecheck and a manual
smoke test against a running backend (`pnpm dev`).
