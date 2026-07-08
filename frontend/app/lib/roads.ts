/**
 * Road pathfinding over the hex terrain grid.
 *
 * Roads run flag-to-flag as a chain of adjacent tiles. A tile can carry a road
 * when it is walkable land (not water or mountain), the elevation change from
 * the previous tile stays within {@link MAX_ROAD_ELEVATION_DELTA}, and it is not
 * already occupied by a building or another flag. This mirrors the backend's
 * road rules (`MapTile.isWalkable` / `canConnectRoadTo`) so a previewed path is
 * one the server will accept.
 */
import type { Coordinates, TileDto } from "~/types/game";
import { hexKey, hexNeighbors } from "./hex";

/** Largest elevation difference a road may climb between two adjacent tiles. */
export const MAX_ROAD_ELEVATION_DELTA = 2;

/** Whether a tile's terrain can ever carry a road (walkable land). */
export function isRoadableTerrain(tile: TileDto | undefined | null): boolean {
  return !!tile && tile.tileType !== "WATER" && tile.tileType !== "MOUNTAIN";
}

/** Terrain and occupancy lookup the road pathfinder reads from. */
export interface RoadGrid {
  /** Resolves the static tile at a coordinate, or `undefined` off-map. */
  tileAt(c: Coordinates): TileDto | undefined;
  /** Keys of tiles occupied by a building or flag; the two endpoints are exempt. */
  blocked: Set<string>;
}

/**
 * Computes the shortest road between two flag tiles over buildable terrain.
 *
 * Breadth-first search, so the result is the fewest-tile route. The endpoints
 * are assumed to be flags and are never rejected for terrain or occupancy; every
 * tile in between must be roadable land, within the elevation limit of its
 * predecessor, and unoccupied.
 *
 * @param grid terrain + occupancy lookup
 * @param from start flag coordinate
 * @param to   destination flag coordinate
 * @returns the intermediate tiles (endpoints excluded, possibly empty for
 *          adjacent flags), or `null` if no buildable route exists
 */
export function findRoadPath(
  grid: RoadGrid,
  from: Coordinates,
  to: Coordinates,
): Coordinates[] | null {
  const startKey = hexKey(from.x, from.y);
  const goalKey = hexKey(to.x, to.y);
  if (startKey === goalKey) return null;

  const cameFrom = new Map<string, string>();
  const coordOf = new Map<string, Coordinates>([
    [startKey, from],
    [goalKey, to],
  ]);
  const visited = new Set<string>([startKey]);
  const queue: Coordinates[] = [from];

  for (let head = 0; head < queue.length; head++) {
    const cur = queue[head]!;
    const curKey = hexKey(cur.x, cur.y);
    if (curKey === goalKey) break;
    const curTile = grid.tileAt(cur);

    for (const nb of hexNeighbors(cur)) {
      const key = hexKey(nb.x, nb.y);
      if (visited.has(key)) continue;

      const isGoal = key === goalKey;
      const tile = grid.tileAt(nb);
      if (!isGoal) {
        if (!isRoadableTerrain(tile)) continue;
        if (grid.blocked.has(key)) continue;
      }
      // Roads cannot climb a steeper step than the elevation limit allows.
      if (
        curTile &&
        tile &&
        Math.abs(curTile.elevation - tile.elevation) > MAX_ROAD_ELEVATION_DELTA
      ) {
        continue;
      }

      visited.add(key);
      cameFrom.set(key, curKey);
      coordOf.set(key, nb);
      queue.push(nb);
    }
  }

  if (!cameFrom.has(goalKey)) return null;

  // Walk the predecessor chain back to the start, then drop both endpoints.
  const chain: Coordinates[] = [];
  let key: string | undefined = goalKey;
  while (key) {
    chain.unshift(coordOf.get(key)!);
    key = cameFrom.get(key);
  }
  return chain.slice(1, -1);
}
