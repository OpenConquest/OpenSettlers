/**
 * Centralised colour palette and small visual helpers used by the renderer and
 * the HUD. Keeping every colour here makes the look-and-feel easy to retune.
 */
import type { ResourceType, TileType } from "~/types/game";

/** Base fill colour for each terrain type. */
export const TILE_COLORS: Record<TileType, string> = {
  GRASS: "#6aa84f",
  WATER: "#3d85c6",
  MOUNTAIN: "#8a7f72",
  FOREST: "#38761d",
  STONE: "#9e9e9e",
  DESERT: "#e6c27a",
  HILLS: "#a8c47a",
  FIELD: "#c9a227",
};

/**
 * Player colours, indexed by player id (0..3). Index 0 is reserved for the
 * local player's "blue", matching the classic Settlers palette.
 */
export const PLAYER_COLORS: readonly string[] = [
  "#2f6fed", // blue
  "#e23b3b", // red
  "#f2a900", // yellow
  "#39b54a", // green
];

/** Returns a player's colour, wrapping for any unexpected id. */
export function playerColor(playerId: number): string {
  return PLAYER_COLORS[((playerId % PLAYER_COLORS.length) + PLAYER_COLORS.length) % PLAYER_COLORS.length]!;
}

/** Short single-glyph icon for each resource, used on flags and in tooltips. */
export const RESOURCE_ICONS: Record<ResourceType, string> = {
  LOG: "🪵",
  PLANK: "🪚",
  STONE: "🪨",
  IRON: "⛓️",
  STEEL: "🔩",
  COAL: "⚫",
  GOLD: "🟡",
  COIN: "🪙",
  WHEAT: "🌾",
  FLOUR: "🌾",
  BREAD: "🍞",
  FISH: "🐟",
  PIG: "🐖",
  MEAT: "🍖",
  DONKEY: "🫏",
  BEER: "🍺",
  SWORD: "⚔️",
  SHIELD: "🛡️",
  TOOL: "🔧",
  WATER: "💧",
};

/** Human-readable label for a resource type. */
export function resourceLabel(resource: ResourceType): string {
  return resource.charAt(0) + resource.slice(1).toLowerCase();
}
