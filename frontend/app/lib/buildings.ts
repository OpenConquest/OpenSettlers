/**
 * Static metadata for every building type: display name, gameplay category and
 * a glyph used to draw it on the map and in the build palette. This is the
 * single source of truth the UI uses to group and label buildings.
 */
import type { BuildingName } from "~/types/game";
import { Castle, Warehouse, Axe, Trees, Pickaxe, Mountain, Droplets, Wheat, Fish, Crosshair, Fan, Croissant, PiggyBank, Beef, Hammer, Flame, Swords, Coins, Anvil, Beer, Carrot, Shield, TowerControl, ShieldHalf, Target, Anchor, Ship } from '@lucide/vue';
import type { FunctionalComponent, SVGAttributes } from 'vue';

/** High-level groups, mirroring the in-game build menu tabs. */
export type BuildingCategory =
  | "headquarters"
  | "extraction"
  | "food"
  | "industry"
  | "military"
  | "naval";

export interface BuildingMeta {
  name: BuildingName;
  label: string;
  category: BuildingCategory;
  icon: FunctionalComponent<SVGAttributes>;
  /** One-line description shown as a tooltip in the build palette. */
  description: string;
}

export const BUILDINGS: Record<BuildingName, BuildingMeta> = {
  HEADQUARTERS: { name: "HEADQUARTERS", label: "Headquarters", category: "headquarters", icon: Castle, description: "Starting hub and main storage." },
  WAREHOUSE: { name: "WAREHOUSE", label: "Warehouse", category: "headquarters", icon: Warehouse, description: "Extra storage for goods and settlers." },

  WOODCUTTER: { name: "WOODCUTTER", label: "Woodcutter", category: "extraction", icon: Axe, description: "Fells nearby trees for logs." },
  FORESTER: { name: "FORESTER", label: "Forester", category: "extraction", icon: Trees, description: "Plants new trees." },
  QUARRY: { name: "QUARRY", label: "Quarry", category: "extraction", icon: Pickaxe, description: "Cuts surface stone." },
  MINE: { name: "MINE", label: "Mine", category: "extraction", icon: Mountain, description: "Digs ore from the mountains; needs food." },
  WATER_WELL: { name: "WATER_WELL", label: "Water Well", category: "extraction", icon: Droplets, description: "Draws water." },

  FARM: { name: "FARM", label: "Farm", category: "food", icon: Wheat, description: "Grows wheat on nearby fields." },
  FISHING_HUT: { name: "FISHING_HUT", label: "Fishing Hut", category: "food", icon: Fish, description: "Catches fish from the water." },
  HUNTERS_HUT: { name: "HUNTERS_HUT", label: "Hunter's Hut", category: "food", icon: Crosshair, description: "Hunts wild game for meat." },
  MILL: { name: "MILL", label: "Mill", category: "food", icon: Fan, description: "Grinds wheat into flour." },
  BAKERY: { name: "BAKERY", label: "Bakery", category: "food", icon: Croissant, description: "Bakes bread from flour and water." },
  PIG_FARM: { name: "PIG_FARM", label: "Pig Farm", category: "food", icon: PiggyBank, description: "Breeds pigs from wheat and water." },
  SLAUGHTERHOUSE: { name: "SLAUGHTERHOUSE", label: "Slaughterhouse", category: "food", icon: Beef, description: "Turns pigs into meat." },

  SAWMILL: { name: "SAWMILL", label: "Sawmill", category: "industry", icon: Hammer, description: "Saws logs into planks." },
  FOUNDRY: { name: "FOUNDRY", label: "Foundry", category: "industry", icon: Flame, description: "Smelts iron and coal into steel." },
  ARMORY: { name: "ARMORY", label: "Armory", category: "industry", icon: Swords, description: "Forges swords and shields." },
  MINT: { name: "MINT", label: "Mint", category: "industry", icon: Coins, description: "Mints gold coins." },
  METALWORKS: { name: "METALWORKS", label: "Metalworks", category: "industry", icon: Anvil, description: "Crafts tools." },
  BREWERY: { name: "BREWERY", label: "Brewery", category: "industry", icon: Beer, description: "Brews beer for recruiting soldiers." },
  DONKEY_BREEDER: { name: "DONKEY_BREEDER", label: "Donkey Breeder", category: "industry", icon: Carrot, description: "Breeds donkeys to speed up roads." },

  BARRACKS: { name: "BARRACKS", label: "Barracks", category: "military", icon: Shield, description: "Small garrison; extends territory." },
  GUARD_HOUSE: { name: "GUARD_HOUSE", label: "Guard House", category: "military", icon: TowerControl, description: "Medium garrison; extends territory." },
  WATCH_TOWER: { name: "WATCH_TOWER", label: "Watch Tower", category: "military", icon: Castle, description: "Large garrison; wide territory." },
  CASTLE: { name: "CASTLE", label: "Castle", category: "military", icon: Castle, description: "Big garrison; broad territory." },
  FORTRESS: { name: "FORTRESS", label: "Fortress", category: "military", icon: ShieldHalf, description: "Largest garrison; widest territory." },
  CATAPULT: { name: "CATAPULT", label: "Catapult", category: "military", icon: Target, description: "Hurls stones at enemy buildings." },

  HARBOR: { name: "HARBOR", label: "Harbor", category: "naval", icon: Anchor, description: "Coastal store; launches expeditions." },
  SHIPYARD: { name: "SHIPYARD", label: "Shipyard", category: "naval", icon: Ship, description: "Builds ships for sea expeditions." },
};

/** Ordered list of categories with display labels for the build palette tabs. */
export const BUILDING_CATEGORIES: { id: BuildingCategory; label: string }[] = [
  { id: "extraction", label: "Extraction" },
  { id: "food", label: "Food" },
  { id: "industry", label: "Industry" },
  { id: "military", label: "Military" },
  { id: "naval", label: "Naval" },
  { id: "headquarters", label: "Storage" },
];

/** Buildings the player may freely place (the HQ is auto-placed by the engine). */
export function placeableBuildings(category: BuildingCategory): BuildingMeta[] {
  return Object.values(BUILDINGS).filter(
    (b) => b.category === category && b.name !== "HEADQUARTERS",
  );
}

/** Whether a building is military (drives the territory/attack affordances). */
export function isMilitary(name: BuildingName): boolean {
  return BUILDINGS[name].category === "military" && name !== "CATAPULT";
}
