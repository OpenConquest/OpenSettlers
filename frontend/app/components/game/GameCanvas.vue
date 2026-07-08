<script setup lang="ts">
/**
 * Canvas renderer and input surface for the live game.
 *
 * Rendering is on-demand: a single `requestAnimationFrame` is scheduled whenever
 * the server state, camera, or hover changes. The whole scene is drawn in world
 * coordinates through a camera transform, with viewport culling so only visible
 * tiles are painted.
 *
 * All gameplay input funnels through {@link onClick}, which dispatches an action
 * based on the active {@link useGameUi} tool.
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  hexCorners,
  hexKey,
  hexToPixel,
  pixelToHex,
  sameHex,
  type Point,
} from "~/lib/hex";
import { TERRAIN_DETAIL, TILE_COLORS, playerColor } from "~/lib/palette";
import {
  drawBuilding,
  drawConstructionSite,
  drawFishRipple,
  drawFlag,
  drawRocks,
  drawSettler,
  drawShip,
  drawSign,
  drawTree,
  drawWheat,
  hashRand,
  shade,
} from "~/lib/sprites";
import { findRoadPath } from "~/lib/roads";
import { toast } from "vue-sonner";
import type {
  BuildingDto,
  Coordinates,
  FlagDto,
  ResourceTileDto,
  ResourceType,
  TileType,
} from "~/types/game";

const HEX_SIZE = 14; // world-space circum-radius of one tile

const { map, state, session, tick } = useGameSession();
const { tool, selectedBuildingId, roadDraft } = useGameUi();
const ui = useGameUi();
const sessionStore = useGameSession();
const { camera, panBy, zoomBy, centerOn, setViewport } = useCamera();
const { log } = useGameLog();

const containerRef = ref<HTMLDivElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const hover = ref<Coordinates | null>(null);

let ctx: CanvasRenderingContext2D | null = null;
const cssW = ref(0);
const cssH = ref(0);
let dpr = 1;
let redrawHandle = 0;
let resizeObserver: ResizeObserver | null = null;

/* ------------------------------- Derived data ------------------------------ */

/** Flags indexed by id, for resolving road endpoints. */
const flagsById = computed(() => {
  const m = new Map<string, FlagDto>();
  for (const f of state.value?.flags ?? []) m.set(f.id, f);
  return m;
});

/** Owner id per tile key, for the territory tint. */
const territoryByKey = computed(() => {
  const m = new Map<string, number>();
  for (const [x, y, owner] of state.value?.territory ?? []) m.set(hexKey(x, y), owner);
  return m;
});

/** Explored tile keys for fog of war; `null` means everything is visible. */
const exploredKeys = computed(() => {
  const explored = state.value?.explored;
  if (!explored) return null;
  const s = new Set<string>();
  for (const [x, y] of explored) s.add(hexKey(x, y));
  return s;
});

/** Tiles a road may not run through: every building and flag position. */
const roadBlockedKeys = computed(() => {
  const s = new Set<string>();
  for (const b of state.value?.buildings ?? []) s.add(hexKey(b.x, b.y));
  for (const f of state.value?.flags ?? []) s.add(hexKey(f.x, f.y));
  return s;
});

/**
 * Live preview while drawing a road: the shortest buildable path from the
 * chosen start flag to the hovered tile. `path` is the intermediate route,
 * `destFlag` is set when the hovered tile is a connectable flag, and
 * `reachable` says whether any buildable route exists at all.
 */
const roadPreview = computed(() => {
  const draft = roadDraft.value;
  const target = hover.value;
  if (!draft || !target || !map.value || sameHex(target, draft.start)) return null;

  const path = findRoadPath(
    { tileAt, blocked: roadBlockedKeys.value },
    draft.start,
    target,
  );
  const flag = findFlagAt(target);
  const destFlag = flag && flag.id !== draft.startFlagId ? flag : null;
  return { path, destFlag, reachable: path !== null };
});

/* ------------------------------ Coordinate maths --------------------------- */

