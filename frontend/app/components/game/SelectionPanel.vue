<script setup lang="ts">
/**
 * Floating detail card for the currently inspected building — the in-game
 * building window of the original. Shows construction progress, productivity and
 * the produced good, garrison and coins, or stored inventory, and offers the
 * contextual controls: pause/resume production, toggle gold-coin delivery,
 * demolish an own building, or attack an enemy one.
 */
import { computed, ref } from "vue";
import { useDraggable } from '@vueuse/core';
import { BUILDINGS, isMilitary } from "~/lib/buildings";
import { RESOURCE_ICONS, playerColor, resourceLabel } from "~/lib/palette";
import type { BuildingName, ResourceType } from "~/types/game";

const { state, session, actions } = useGameSession();
const { selectedBuildingId } = useGameUi();

const building = computed(() =>
  state.value?.buildings.find((b) => b.id === selectedBuildingId.value) ?? null,
);

const meta = computed(() => (building.value?.name ? BUILDINGS[building.value.name] : null));
const isOwn = computed(() => building.value?.playerId === session.playerId);
const stored = computed(() =>
  Object.entries(building.value?.storedResources ?? {})
    .filter(([, qty]) => qty > 0)
    .sort((a, b) => b[1] - a[1]),
);

/** Primary good each production building turns out, for the output readout. */
const OUTPUT_OF: Partial<Record<BuildingName, ResourceType>> = {
  WOODCUTTER: "LOG", QUARRY: "STONE", FARM: "WHEAT", FISHING_HUT: "FISH",
  HUNTERS_HUT: "MEAT", WATER_WELL: "WATER", SAWMILL: "PLANK", MILL: "FLOUR",
  BAKERY: "BREAD", PIG_FARM: "PIG", SLAUGHTERHOUSE: "MEAT", DONKEY_BREEDER: "DONKEY",
  FOUNDRY: "STEEL", ARMORY: "SWORD", MINT: "COIN", METALWORKS: "TOOL", BREWERY: "BEER",
};
const output = computed(() => (building.value?.name ? OUTPUT_OF[building.value.name] ?? null : null));
const isHarbor = computed(() => building.value?.name === "HARBOR");

function close(): void {
  selectedBuildingId.value = null;
}

function demolish(): void {
  if (building.value) actions.destroy(building.value.id);
  close();
}

function attack(): void {
  if (building.value) actions.attack(building.value.id);
  close();
}

function togglePause(): void {
  if (building.value) actions.setProduction(building.value.id, building.value.productionPaused === true);
}

function toggleCoins(): void {
  if (building.value) actions.setCoinDelivery(building.value.id, building.value.coinsAllowed === false);
}

const el = ref<HTMLElement | null>(null);
const handle = ref<HTMLElement | null>(null);
const { style } = useDraggable(el, { initialValue: { x: 300, y: 400 }, handle });
</script>

