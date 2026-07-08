/**
 * Shared 2D camera (pan + zoom) over the world-space map. The canvas renderer
 * reads it to build its transform; the minimap writes to it to recentre.
 */
import { reactive } from "vue";
import type { Point } from "~/lib/hex";

const MIN_ZOOM = 0.25;
const MAX_ZOOM = 3;

const camera = reactive({
  /** World-space point shown at the centre of the viewport. */
  x: 0,
  y: 0,
  zoom: 1,
  /** Size of the main canvas in CSS pixels, published by the renderer. */
  viewW: 0,
  viewH: 0,
});

/** Records the current viewport size so other views (the minimap) can draw it. */
function setViewport(w: number, h: number): void {
  camera.viewW = w;
  camera.viewH = h;
}

/** Pans the camera by a screen-space delta (already divided by zoom). */
function panBy(dx: number, dy: number): void {
  camera.x -= dx;
  camera.y -= dy;
}

/** Centres the camera on a world-space point. */
function centerOn(p: Point): void {
  camera.x = p.x;
  camera.y = p.y;
}

/** Multiplies the zoom, clamped to the allowed range. */
function zoomBy(factor: number): void {
  camera.zoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, camera.zoom * factor));
}

export function useCamera() {
  return { camera, panBy, centerOn, zoomBy, setViewport, MIN_ZOOM, MAX_ZOOM };
}
