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
  isNeighbor,
  pixelToHex,
  sameHex,
  type Point,
} from "~/lib/hex";
import { PLAYER_COLORS, TILE_COLORS, playerColor } from "~/lib/palette";
import { BUILDINGS } from "~/lib/buildings";
import type { BuildingDto, Coordinates, FlagDto } from "~/types/game";

const HEX_SIZE = 14; // world-space circum-radius of one tile

const { map, state, session } = useGameSession();
const { tool, selectedBuildingId, roadDraft } = useGameUi();
const ui = useGameUi();
const sessionStore = useGameSession();
const { camera, panBy, zoomBy, centerOn, setViewport } = useCamera();

const containerRef = ref<HTMLDivElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const hover = ref<Coordinates | null>(null);

let ctx: CanvasRenderingContext2D | null = null;
let cssW = 0;
let cssH = 0;
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

/* ------------------------------ Coordinate maths --------------------------- */

function screenToWorld(sx: number, sy: number): Point {
  return {
    x: camera.x + (sx - cssW / 2) / camera.zoom,
    y: camera.y + (sy - cssH / 2) / camera.zoom,
  };
}

function eventToHex(e: PointerEvent | MouseEvent): Coordinates {
  const rect = canvasRef.value!.getBoundingClientRect();
  const w = screenToWorld(e.clientX - rect.left, e.clientY - rect.top);
  return pixelToHex(w.x, w.y, HEX_SIZE);
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
  ctx!.translate(cssW / 2, cssH / 2);
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
  c.clearRect(0, 0, cssW, cssH);
  c.fillStyle = "#1b2a36";
  c.fillRect(0, 0, cssW, cssH);
  applyCameraTransform();

  // Viewport bounds in world space (with a one-tile margin) for culling.
  const tl = screenToWorld(0, 0);
  const br = screenToWorld(cssW, cssH);
  const margin = HEX_SIZE * 2;

  drawTerrain(tl, br, margin);
  drawRoads();
  drawFlags();
  drawBuildings();
  drawUnits();
  drawSigns();
  drawOverlays();
}

