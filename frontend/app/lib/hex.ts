/**
 * Doubled-height hexagonal grid maths (flat-top hexagons).
 *
 * The backend stores tiles in *doubled-height* coordinates where a tile is
 * valid when `x + y` is even. Direct neighbours are N, NE, SE, S, SW, NW.
 * See {@link https://www.redblobgames.com/grids/hexagons/#coordinates-doubled}.
 *
 * Pixel placement for a hexagon of circum-radius `size`:
 *   px = size * 3/2 * x
 *   py = size * sqrt(3)/2 * y
 */
import type { Coordinates } from "~/types/game";

const SQRT3 = Math.sqrt(3);

/** The six flat-top neighbour offsets in doubled-height coordinates. */
export const HEX_DIRECTIONS: readonly Coordinates[] = [
  { x: 0, y: -2 }, // N
  { x: 1, y: -1 }, // NE
  { x: 1, y: 1 }, // SE
  { x: 0, y: 2 }, // S
  { x: -1, y: 1 }, // SW
  { x: -1, y: -1 }, // NW
] as const;

/** A pixel-space point. */
export interface Point {
  x: number;
  y: number;
}

/**
 * Converts a hex coordinate to the pixel position of its centre.
 *
 * @param x    doubled column
 * @param y    doubled row
 * @param size hexagon circum-radius in pixels
 */
export function hexToPixel(x: number, y: number, size: number): Point {
  return { x: size * 1.5 * x, y: size * (SQRT3 / 2) * y };
}

/**
 * Returns the six corner points of the flat-top hexagon centred at `center`.
 *
 * @param center hexagon centre in pixels
 * @param size   hexagon circum-radius in pixels
 */
export function hexCorners(center: Point, size: number): Point[] {
  const corners: Point[] = [];
  for (let i = 0; i < 6; i++) {
    const angle = (Math.PI / 180) * (60 * i);
    corners.push({
      x: center.x + size * Math.cos(angle),
      y: center.y + size * Math.sin(angle),
    });
  }
  return corners;
}

/**
 * Finds the nearest valid hex tile (where `x + y` is even) to a pixel position.
 * Used to translate pointer clicks back into grid coordinates.
 *
 * @param px   pixel x in world space
 * @param py   pixel y in world space
 * @param size hexagon circum-radius in pixels
 */
export function pixelToHex(px: number, py: number, size: number): Coordinates {
  const fx = px / (size * 1.5);
  const fy = py / (size * (SQRT3 / 2));

  // Candidate columns/rows around the fractional position; the valid tile is
  // whichever even-parity centre is closest in actual pixel distance.
  let best: Coordinates = { x: 0, y: 0 };
  let bestDist = Infinity;
  for (const cx of [Math.floor(fx), Math.ceil(fx)]) {
    for (const cy of [Math.floor(fy), Math.ceil(fy)]) {
      if (((cx + cy) % 2 + 2) % 2 !== 0) continue;
      const c = hexToPixel(cx, cy, size);
      const d = (c.x - px) ** 2 + (c.y - py) ** 2;
      if (d < bestDist) {
        bestDist = d;
        best = { x: cx, y: cy };
      }
    }
  }
  return best;
}

/** Hex distance (number of steps) between two doubled-height coordinates. */
export function hexDistance(a: Coordinates, b: Coordinates): number {
  const dx = Math.abs(a.x - b.x);
  const dy = Math.abs(a.y - b.y);
  return dx + Math.max(0, (dy - dx) / 2);
}

/** Whether two coordinates are the same tile. */
export function sameHex(a: Coordinates, b: Coordinates): boolean {
  return a.x === b.x && a.y === b.y;
}

/** Whether `b` is a direct hex neighbour of `a`. */
export function isNeighbor(a: Coordinates, b: Coordinates): boolean {
  return HEX_DIRECTIONS.some((d) => a.x + d.x === b.x && a.y + d.y === b.y);
}

/** Stable string key for a coordinate, suitable for Map/Set lookups. */
export function hexKey(x: number, y: number): string {
  return `${x},${y}`;
}