function screenToWorld(sx: number, sy: number): Point {
  return {
    x: camera.x + (sx - cssW.value / 2) / camera.zoom,
    y: camera.y + (sy - cssH.value / 2) / camera.zoom,
  };
}

function eventToHex(e: PointerEvent | MouseEvent): Coordinates {
  const rect = canvasRef.value!.getBoundingClientRect();
  const w = screenToWorld(e.clientX - rect.left, e.clientY - rect.top);
  return pixelToHex(w.x, w.y, HEX_SIZE);
}

function tileAt(c: Coordinates) {
  return map.value?.tiles.get(hexKey(c.x, c.y));
}

function findBuildingAt(c: Coordinates): BuildingDto | undefined {
  return state.value?.buildings.find((b) => b.x === c.x && b.y === c.y);
}

function findFlagAt(c: Coordinates): FlagDto | undefined {
  return state.value?.flags.find((f) => f.x === c.x && f.y === c.y);
}

/* --------------------------------- Drawing -------------------------------- */

function requestRedraw(): void {
  if (redrawHandle) return;
  redrawHandle = requestAnimationFrame(() => {
    redrawHandle = 0;
    draw();
  });
}

function applyCameraTransform(): void {
  ctx!.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx!.translate(cssW.value / 2, cssH.value / 2);
  ctx!.scale(camera.zoom, camera.zoom);
  ctx!.translate(-camera.x, -camera.y);
}

function tracePath(corners: Point[]): void {
  ctx!.beginPath();
  ctx!.moveTo(corners[0]!.x, corners[0]!.y);
  for (let i = 1; i < corners.length; i++) ctx!.lineTo(corners[i]!.x, corners[i]!.y);
  ctx!.closePath();
}

function draw(): void {
  if (!ctx || !map.value) return;
  const c = ctx;

  c.setTransform(dpr, 0, 0, dpr, 0, 0);
  c.clearRect(0, 0, cssW.value, cssH.value);
  c.fillStyle = "#1b2a36";
  c.fillRect(0, 0, cssW.value, cssH.value);
  applyCameraTransform();

  // Viewport bounds in world space (with a generous margin so tall sprites
  // straddling the edge are not clipped) for culling.
  const tl = screenToWorld(0, 0);
  const br = screenToWorld(cssW.value, cssH.value);
  const margin = HEX_SIZE * 3;

  drawTerrain(tl, br, margin);
  drawRoads();
  drawScene(tl, br, margin);
  drawOverlays();
}

/** Whether a world point lies within the culling window. */
function inView(p: Point, tl: Point, br: Point, margin: number): boolean {
  return p.x >= tl.x - margin && p.x <= br.x + margin && p.y >= tl.y - margin && p.y <= br.y + margin;
}

function drawTerrain(tl: Point, br: Point, margin: number): void {
  const c = ctx!;
  const explored = exploredKeys.value;
  const phase = tick.value;

  for (const tile of map.value!.tiles.values()) {
    const center = hexToPixel(tile.x, tile.y, HEX_SIZE);
    if (!inView(center, tl, br, margin)) continue;

    const key = hexKey(tile.x, tile.y);
    const isExplored = !explored || explored.has(key);
    // Overlap tiles slightly so the seams between them disappear.
    tracePath(hexCorners(center, HEX_SIZE + 0.8));

    // Base fill, jittered per tile so large terrain swathes aren't flat slabs.
    const jitter = (hashRand(tile.x, tile.y) - 0.5) * 0.1;
    c.fillStyle = shade(TILE_COLORS[tile.tileType], jitter);
    c.fill();

    if (isExplored) drawTerrainDetail(tile, center, phase);

    // Territory tint.
    const owner = territoryByKey.value.get(key);
    if (owner != null && owner >= 0) {
      c.fillStyle = playerColor(owner) + "33";
      c.fill();
    }

    // Fog of war: darken unexplored tiles.
    if (!isExplored) {
      c.fillStyle = "rgba(8, 14, 20, 0.82)";
      c.fill();
    }
  }
}

/**
 * Paints the cheap per-tile texture that gives each terrain its character:
 * grass tufts, sandy speckles, drifting water highlights, rocky shading, field
 * furrows. Marks are placed deterministically (so they never shimmer) and the
 * path is already clipped to the tile by the caller's fill.
 */