function drawTerrain(tl: Point, br: Point, margin: number): void {
  const c = ctx!;
  const explored = exploredKeys.value;
  for (const tile of map.value!.tiles.values()) {
    const center = hexToPixel(tile.x, tile.y, HEX_SIZE);
    if (
      center.x < tl.x - margin ||
      center.x > br.x + margin ||
      center.y < tl.y - margin ||
      center.y > br.y + margin
    ) {
      continue;
    }

    const key = hexKey(tile.x, tile.y);
    const isExplored = !explored || explored.has(key);
    const corners = hexCorners(center, HEX_SIZE + 0.5);
    tracePath(corners);

    c.fillStyle = TILE_COLORS[tile.tileType];
    c.fill();

    // Territory tint.
    const owner = territoryByKey.value.get(key);
    if (owner != null && owner >= 0) {
      c.fillStyle = playerColor(owner) + "44";
      c.fill();
    }

    // Fog of war: darken unexplored tiles.
    if (!isExplored) {
      c.fillStyle = "rgba(8, 14, 20, 0.78)";
      c.fill();
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

    c.strokeStyle = road.level >= 2 ? "#caa472" : "#9b7b53";
    c.lineWidth = (road.level >= 2 ? 4 : 2.5) / camera.zoom + 1.5;
    c.lineJoin = "round";
    c.lineCap = "round";
    c.beginPath();
    c.moveTo(pts[0]!.x, pts[0]!.y);
    for (let i = 1; i < pts.length; i++) c.lineTo(pts[i]!.x, pts[i]!.y);
    c.stroke();

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
  c.fillStyle = "#f4e4c1";
  c.beginPath();
  c.arc(x, y, 3, 0, Math.PI * 2);
  c.fill();
}

function drawFlags(): void {
  const c = ctx!;
  for (const flag of state.value?.flags ?? []) {
    const p = hexToPixel(flag.x, flag.y, HEX_SIZE);
    const col = playerColor(flag.playerId);
    // Pole.
    c.strokeStyle = "#3a2e22";
    c.lineWidth = 1.5;
    c.beginPath();
    c.moveTo(p.x, p.y);
    c.lineTo(p.x, p.y - HEX_SIZE * 0.7);
    c.stroke();
    // Pennant.
    c.fillStyle = col;
    c.beginPath();
    c.moveTo(p.x, p.y - HEX_SIZE * 0.7);
    c.lineTo(p.x + HEX_SIZE * 0.5, p.y - HEX_SIZE * 0.55);
    c.lineTo(p.x, p.y - HEX_SIZE * 0.4);
    c.closePath();
    c.fill();
    // Waiting resources as little dots.
    flag.resources.slice(0, 6).forEach((_, idx) => {
      c.fillStyle = "#e8d39a";
      c.beginPath();
      c.arc(p.x - 4 + (idx % 3) * 4, p.y + 4 + Math.floor(idx / 3) * 4, 1.6, 0, Math.PI * 2);
      c.fill();
    });
  }
}

function drawBuildings(): void {
  const c = ctx!;
  for (const b of state.value?.buildings ?? []) {
    const p = hexToPixel(b.x, b.y, HEX_SIZE);
    const r = HEX_SIZE * 0.78;
    const col = playerColor(b.playerId);

    c.fillStyle = b.underConstruction ? "rgba(70, 55, 35, 0.75)" : "#e9e2d4";
    c.strokeStyle = col;
    c.lineWidth = 2;
    c.beginPath();
    c.arc(p.x, p.y, r, 0, Math.PI * 2);
    c.fill();
    c.stroke();

    const meta = b.name ? BUILDINGS[b.name] : null;
    if (meta) {
      c.font = `${r * 1.2}px serif`;
      c.textAlign = "center";
      c.textBaseline = "middle";
      c.globalAlpha = b.underConstruction ? 0.5 : 1;
      c.fillText(meta.icon, p.x, p.y + 1);
      c.globalAlpha = 1;
    }

    if (b.underConstruction) drawConstruction(p, r, b);
    if (b.id === selectedBuildingId.value) {
      c.strokeStyle = "#ffffff";
      c.lineWidth = 2.5;
      c.beginPath();
      c.arc(p.x, p.y, r + 3, 0, Math.PI * 2);
      c.stroke();
    }
  }
}

function drawConstruction(p: Point, r: number, b: BuildingDto): void {
  const c = ctx!;
  const groundwork = b.groundworkProgress ?? 0;
  const building = b.buildingProgress ?? 0;
  const pct = Math.min(100, (groundwork + building) / 2);
  c.strokeStyle = "#9fd0ff";
  c.lineWidth = 2.5;
  c.beginPath();
  c.arc(p.x, p.y, r + 3, -Math.PI / 2, -Math.PI / 2 + (Math.PI * 2 * pct) / 100);
  c.stroke();
}

function drawUnits(): void {
  const c = ctx!;
  const dot = (x: number, y: number, color: string, radius: number) => {
    const p = hexToPixel(x, y, HEX_SIZE);
    c.fillStyle = color;
    c.beginPath();
    c.arc(p.x, p.y, radius, 0, Math.PI * 2);
    c.fill();
  };
  for (const w of state.value?.workers ?? []) dot(w.x, w.y, "#f5e9c8", 2.4);
  for (const d of state.value?.donkeys ?? []) dot(d.x, d.y, "#c9a66b", 2.8);
  for (const s of state.value?.soldiers ?? []) {
    const p = hexToPixel(s.x, s.y, HEX_SIZE);
    c.fillStyle = playerColor(s.playerId);
    c.strokeStyle = "#1a1a1a";
    c.lineWidth = 1;
    c.beginPath();
    c.arc(p.x, p.y, 3.2, 0, Math.PI * 2);
    c.fill();
    c.stroke();
  }
  for (const ship of state.value?.ships ?? []) {
    const p = hexToPixel(ship.x, ship.y, HEX_SIZE);
    c.fillStyle = playerColor(ship.playerId);
    c.fillRect(p.x - 4, p.y - 2.5, 8, 5);
  }
}

function drawSigns(): void {
  const c = ctx!;
  for (const sign of state.value?.signs ?? []) {
    const p = hexToPixel(sign.x, sign.y, HEX_SIZE);
    c.fillStyle = sign.resource ? "#ffe08a" : "#6b6b6b";
    c.font = `${HEX_SIZE}px serif`;
    c.textAlign = "center";
    c.textBaseline = "middle";
    c.fillText(sign.resource ? "✦" : "✕", p.x, p.y);
  }
}

function drawOverlays(): void {
  const c = ctx!;
  // Hover highlight.
  if (hover.value) {
    const center = hexToPixel(hover.value.x, hover.value.y, HEX_SIZE);
    tracePath(hexCorners(center, HEX_SIZE));
    c.strokeStyle = "rgba(255,255,255,0.85)";
    c.lineWidth = 2;
    c.stroke();

    // Ghost preview for build / flag tools.
    if (tool.value.kind === "build" && tool.value.building) {
      c.font = `${HEX_SIZE * 1.1}px serif`;
      c.textAlign = "center";
      c.textBaseline = "middle";
      c.globalAlpha = 0.55;
      c.fillText(BUILDINGS[tool.value.building].icon, center.x, center.y);
      c.globalAlpha = 1;
    }
  }

  // Road draft preview.
  const draft = roadDraft.value;
  if (draft) {
    const pts = [hexToPixel(draft.start.x, draft.start.y, HEX_SIZE)];
    for (const node of draft.path) pts.push(hexToPixel(node.x, node.y, HEX_SIZE));
    if (hover.value) pts.push(hexToPixel(hover.value.x, hover.value.y, HEX_SIZE));
    c.strokeStyle = "rgba(255, 230, 150, 0.85)";
    c.lineWidth = 3;
    c.setLineDash([5, 4]);
    c.beginPath();
    c.moveTo(pts[0]!.x, pts[0]!.y);
    for (let i = 1; i < pts.length; i++) c.lineTo(pts[i]!.x, pts[i]!.y);
    c.stroke();
    c.setLineDash([]);
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
      selectedBuildingId.value = b ? b.id : null;
      break;
    }
    case "flag":
      a.placeFlag(c);
      ui.resetTool();
      break;
    case "build":
      if (tool.value.building) a.build(tool.value.building, c);
      ui.resetTool();
      break;
    case "geologist": {
      const f = findFlagAt(c);
      if (f) a.sendGeologist(f.id);
      ui.resetTool();
      break;
    }
    case "destroy": {
      const b = findBuildingAt(c);
      if (b && b.playerId === session.playerId) a.destroy(b.id);
      ui.resetTool();
      break;
    }
    case "attack": {
      const b = findBuildingAt(c);
      if (b && b.playerId !== session.playerId) a.attack(b.id);
      ui.resetTool();
      break;
    }
    case "road":
      handleRoadClick(c);
      break;
  }
}

/** Lays a road flag-to-flag: first click picks the start flag, each later click
 *  extends the path one adjacent tile, finishing when another flag is reached. */
function handleRoadClick(c: Coordinates): void {
  const a = sessionStore.actions;
  const draft = roadDraft.value;
  const flag = findFlagAt(c);

  if (!draft) {
    if (flag) roadDraft.value = { startFlagId: flag.id, start: { x: flag.x, y: flag.y }, path: [] };
    return;
  }

  const last = draft.path.length ? draft.path[draft.path.length - 1]! : draft.start;
  if (!isNeighbor(last, c)) return; // roads advance one tile at a time

  if (flag && !sameHex(c, draft.start)) {
    a.linkFlags(draft.startFlagId, flag.id, draft.path);
    roadDraft.value = null;
    ui.resetTool();
    return;
  }
  roadDraft.value = { ...draft, path: [...draft.path, c] };
}

/* ------------------------------- Lifecycle -------------------------------- */

function resize(): void {
  const el = containerRef.value;
  const canvas = canvasRef.value;
  if (!el || !canvas) return;
  dpr = window.devicePixelRatio || 1;
  cssW = el.clientWidth;
  cssH = el.clientHeight;
  canvas.width = Math.round(cssW * dpr);
  canvas.height = Math.round(cssH * dpr);
  canvas.style.width = `${cssW}px`;
  canvas.style.height = `${cssH}px`;
  setViewport(cssW, cssH);
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

watch([map, state, () => camera.x, () => camera.y, () => camera.zoom, roadDraft, () => tool.value], requestRedraw, {
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
