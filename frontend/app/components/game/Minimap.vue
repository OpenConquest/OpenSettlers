<script setup lang="ts">
/**
 * Minimap: a downscaled top-down view of the whole island drawn on its own
 * canvas. Terrain is painted from the static map, ownership from the live
 * territory, and a click recentres the main camera on the matching world point.
 */
import { onMounted, ref, watch } from "vue";
import { hexToPixel } from "~/lib/hex";
import { TILE_COLORS, playerColor } from "~/lib/palette";

const SIZE = 180; // pixel side of the minimap canvas
const HEX_SIZE = 14; // must match GameCanvas to map clicks back to world space

const { map, state } = useGameSession();
const { camera, centerOn } = useCamera();

const canvasRef = ref<HTMLCanvasElement | null>(null);

/** World-space bounds of the map, recomputed when the static map changes. */
let bounds = { minX: 0, minY: 0, maxX: 1, maxY: 1 };

function computeBounds(): void {
  if (!map.value) return;
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;
  for (const t of map.value.tiles.values()) {
    const p = hexToPixel(t.x, t.y, HEX_SIZE);
    minX = Math.min(minX, p.x);
    minY = Math.min(minY, p.y);
    maxX = Math.max(maxX, p.x);
    maxY = Math.max(maxY, p.y);
  }
  bounds = { minX, minY, maxX, maxY };
}

function draw(): void {
  const canvas = canvasRef.value;
  const ctx = canvas?.getContext("2d");
  if (!canvas || !ctx || !map.value) return;

  const w = bounds.maxX - bounds.minX || 1;
  const h = bounds.maxY - bounds.minY || 1;
  const scale = Math.min(SIZE / w, SIZE / h);

  ctx.clearRect(0, 0, SIZE, SIZE);
  ctx.fillStyle = "#10181f";
  ctx.fillRect(0, 0, SIZE, SIZE);

  const px = (wx: number) => (wx - bounds.minX) * scale;
  const py = (wy: number) => (wy - bounds.minY) * scale;
  const dot = Math.max(1.5, scale * HEX_SIZE * 1.1);

  for (const t of map.value.tiles.values()) {
    const p = hexToPixel(t.x, t.y, HEX_SIZE);
    ctx.fillStyle = TILE_COLORS[t.tileType];
    ctx.fillRect(px(p.x), py(p.y), dot, dot);
  }
  for (const [x, y, owner] of state.value?.territory ?? []) {
    if (owner < 0) continue;
    const p = hexToPixel(x, y, HEX_SIZE);
    ctx.fillStyle = playerColor(owner);
    ctx.fillRect(px(p.x), py(p.y), dot, dot);
  }

  drawViewport(ctx, px, py, scale);
}

/** Outlines the slice of the world the main camera currently shows. */
function drawViewport(
  ctx: CanvasRenderingContext2D,
  px: (wx: number) => number,
  py: (wy: number) => number,
  scale: number,
): void {
  if (!camera.viewW || !camera.viewH) return;
  const halfW = camera.viewW / 2 / camera.zoom;
  const halfH = camera.viewH / 2 / camera.zoom;
  ctx.strokeStyle = "rgba(255, 255, 255, 0.85)";
  ctx.lineWidth = 1;
  ctx.strokeRect(
    px(camera.x - halfW),
    py(camera.y - halfH),
    2 * halfW * scale,
    2 * halfH * scale,
  );
}

function onClick(e: MouseEvent): void {
  const canvas = canvasRef.value;
  if (!canvas || !map.value) return;
  const rect = canvas.getBoundingClientRect();
  const w = bounds.maxX - bounds.minX || 1;
  const h = bounds.maxY - bounds.minY || 1;
  const scale = Math.min(SIZE / w, SIZE / h);
  centerOn({
    x: bounds.minX + (e.clientX - rect.left) / scale,
    y: bounds.minY + (e.clientY - rect.top) / scale,
  });
}

onMounted(() => {
  computeBounds();
  draw();
});

watch(map, () => {
  computeBounds();
  draw();
});
watch(() => state.value?.territory, draw);
// Keep the viewport rectangle in sync as the player pans or zooms.
watch(() => [camera.x, camera.y, camera.zoom, camera.viewW, camera.viewH], draw);
</script>

<template>
  <div class="absolute right-4 top-16 rounded-lg border border-border bg-card/90 p-1.5 shadow-lg backdrop-blur">
    <canvas
      ref="canvasRef"
      :width="SIZE"
      :height="SIZE"
      class="cursor-pointer rounded"
      @click="onClick"
    />
  </div>
</template>
