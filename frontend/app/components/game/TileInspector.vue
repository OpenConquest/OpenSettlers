<script setup lang="ts">
/**
 * Tile inspector: a Settlers-style panel describing the tile the player clicked
 * — its terrain, elevation, owner and any live natural resource (trees, stone,
 * ore, fish, water) with the quantity still remaining. When the tile can be
 * built on it also offers the build menu and a flag placement.
 */
import { computed, ref } from "vue";
import {
  BUILDING_CATEGORIES,
  placeableBuildings,
  type BuildingCategory,
} from "~/lib/buildings";
import { RESOURCE_ICONS, RESOURCE_NODE_NOUN, playerColor } from "~/lib/palette";
import { hexKey } from "~/lib/hex";
import { BUILDINGS } from "~/lib/buildings";

const { selectedTile, tool } = useGameUi();
const { map, state, session, actions } = useGameSession();
const { log } = useGameLog();

/** Short label + glyph for each terrain type. */
const TERRAIN: Record<string, { label: string; glyph: string }> = {
  GRASS: { label: "Grassland", glyph: "🌱" },
  WATER: { label: "Water", glyph: "🌊" },
  MOUNTAIN: { label: "Mountain", glyph: "⛰️" },
  FOREST: { label: "Forest", glyph: "🌲" },
  STONE: { label: "Stony ground", glyph: "🪨" },
  DESERT: { label: "Desert", glyph: "🏜️" },
  HILLS: { label: "Hills", glyph: "🌿" },
  FIELD: { label: "Wheat field", glyph: "🌾" },
};

/** The static tile under the selection (terrain, elevation). */
const tile = computed(() => {
  const c = selectedTile.value;
  return c ? map.value?.tiles.get(hexKey(c.x, c.y)) : undefined;
});

const terrain = computed(() =>
  tile.value ? (TERRAIN[tile.value.tileType] ?? { label: tile.value.tileType, glyph: "▦" }) : null,
);

/** Live resource node on the tile, if any, with its remaining quantity. */
const resource = computed(() => {
  const c = selectedTile.value;
  if (!c) return null;
  return state.value?.resources.find((r) => r.x === c.x && r.y === c.y) ?? null;
});

const resourceNoun = computed(() =>
  resource.value ? (RESOURCE_NODE_NOUN[resource.value.resource] ?? resource.value.resource) : null,
);

/** Territory owner of the tile (-1 / null when unclaimed). */
const owner = computed(() => {
  const c = selectedTile.value;
  if (!c) return null;
  const t = state.value?.territory.find(([x, y]) => x === c.x && y === c.y);
  return t ? t[2] : null;
});

const buildingHere = computed(() => {
  const c = selectedTile.value;
  return c ? state.value?.buildings.find((b) => b.x === c.x && b.y === c.y) : undefined;
});

/**
 * Whether this tile can host a building at all (buildable terrain, unoccupied).
 * Territory ownership is *not* required here so the build menu is always
 * reachable; the server still validates placement and refuses tiles outside the
 * player's land.
 */
const canBuild = computed(
  () => !!tile.value && tile.value.tileType === "GRASS" && !buildingHere.value,
);

/** Whether the tile sits inside the local player's own territory. */
const ownedByMe = computed(() => owner.value === session.playerId);

const openCategory = ref<BuildingCategory>("extraction");
const showBuildMenu = ref(false);

function close(): void {
  selectedTile.value = null;
  showBuildMenu.value = false;
}

function build(buildingName: string): void {
  if (selectedTile.value) {
    actions.build(buildingName as any, selectedTile.value);
    log(`Ordered ${BUILDINGS[buildingName as keyof typeof BUILDINGS]?.label ?? "building"}.`, "good");
  }
  close();
}

function placeFlag(): void {
  if (selectedTile.value) {
    actions.placeFlag(selectedTile.value);
    log("Flag planted.", "good");
  }
  close();
}
</script>