function drawTerrainDetail(tile: { x: number; y: number; tileType: TileType }, center: Point, phase: number): void {
  const c = ctx!;
  const detail = TERRAIN_DETAIL[tile.tileType];
  const speckle = (n: number, r: number, color: string) => {
    c.fillStyle = color;
    for (let i = 0; i < n; i++) {
      const ox = (hashRand(tile.x, tile.y, i * 2 + 1) - 0.5) * HEX_SIZE * 1.4;
      const oy = (hashRand(tile.x, tile.y, i * 2 + 2) - 0.5) * HEX_SIZE * 1.2;
      c.beginPath();
      c.arc(center.x + ox, center.y + oy, r, 0, Math.PI * 2);
      c.fill();
    }
  };

  switch (tile.tileType) {
    case "GRASS":
    case "HILLS":
      speckle(3, 0.7, detail.dark);
      speckle(2, 0.6, detail.light);
      break;
    case "DESERT":
      speckle(4, 0.6, detail.dark);
      break;
    case "MOUNTAIN":
    case "STONE":
      speckle(3, 1.1, detail.dark);
      speckle(2, 0.8, detail.light);
      break;
    case "FIELD": {
      // Ploughed furrows.
      c.strokeStyle = detail.dark;
      c.lineWidth = 0.6;
      for (let i = -1; i <= 1; i++) {
        c.beginPath();
        c.moveTo(center.x - HEX_SIZE * 0.8, center.y + i * HEX_SIZE * 0.45);
        c.lineTo(center.x + HEX_SIZE * 0.8, center.y + i * HEX_SIZE * 0.45);
        c.stroke();
      }
      break;
    }
    case "WATER": {
      // Two slowly drifting highlight strokes.
      c.strokeStyle = detail.light;
      c.lineWidth = 0.7;
      c.globalAlpha = 0.5;
      for (let i = 0; i < 2; i++) {
        const yy = center.y + (i - 0.5) * HEX_SIZE * 0.5 + Math.sin(phase * 0.1 + tile.x) * 1.2;
        c.beginPath();
        c.moveTo(center.x - HEX_SIZE * 0.6, yy);
        c.quadraticCurveTo(center.x, yy - 1.5, center.x + HEX_SIZE * 0.6, yy);
        c.stroke();
      }
      c.globalAlpha = 1;
      break;
    }
  }
}

function drawRoads(): void {
  const c = ctx!;
  for (const road of state.value?.roads ?? []) {
    const a = flagsById.value.get(road.startFlagId);
    const b = flagsById.value.get(road.endFlagId);
    if (!a || !b) continue;

    const pts: Point[] = [hexToPixel(a.x, a.y, HEX_SIZE)];
    for (const [x, y] of road.path) pts.push(hexToPixel(x, y, HEX_SIZE));
    pts.push(hexToPixel(b.x, b.y, HEX_SIZE));

    // Draw a dirt path: a dark edge with a lighter tan track on top.
    const topPx = road.level >= 2 ? 4.5 : 3;
    c.lineJoin = "round";
    c.lineCap = "round";
    const trace = () => {
      c.beginPath();
      c.moveTo(pts[0]!.x, pts[0]!.y);
      for (let i = 1; i < pts.length; i++) c.lineTo(pts[i]!.x, pts[i]!.y);
      c.stroke();
    };
    c.strokeStyle = "#5b4226";
    c.lineWidth = (topPx + 2.5) / camera.zoom;
    trace();
    c.strokeStyle = road.level >= 2 ? "#d0ab6e" : "#b58f5b";
    c.lineWidth = topPx / camera.zoom;
    trace();

    // Carrier as a small dot interpolated along the path.
    if (road.carrier) drawCarrier(pts, road.carrier.progress, road.path.length + 1);
  }
}

