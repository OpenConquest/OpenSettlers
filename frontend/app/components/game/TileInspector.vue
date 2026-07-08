<script setup lang="ts">
/**
 * Tile context panel: opens when the player clicks a tile (inspect tool). It
 * describes the tile — terrain, elevation, owner and any live natural resource —
 * and, when the tile is buildable, shows the construction menu directly (a
 * category tab strip and a grid of buildings) plus a flag placement.
 */
import { computed, ref, watch } from "vue";
import {
  BUILDING_CATEGORIES,
  BUILDINGS,
  placeableBuildings,
  type BuildingCategory,
} from "~/lib/buildings";
import { RESOURCE_ICONS, RESOURCE_NODE_NOUN, playerColor } from "~/lib/palette";
import { hexKey } from "~/lib/hex";
import type { BuildingName } from "~/types/game";

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

const tile = computed(() => {
  const c = selectedTile.value;
  return c ? map.value?.tiles.get(hexKey(c.x, c.y)) : undefined;
});

const terrain = computed(() =>
  tile.value ? (TERRAIN[tile.value.tileType] ?? { label: tile.value.tileType, glyph: "▦" }) : null,
);

const resource = computed(() => {
  const c = selectedTile.value;
  if (!c) return null;
  return state.value?.resources.find((r) => r.x === c.x && r.y === c.y) ?? null;
});

const resourceNoun = computed(() =>
  resource.value ? (RESOURCE_NODE_NOUN[resource.value.resource] ?? resource.value.resource) : null,
);

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
 * Whether this tile can host a building (buildable terrain, unoccupied).
 * Territory ownership is not required here so the menu is always reachable; the
 * server still validates placement and refuses tiles outside the player's land.
 */
const canBuild = computed(() => {
  if (!tile.value || buildingHere.value) return false;
  const t = tile.value.tileType;
  return (t === "GRASS" || t === "HILLS" || t === "MOUNTAIN") && tile.value.elevation <= 4;
});

const ownedByMe = computed(() => owner.value === session.playerId);

const openCategory = ref<BuildingCategory>("extraction");
/** Keep the build grid scrolled to the chosen category whenever a tile changes. */
watch(selectedTile, () => (openCategory.value = "extraction"));

function close(): void {
  selectedTile.value = null;
}

function build(buildingName: BuildingName): void {
  if (selectedTile.value) {
    actions.build(buildingName, selectedTile.value);
    log(`Ordered ${BUILDINGS[buildingName]?.label ?? "building"}.`, "good");
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
    class="wood-panel absolute bottom-28 left-6 w-80 p-3.5"
  >
    <!-- Header: terrain + coordinates -->
    <div class="mb-2.5 flex items-center justify-between border-b border-amber-900/30 pb-2">
      <div class="flex items-center gap-2.5">
        <span class="text-2xl drop-shadow">{{ terrain.glyph }}</span>
        <div class="leading-tight">
          <p class="cinzel-title text-base font-bold text-amber-950">{{ terrain.label }}</p>
          <p class="text-[11px] font-bold text-amber-900/80 tabular-nums">
            {{ selectedTile.x }}, {{ selectedTile.y }} · elev {{ tile.elevation }}
          </p>
        </div>
      </div>
      <button
        class="flex h-6 w-6 items-center justify-center rounded-full border border-amber-900/40 bg-amber-950/10 text-xs font-bold text-amber-900 transition hover:bg-amber-900/20"
        @click="close"
      >✕</button>
    </div>

    <!-- Resource / ownership readout -->
    <div class="mb-2.5 flex items-center gap-2 text-sm font-bold text-amber-950">
      <span
        v-if="resource"
        class="flex flex-1 items-center justify-between rounded border border-amber-900/20 bg-amber-950/10 px-2 py-1 shadow-inner"
      >
        <span class="flex items-center gap-1.5 text-amber-900">
          <component :is="RESOURCE_ICONS[resource.resource]" class="h-4 w-4" />
          {{ resourceNoun }}
        </span>
        <span class="tabular-nums">{{ resource.quantity }}</span>
      </span>
      <span v-else class="flex-1 text-[12px] italic text-amber-900/60">No resource here.</span>

      <span class="flex items-center gap-1.5 whitespace-nowrap">
        <span
          v-if="owner != null && owner >= 0"
          class="inline-block size-2.5 rounded-full border border-black/40"
          :style="{ backgroundColor: playerColor(owner) }"
        />
        <span class="text-[12px] text-amber-900">
          {{ owner == null || owner < 0 ? "Unclaimed" : owner === session.playerId ? "Yours" : `Player ${owner + 1}` }}
        </span>
      </span>
    </div>

    <!-- Construction menu (shown directly for a buildable tile) -->
    <template v-if="canBuild">
      <div class="mb-2 flex items-center justify-between">
        <p class="cinzel-title text-sm font-bold text-amber-900">Build here</p>
        <button
          class="rounded border border-amber-900/40 bg-amber-100/40 px-2.5 py-1 text-xs font-bold text-amber-950 transition hover:bg-amber-100/70"
          @click="placeFlag"
        >⚑ Flag</button>
      </div>

      <p v-if="!ownedByMe" class="mb-2 text-[11px] italic text-amber-900/70">
        Outside your land — placement may be refused. Expand with a military building.
      </p>

      <!-- Category tabs -->
      <div class="mb-2 flex flex-wrap gap-1">
        <button
          v-for="cat in BUILDING_CATEGORIES"
          :key="cat.id"
          class="rounded border px-2 py-1 text-[10px] font-bold uppercase tracking-wide transition"
          :class="openCategory === cat.id
            ? 'border-amber-700 bg-amber-700 text-amber-50 shadow'
            : 'border-amber-900/30 bg-amber-100/30 text-amber-900 hover:bg-amber-100/60'"
          @click="openCategory = cat.id"
        >{{ cat.label }}</button>
      </div>

      <!-- Building grid -->
      <div class="grid grid-cols-5 gap-1.5">
        <button
          v-for="b in placeableBuildings(openCategory)"
          :key="b.name"
          :title="`${b.label} — ${b.description}`"
          class="flex aspect-square items-center justify-center rounded border border-amber-900/30 bg-amber-100/40 p-1 text-amber-900 shadow-sm transition hover:scale-110 hover:border-amber-700 hover:bg-amber-100/80"
          @click="build(b.name)"
        >
          <component :is="b.icon" class="h-6 w-6 drop-shadow" />
        </button>
      </div>
    </template>

    <p v-else-if="buildingHere" class="text-[12px] italic text-amber-900/70">
      A building already stands here.
    </p>
    <p v-else class="text-[12px] italic text-amber-900/70">
      This terrain cannot be built on.
    </p>
  </div>
</template>