<template>
  <div v-if="building && meta" ref="el" class="wood-panel fixed w-72 p-4" :style="style">
    <div ref="handle" class="mb-3 flex items-start justify-between border-b border-amber-900/30 pb-2 cursor-move">
      <div class="flex items-center gap-3">
        <div class="flex h-12 w-12 items-center justify-center rounded border border-amber-900/40 bg-amber-950/10 shadow-inner">
          <component :is="meta.icon" class="h-8 w-8 text-amber-900 drop-shadow-md" />
        </div>
        <div>
          <p class="cinzel-title text-lg font-bold leading-tight text-amber-950 drop-shadow-sm">{{ meta.label }}</p>
          <p class="flex items-center gap-1 text-xs font-bold text-amber-900">
            <span
              class="inline-block size-2 rounded-full border border-black/40 shadow-inner"
              :style="{ backgroundColor: playerColor(building.playerId) }"
            />
            Player {{ building.playerId + 1 }}
          </p>
        </div>
      </div>
      <button class="wood-btn flex h-6 w-6 items-center justify-center rounded-full text-xs" @click="close">✕</button>
    </div>

    <div v-if="isOwn" class="space-y-2 text-sm font-bold text-amber-950">
      <p v-if="building.underConstruction" class="text-amber-900/80">
        Under construction — groundwork {{ building.groundworkProgress ?? 0 }}%, walls
        {{ building.buildingProgress ?? 0 }}%
      </p>

      <template v-else>
        <template v-if="building.productivity != null">
          <p class="flex justify-between">
            <span class="text-amber-900">Productivity</span>
            <span :class="building.productionPaused ? 'text-red-800' : ''">
              {{ building.productionPaused ? "paused" : building.productivity + "%" }}
            </span>
          </p>
          <!-- Productivity bar. -->
          <div class="h-1.5 w-full overflow-hidden rounded-full bg-amber-950/20">
            <div
              class="h-full rounded-full transition-all"
              :class="building.productionPaused ? 'bg-red-700/70' : 'bg-amber-700'"
              :style="{ width: (building.productionPaused ? 0 : building.productivity) + '%' }"
            />
          </div>
          <p v-if="output" class="flex items-center justify-between">
            <span class="text-amber-900">Produces</span>
            <span class="flex items-center gap-1">
              <component :is="RESOURCE_ICONS[output]" class="h-4 w-4 text-amber-900" />
              {{ resourceLabel(output) }}
              <span class="text-amber-900/70">×{{ building.outputQuantity ?? 0 }}</span>
            </span>
          </p>
        </template>

        <p v-if="building.garrison != null" class="flex justify-between">
          <span class="text-amber-900">Garrison</span>
          <span>{{ building.garrison }} / {{ building.maxGarrison }}</span>
        </p>
        <p v-if="building.coins != null && isMilitary(building.name!)" class="flex items-center justify-between">
          <span class="text-amber-900">Gold coins</span>
          <span class="flex items-center gap-1">
            <component :is="RESOURCE_ICONS.COIN" class="h-4 w-4 text-amber-900" />{{ building.coins }}
          </span>
        </p>

        <p v-if="isHarbor" class="text-xs text-amber-900/80">
          Harbour — an expedition sails automatically once enough planks and stone are stored
          (needs a shipyard).
        </p>

        <div v-if="stored.length" class="mt-2 flex flex-wrap gap-2">
          <span
            v-for="[res, qty] in stored"
            :key="res"
            class="flex items-center gap-1 rounded border border-amber-900/30 bg-amber-950/10 px-2 py-1 text-sm shadow-sm"
            :title="resourceLabel(res as ResourceType)"
          >
            <component :is="RESOURCE_ICONS[res as keyof typeof RESOURCE_ICONS]" class="inline-block h-4 w-4 text-amber-900" />
            <span>{{ qty }}</span>
          </span>
        </div>
      </template>
    </div>

    <div v-if="!building.underConstruction" class="mt-4 flex flex-wrap gap-2">
      <!-- Pause / resume production (own production buildings). -->
      <button
        v-if="isOwn && building.productivity != null"
        class="wood-btn flex-1 py-1.5 text-sm"
        @click="togglePause"
      >
        {{ building.productionPaused ? "Resume" : "Pause" }}
      </button>
      <!-- Gold-coin delivery toggle (own military buildings). -->
      <button
        v-if="isOwn && building.coinsAllowed != null"
        class="wood-btn flex-1 py-1.5 text-sm"
        :class="{ active: building.coinsAllowed }"
        @click="toggleCoins"
      >
        Coins: {{ building.coinsAllowed ? "On" : "Off" }}
      </button>
      <button
        v-if="isOwn && building.name !== 'HEADQUARTERS'"
        class="wood-btn flex-1 py-1.5 text-sm"
        @click="demolish"
      >
        Demolish
      </button>
      <button v-else-if="!isOwn" class="wood-btn flex-1 py-1.5 text-sm" @click="attack">
        Attack
      </button>
    </div>
  </div>
</template>