function drawCarrier(pts: Point[], progress: number, totalSegments: number): void {
  const c = ctx!;
  const t = Math.max(0, Math.min(1, progress / Math.max(1, totalSegments)));
  const span = (pts.length - 1) * t;
  const i = Math.min(pts.length - 2, Math.floor(span));
  const f = span - i;
  const p = pts[i]!;
  const q = pts[i + 1]!;
  const x = p.x + (q.x - p.x) * f;
  const y = p.y + (q.y - p.y) * f;
  drawSettler(ctx!, x, y, HEX_SIZE * 0.78, "#caa15a", "carrier");
}

/** Tint used to draw an ore node / surveyed sign for each resource. */
function oreColor(r: ResourceType): string {
  switch (r) {
    case "COAL": return "#33312e";
    case "IRON": return "#a6624a";
    case "GOLD": return "#caa12e";
    case "STONE": return "#9a9488";
    default: return "#caa12e";
  }
}

/** Draws one natural-resource node (a tree cluster, ore rocks, a wheat patch or
 *  a fishing ripple) whose size hints at the remaining quantity. */
function drawResourceNode(node: ResourceTileDto, p: Point, phase: number): void {
  const c = ctx!;
  switch (node.resource) {
    case "LOG": {
      // A small grove: more trees while the node is rich, fewer as it depletes.
      const count = Math.min(4, Math.max(1, Math.ceil(node.quantity / 12)));
      const trees: { ox: number; oy: number; seed: number; broad: boolean }[] = [];
      for (let i = 0; i < count; i++) {
        trees.push({
          ox: (hashRand(node.x, node.y, i * 2 + 1) - 0.5) * HEX_SIZE * 0.8,
          oy: (hashRand(node.x, node.y, i * 2 + 2) - 0.5) * HEX_SIZE * 0.5,
          seed: hashRand(node.x, node.y, i + 7),
          broad: hashRand(node.x, node.y, i + 20) > 0.62,
        });
      }
      trees.sort((a, b) => a.oy - b.oy); // paint back-to-front within the tile
      for (const t of trees) drawTree(c, p.x + t.ox, p.y + t.oy, HEX_SIZE * 0.8, t.seed, t.broad);
      break;
    }
    case "STONE": drawRocks(c, p.x, p.y, HEX_SIZE, hashRand(node.x, node.y), "#9a9488"); break;
    case "COAL": drawRocks(c, p.x, p.y, HEX_SIZE, hashRand(node.x, node.y), "#33312e"); break;
    case "IRON": drawRocks(c, p.x, p.y, HEX_SIZE, hashRand(node.x, node.y), "#a6624a"); break;
    case "GOLD": drawRocks(c, p.x, p.y, HEX_SIZE, hashRand(node.x, node.y), "#caa12e"); break;
    case "WHEAT":
      for (let i = 0; i < 3; i++) drawWheat(c, p.x + (i - 1) * HEX_SIZE * 0.32, p.y + HEX_SIZE * 0.1, HEX_SIZE, hashRand(node.x, node.y, i));
      break;
    case "FISH": drawFishRipple(c, p.x, p.y, HEX_SIZE, phase); break;
  }
}

/**
 * Paints every upright object — natural resources, signs, flags, buildings and
 * units — in a single pass sorted by world `y`, so southern sprites overlap
 * northern ones and the flat map gains a Settlers-like sense of depth. Each
 * object is culled to the viewport by its base point.
 */
