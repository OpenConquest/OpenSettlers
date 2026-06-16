/**
 * Centralised colour palette and small visual helpers used by the renderer and
 * the HUD. Keeping every colour here makes the look-and-feel easy to retune.
 */
import type { BuildingName, ResourceType, TileType } from "~/types/game";
import { TreePine, Blocks, Gem, Anvil, Diamond, Flame, CircleDollarSign, Coins, Wheat, Cloud, Croissant, Fish, PiggyBank, Beef, Carrot, Beer, Sword, Shield, Wrench, Droplets } from '@lucide/vue';
import type { FunctionalComponent, SVGAttributes } from 'vue';

/** Base fill colour for each terrain type, tuned for the lush Settlers look. */
export const TILE_COLORS: Record<TileType, string> = {
  GRASS: "#6fa83c",
  WATER: "#2f7fb8",
  MOUNTAIN: "#8d8377",
  FOREST: "#3c7a26",
  STONE: "#9a9488",
  DESERT: "#dcc285",
  HILLS: "#9cb85f",
  FIELD: "#d3a829",
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
export const RESOURCE_ICONS: Record<ResourceType, FunctionalComponent<SVGAttributes>> = {
  LOG: TreePine,
  PLANK: Blocks,
  STONE: Gem,
  IRON: Diamond,
  STEEL: Anvil,
  COAL: Flame,
  GOLD: CircleDollarSign,
  COIN: Coins,
  WHEAT: Wheat,
  FLOUR: Cloud,
  BREAD: Croissant,
  FISH: Fish,
  PIG: PiggyBank,
  MEAT: Beef,
  DONKEY: Carrot,
  BEER: Beer,
  SWORD: Sword,
  SHIELD: Shield,
  TOOL: Wrench,
  WATER: Droplets,
};

/** Human-readable label for a resource type. */
export function resourceLabel(resource: ResourceType): string {
  return resource.charAt(0) + resource.slice(1).toLowerCase();
}

/**
 * Noun describing a *natural-resource node* sitting on a tile, used by the tile
 * inspector ("38 Trees", "12 Iron ore", …). Only the resources that occur as
 * map nodes are listed.
 */
export const RESOURCE_NODE_NOUN: Partial<Record<ResourceType, string>> = {
  LOG: "Trees",
  STONE: "Stone",
  COAL: "Coal",
  IRON: "Iron ore",
  GOLD: "Gold ore",
  FISH: "Fish",
  WATER: "Water",
  WHEAT: "Wheat",
};

/* -------------------------------------------------------------------------- */
/*  Procedural building & terrain styling (consumed by lib/sprites.ts)        */
/* -------------------------------------------------------------------------- */

/**
 * Drawing archetypes for the procedural building renderer. Each maps to one
 * shape routine in {@link import("./sprites")}; many buildings share a routine
 * and differ only by their {@link BuildingStyle} colours.
 */
export type BuildingArchetype =
  | "hut" // small one-worker shack
  | "house" // medium production house
  | "farm" // house beside a fenced crop patch
  | "industry" // workshop with a smoking chimney
  | "mill" // windmill with turning sails
  | "store" // long low storehouse (warehouse)
  | "keep" // the gold-domed headquarters
  | "tower" // crenellated military building (sized by tier)
  | "well" // roofed stone well
  | "mine" // cave entrance set in a rocky mound
  | "harbor" // storehouse with a timber dock
  | "shipyard" // open hall with a ship on the slipway
  | "catapult"; // siege engine

/** Wall / roof colours, archetype and optional sizing tier for one building. */
export interface BuildingStyle {
  archetype: BuildingArchetype;
  wall: string;
  roof: string;
  /** Glow/ember accent for forges and the mint; smoke colour otherwise. */
  accent?: string;
  /** Relative size for military {@link tower}s and a few others (0 = smallest). */
  tier?: number;
}

/**
 * The single source of truth for how every building is drawn. Colours follow the
 * lush Settlers II palette: warm timber walls and terracotta roofs, grey stone
 * for fortifications, gold for the headquarters dome.
 */
export const BUILDING_STYLE: Record<BuildingName, BuildingStyle> = {
  HEADQUARTERS: { archetype: "keep", wall: "#dcd6c6", roof: "#a83b2a", accent: "#e8c34d" },
  WAREHOUSE: { archetype: "store", wall: "#d9c39a", roof: "#7d5a3a" },

  WOODCUTTER: { archetype: "hut", wall: "#caa276", roof: "#8a5a34" },
  FORESTER: { archetype: "hut", wall: "#bfa06f", roof: "#5f7d3a" },
  QUARRY: { archetype: "hut", wall: "#cfc7b6", roof: "#7c6f5b" },
  MINE: { archetype: "mine", wall: "#8a7d6b", roof: "#544a40" },
  WATER_WELL: { archetype: "well", wall: "#cfc9bd", roof: "#8a5a34" },

  FARM: { archetype: "farm", wall: "#e7d6a8", roof: "#b5432f" },
  FISHING_HUT: { archetype: "hut", wall: "#cdb78d", roof: "#3f6f8a" },
  HUNTERS_HUT: { archetype: "hut", wall: "#bfa06f", roof: "#6b4a2c" },
  MILL: { archetype: "mill", wall: "#e3cf9f", roof: "#9c3f2c" },
  BAKERY: { archetype: "industry", wall: "#e7d6b0", roof: "#b5432f", accent: "#d8d8d8" },
  PIG_FARM: { archetype: "farm", wall: "#e0caa0", roof: "#9c6b3f" },
  SLAUGHTERHOUSE: { archetype: "house", wall: "#e0c8a4", roof: "#a23a2a" },

  SAWMILL: { archetype: "industry", wall: "#d8c194", roof: "#8a5a34", accent: "#c9c9c9" },
  FOUNDRY: { archetype: "industry", wall: "#b7a98f", roof: "#52483c", accent: "#ff8a3c" },
  ARMORY: { archetype: "industry", wall: "#c3b48f", roof: "#5a4636", accent: "#ffb24a" },
  MINT: { archetype: "industry", wall: "#d8c79a", roof: "#7a5a32", accent: "#ffd95a" },
  METALWORKS: { archetype: "industry", wall: "#c8b68f", roof: "#6b5436", accent: "#c9c9c9" },
  BREWERY: { archetype: "industry", wall: "#e0cb9a", roof: "#8a5a34", accent: "#e8e0c8" },
  DONKEY_BREEDER: { archetype: "farm", wall: "#d8c194", roof: "#8a5a34" },

  BARRACKS: { archetype: "tower", wall: "#ccc6b8", roof: "#7c4636", tier: 0 },
  GUARD_HOUSE: { archetype: "tower", wall: "#cac4b4", roof: "#7c4636", tier: 1 },
  WATCH_TOWER: { archetype: "tower", wall: "#c4beb0", roof: "#6f4030", tier: 2 },
  CASTLE: { archetype: "tower", wall: "#c0baac", roof: "#6a3c2c", tier: 3 },
  FORTRESS: { archetype: "tower", wall: "#bcb6a8", roof: "#653a2a", tier: 4 },
  CATAPULT: { archetype: "catapult", wall: "#9c6b3f", roof: "#6b4a2c" },

  HARBOR: { archetype: "harbor", wall: "#d4be94", roof: "#7d5a3a" },
  SHIPYARD: { archetype: "shipyard", wall: "#d2bd92", roof: "#7a5636" },
};

/** Per-terrain detail colours layered over the flat base fill for texture. */
export const TERRAIN_DETAIL: Record<TileType, { dark: string; light: string }> = {
  GRASS: { dark: "#5d9233", light: "#83bd52" },
  WATER: { dark: "#2a6fa3", light: "#67b4e0" },
  MOUNTAIN: { dark: "#736959", light: "#a59b8c" },
  FOREST: { dark: "#2f6620", light: "#4f8f32" },
  STONE: { dark: "#827c70", light: "#b3ad9f" },
  DESERT: { dark: "#cdb074", light: "#ecd8a6" },
  HILLS: { dark: "#86a64f", light: "#b3cf78" },
  FIELD: { dark: "#b8902a", light: "#e8c24a" },
};