<template>
  <div
    v-if="selectedTile && tile && terrain && tool.kind === 'inspect'"
    class="wood-panel absolute bottom-28 left-6 w-72 p-3"
  >
    <!-- Header: terrain + coordinates -->
    <div class="mb-2 flex items-center justify-between border-b border-amber-900/30 pb-2">
      <div class="flex items-center gap-2">
        <span class="text-2xl drop-shadow">{{ terrain.glyph }}</span>
        <div class="leading-tight">
          <p class="cinzel-title text-base font-bold text-amber-950">{{ terrain.label }}</p>
          <p class="text-[11px] font-bold text-amber-900/80 tabular-nums">
            {{ selectedTile.x }}, {{ selectedTile.y }} · elev {{ tile.elevation }}
          </p>
        </div>
      </div>
      <button class="wood-btn flex h-6 w-6 items-center justify-center rounded-full text-xs" @click="close">✕</button>
    </div>

    <!-- Resource / ownership readout -->
    <div class="space-y-1.5 text-sm font-bold text-amber-950">
      <div v-if="resource" class="flex items-center justify-between rounded bg-amber-950/10 px-2 py-1.5 shadow-inner">
        <span class="flex items-center gap-1.5 text-amber-900">
          <component :is="RESOURCE_ICONS[resource.resource]" class="h-4 w-4" />
          {{ resourceNoun }}
        </span>
        <span class="tabular-nums text-base">{{ resource.quantity }}</span>
      </div>
      <p v-else class="text-[12px] italic text-amber-900/70">No harvestable resource here.</p>

      <p class="flex items-center justify-between">
        <span class="text-amber-900">Owner</span>
        <span v-if="owner != null && owner >= 0" class="flex items-center gap-1.5">
          <span
            class="inline-block size-2.5 rounded-full border border-black/40 shadow-inner"
            :style="{ backgroundColor: playerColor(owner) }"
          />
          {{ owner === session.playerId ? "You" : `Player ${owner + 1}` }}
        </span>
        <span v-else class="text-amber-900/70">Unclaimed</span>
      </p>
    </div>

    <!-- Build affordances -->
    <div v-if="canBuild" class="mt-3">
      <p v-if="!ownedByMe" class="mb-2 text-[11px] italic text-amber-900/80">
        Outside your territory — placement may be refused. Expand with a military building.
      </p>
      <div class="flex gap-2">
        <button class="wood-btn flex-1 py-1.5 text-sm" @click="showBuildMenu = !showBuildMenu">
          {{ showBuildMenu ? "Hide buildings" : "Build…" }}
        </button>
        <button class="wood-btn flex-1 py-1.5 text-sm" @click="placeFlag">
          Place Flag
        </button>
      </div>

      <div v-if="showBuildMenu" class="mt-3">
        <div class="mb-2 flex flex-wrap gap-1 border-b border-amber-900/20 pb-2">
          <button
            v-for="cat in BUILDING_CATEGORIES"
            :key="cat.id"
            class="wood-btn px-2 py-1 text-[10px] font-bold"
            :class="{ active: openCategory === cat.id }"
            @click="openCategory = cat.id"
          >
            {{ cat.label }}
          </button>
        </div>
        <div class="grid grid-cols-5 gap-1.5">
          <button
            v-for="b in placeableBuildings(openCategory)"
            :key="b.name"
            :title="`${b.label} — ${b.description}`"
            class="wood-btn flex aspect-square items-center justify-center rounded p-1 transition-transform hover:scale-110"
            @click="build(b.name)"
          >
            <component :is="b.icon" class="h-6 w-6 drop-shadow" />
          </button>
        </div>
      </div>
    </div>
    <p v-else-if="buildingHere" class="mt-2 text-[11px] italic text-amber-900/70">
      A building already stands here.
    </p>
    <p v-else class="mt-2 text-[11px] italic text-amber-900/70">
      This terrain cannot be built on.
    </p>
  </div>
</template>