function drawScene(tl: Point, br: Point, margin: number): void {
  const c = ctx!;
  const phase = tick.value;
  const items: { y: number; draw: () => void }[] = [];
  const add = (x: number, y: number, draw: () => void) => {
    const p = hexToPixel(x, y, HEX_SIZE);
    if (inView(p, tl, br, margin)) items.push({ y: p.y, draw });
  };

  for (const node of state.value?.resources ?? [])
    add(node.x, node.y, () => drawResourceNode(node, hexToPixel(node.x, node.y, HEX_SIZE), phase));
  for (const sign of state.value?.signs ?? [])
    add(sign.x, sign.y, () => {
      const p = hexToPixel(sign.x, sign.y, HEX_SIZE);
      drawSign(c, p.x, p.y, HEX_SIZE, sign.resource ? oreColor(sign.resource) : null);
    });
  for (const f of state.value?.flags ?? [])
    add(f.x, f.y, () => {
      const p = hexToPixel(f.x, f.y, HEX_SIZE);
      drawFlag(c, p.x, p.y, HEX_SIZE, playerColor(f.playerId), f.resources.length, phase);
    });
  for (const b of state.value?.buildings ?? []) {
    if (!b.name) continue;
    const name = b.name;
    add(b.x, b.y, () => {
      const p = hexToPixel(b.x, b.y, HEX_SIZE);
      if (b.underConstruction) {
        const progress = Math.min(1, ((b.groundworkProgress ?? 0) + (b.buildingProgress ?? 0)) / 200);
        drawConstructionSite(c, name, p.x, p.y, HEX_SIZE, progress, playerColor(b.playerId));
      } else {
        drawBuilding(c, name, p.x, p.y, HEX_SIZE, {
          color: playerColor(b.playerId),
          selected: b.id === selectedBuildingId.value,
          phase,
        });
      }
    });
  }
  for (const d of state.value?.donkeys ?? [])
    add(d.x, d.y, () => {
      const p = hexToPixel(d.x, d.y, HEX_SIZE);
      drawSettler(c, p.x, p.y, HEX_SIZE, playerColor(d.playerId), "donkey");
    });
  for (const w of state.value?.workers ?? [])
    add(w.x, w.y, () => {
      const p = hexToPixel(w.x, w.y, HEX_SIZE);
      drawSettler(c, p.x, p.y, HEX_SIZE, playerColor(w.playerId), "worker");
    });
  for (const s of state.value?.soldiers ?? [])
    add(s.x, s.y, () => {
      const p = hexToPixel(s.x, s.y, HEX_SIZE);
      drawSettler(c, p.x, p.y, HEX_SIZE, playerColor(s.playerId), "soldier");
    });
  for (const ship of state.value?.ships ?? [])
    add(ship.x, ship.y, () => {
      const p = hexToPixel(ship.x, ship.y, HEX_SIZE);
      drawShip(c, p.x, p.y, HEX_SIZE, playerColor(ship.playerId), phase);
    });

  items.sort((a, b) => a.y - b.y);
  for (const it of items) it.draw();
}

function drawOverlays(): void {
  const c = ctx!;
  const uiState = useGameUi();
  // Highlight selected empty tile
  if (uiState.selectedTile.value) {
    const center = hexToPixel(uiState.selectedTile.value.x, uiState.selectedTile.value.y, HEX_SIZE);
    tracePath(hexCorners(center, HEX_SIZE));
    c.strokeStyle = "rgba(255, 200, 50, 0.9)";
    c.lineWidth = 2.5;
    c.stroke();
  }

  // Hover highlight.
  if (hover.value) {
    const center = hexToPixel(hover.value.x, hover.value.y, HEX_SIZE);
    tracePath(hexCorners(center, HEX_SIZE));
    c.strokeStyle = "rgba(255,255,255,0.85)";
    c.lineWidth = 2;
    c.stroke();
  }

  // Build tool: a translucent ghost of the chosen building under the cursor.
  if (tool.value.kind === "build" && tool.value.building && hover.value) {
    const p = hexToPixel(hover.value.x, hover.value.y, HEX_SIZE);
    c.save();
    c.globalAlpha = 0.55;
    drawBuilding(c, tool.value.building, p.x, p.y, HEX_SIZE, {
      color: playerColor(session.playerId ?? 0),
      phase: tick.value,
    });
    c.restore();
  }

  // Road draft: highlight the chosen start flag and preview the auto-routed path.
  const draft = roadDraft.value;
  if (draft) {
    const startPx = hexToPixel(draft.start.x, draft.start.y, HEX_SIZE);
    c.strokeStyle = "rgba(110, 231, 120, 0.95)";
    c.lineWidth = 2.5;
    c.beginPath();
    c.arc(startPx.x, startPx.y, HEX_SIZE * 0.55, 0, Math.PI * 2);
    c.stroke();

    const preview = roadPreview.value;
    if (preview && hover.value) {
      // Green = reaches a connectable flag; amber = buildable so far but not a
      // flag yet; red = no buildable route to the cursor.
      const committable = preview.reachable && !!preview.destFlag;
      const pts = [startPx];
      if (preview.reachable && preview.path) {
        for (const node of preview.path) pts.push(hexToPixel(node.x, node.y, HEX_SIZE));
      }
      pts.push(hexToPixel(hover.value.x, hover.value.y, HEX_SIZE));

      c.strokeStyle = committable
        ? "rgba(110, 231, 120, 0.95)"
        : preview.reachable
          ? "rgba(255, 214, 120, 0.9)"
          : "rgba(244, 96, 96, 0.9)";
      c.lineWidth = 3.5;
      c.lineJoin = "round";
      c.lineCap = "round";
      c.setLineDash(preview.reachable ? [6, 4] : [3, 6]);
      c.beginPath();
      c.moveTo(pts[0]!.x, pts[0]!.y);
      for (let i = 1; i < pts.length; i++) c.lineTo(pts[i]!.x, pts[i]!.y);
      c.stroke();
      c.setLineDash([]);

      // Dot each tile the road would occupy, so the route reads clearly.
      if (preview.reachable && preview.path) {
        c.fillStyle = committable ? "rgba(110, 231, 120, 0.95)" : "rgba(255, 214, 120, 0.9)";
        for (const node of preview.path) {
          const np = hexToPixel(node.x, node.y, HEX_SIZE);
          c.beginPath();
          c.arc(np.x, np.y, 2.4, 0, Math.PI * 2);
          c.fill();
        }
      }
    }
  }
}

/* --------------------------------- Input ---------------------------------- */

let dragging = false;
let lastDrag: { x: number; y: number } | null = null;
let moved = false;

function onPointerDown(e: PointerEvent): void {
  // Middle / right button, or left button with no active tool that consumes drag.
  if (e.button === 1 || e.button === 2 || tool.value.kind === "inspect" || e.shiftKey) {
    dragging = true;
    moved = false;
    lastDrag = { x: e.clientX, y: e.clientY };
    canvasRef.value?.setPointerCapture(e.pointerId);
  }
}

function onPointerMove(e: PointerEvent): void {
  hover.value = eventToHex(e);
  if (dragging && lastDrag) {
    const dx = (e.clientX - lastDrag.x) / camera.zoom;
    const dy = (e.clientY - lastDrag.y) / camera.zoom;
    if (Math.abs(e.clientX - lastDrag.x) + Math.abs(e.clientY - lastDrag.y) > 3) moved = true;
    panBy(dx, dy);
    lastDrag = { x: e.clientX, y: e.clientY };
  }
  requestRedraw();
}

function onPointerUp(e: PointerEvent): void {
  if (dragging) {
    dragging = false;
    lastDrag = null;
    canvasRef.value?.releasePointerCapture(e.pointerId);
    if (moved) return; // a drag, not a click
  }
  if (e.button === 0) onClick(eventToHex(e));
}

function onWheel(e: WheelEvent): void {
  e.preventDefault();
  zoomBy(e.deltaY < 0 ? 1.1 : 1 / 1.1);
  requestRedraw();
}

/** Routes a left-click on a tile to the action matching the active tool. */
function onClick(c: Coordinates): void {
  const a = sessionStore.actions;
  switch (tool.value.kind) {
    case "inspect": {
      const b = findBuildingAt(c);
      if (b) {
        selectedBuildingId.value = b.id;
        ui.selectedTile.value = null;
      } else {
        selectedBuildingId.value = null;
        ui.selectedTile.value = c;
      }
      break;
    }
    case "flag":
      a.placeFlag(c);
      log("Flag planted.", "good");
      ui.resetTool();
      break;
    case "build":
      if (tool.value.building) a.build(tool.value.building, c);
      ui.resetTool();
      break;
    case "geologist": {
      const f = findFlagAt(c);
      if (f) {
        a.sendGeologist(f.id);
        log("Geologist dispatched.", "info");
      }
      ui.resetTool();
      break;
    }
    case "scout": {
      const f = findFlagAt(c);
      if (f) {
        a.sendScout(f.id);
        log("Scout dispatched.", "info");
      }
      ui.resetTool();
      break;
    }
    case "destroy": {
      const b = findBuildingAt(c);
      if (b && b.playerId === session.playerId) {
        a.destroy(b.id);
        log("Building demolished.", "warn");
      }
      ui.resetTool();
      break;
    }
    case "attack": {
      const b = findBuildingAt(c);
      if (b && b.playerId !== session.playerId) {
        a.attack(b.id, ui.attackerCount.value);
        const n = ui.attackerCount.value;
        log(n > 0 ? `Attack launched with up to ${n} soldiers!` : "Attack launched!", "bad");
      }
      ui.resetTool();
      break;
    }
    case "road":
      handleRoadClick(c);
      break;
  }
}

/** Lays a road flag-to-flag: the first click picks the start flag, the second
 *  picks the target flag and the shortest buildable path between them is routed
 *  automatically (the route is previewed live as the cursor moves). */
function handleRoadClick(c: Coordinates): void {
  const a = sessionStore.actions;
  const draft = roadDraft.value;
  const flag = findFlagAt(c);

  if (!draft) {
    if (flag) roadDraft.value = { startFlagId: flag.id, start: { x: flag.x, y: flag.y } };
    else toast.info("Pick a flag to start the road.");
    return;
  }

  // Clicking the start flag again cancels the draft.
  if (flag && flag.id === draft.startFlagId) {
    roadDraft.value = null;
    return;
  }

  // Clicking another flag commits the auto-routed road, if one is buildable.
  if (flag) {
    const path = findRoadPath(
      { tileAt, blocked: roadBlockedKeys.value },
      draft.start,
      { x: flag.x, y: flag.y },
    );
    if (!path) {
      toast.error("No buildable road reaches that flag.");
      return;
    }
    a.linkFlags(draft.startFlagId, flag.id, path);
    log(`Road built (${path.length + 1} segments).`, "good");
    roadDraft.value = null;
    ui.resetTool();
    return;
  }

  // Clicking empty ground does nothing — routing only needs the two flags.
}

/* ------------------------------- Lifecycle -------------------------------- */

function resize(): void {
  const el = containerRef.value;
  const canvas = canvasRef.value;
  if (!el || !canvas) return;
  dpr = window.devicePixelRatio || 1;
  cssW.value = el.clientWidth;
  cssH.value = el.clientHeight;
  canvas.width = Math.round(cssW.value * dpr);
  canvas.height = Math.round(cssH.value * dpr);
  canvas.style.width = `${cssW.value}px`;
  canvas.style.height = `${cssH.value}px`;
  setViewport(cssW.value, cssH.value);
  requestRedraw();
}

onMounted(() => {
  ctx = canvasRef.value!.getContext("2d");
  resize();
  resizeObserver = new ResizeObserver(resize);
  resizeObserver.observe(containerRef.value!);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  if (redrawHandle) cancelAnimationFrame(redrawHandle);
});

// Centre on the player's headquarters the first time the state arrives.
let centred = false;
watch(
  () => state.value,
  (s) => {
    if (centred || !s) return;
    const hq =
      s.buildings.find((b) => b.playerId === session.playerId && b.name === "HEADQUARTERS") ??
      s.buildings[0];
    if (hq) {
      centerOn(hexToPixel(hq.x, hq.y, HEX_SIZE));
      centred = true;
    }
    requestRedraw();
  },
);

watch([map, state, () => camera.x, () => camera.y, () => camera.zoom, roadDraft, () => tool.value, () => ui.selectedTile.value], requestRedraw, {
  deep: true,
});
</script>

<template>
  <div ref="containerRef" class="relative h-full w-full overflow-hidden">
    <canvas
      ref="canvasRef"
      class="block h-full w-full touch-none select-none"
      :class="{
        'cursor-grab': tool.kind === 'inspect',
        'cursor-crosshair': tool.kind !== 'inspect',
      }"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointerleave="hover = null"
      @wheel="onWheel"
      @contextmenu.prevent
    />
  </div>
</template>
