<script setup lang="ts">
/**
 * The player's inventory & empire overview — the goods screen of the original.
 * Aggregates every good held across the player's storage buildings and a short
 * tally of buildings and military strength. Read-only, computed from live state.
 */
import { computed } from "vue";
import { RESOURCE_ICONS, resourceLabel } from "~/lib/palette";
import type { ResourceType } from "~/types/game";

const { state, session } = useGameSession();
const { openPanel, togglePanel } = useGameUi();

const visible = computed(() => openPanel.value === "inventory" && session.playerId != null);

/** Every good, in production-chain-ish reading order. */
const ALL_GOODS: ResourceType[] = [
  "LOG", "PLANK", "STONE", "IRON", "COAL", "GOLD", "STEEL", "COIN", "TOOL", "SWORD", "SHIELD",
  "WHEAT", "FLOUR", "BREAD", "FISH", "MEAT", "PIG", "WATER", "BEER", "DONKEY",
];

/** Own buildings (excluding construction sites). */
const ownBuildings = computed(() =>
  (state.value?.buildings ?? []).filter((b) => b.playerId === session.playerId && !b.underConstruction),
);

/** Aggregated stock across the player's storage buildings. */
const stock = computed<Record<string, number>>(() => {
  const totals: Record<string, number> = {};
  for (const b of ownBuildings.value) {
    if (!b.storedResources) continue;
    for (const [res, qty] of Object.entries(b.storedResources)) totals[res] = (totals[res] ?? 0) + qty;
  }
  return totals;
});

const buildingCount = computed(() => ownBuildings.value.length);
const militaryCount = computed(() => ownBuildings.value.filter((b) => b.garrison != null).length);
/** Garrisoned soldiers plus this player's soldiers in the field. */
const soldiers = computed(() => {
  let n = 0;
  for (const b of ownBuildings.value) n += b.garrison ?? 0;
  for (const s of state.value?.soldiers ?? []) if (s.playerId === session.playerId) n += 1;
  return n;
});
</script>

<template>
  <div
    v-if="visible"
    class="pointer-events-auto fixed inset-0 z-[60] flex items-center justify-center bg-black/50"
    @click.self="togglePanel('inventory')"
  >
    <div class="wood-panel max-h-[80vh] w-[32rem] overflow-y-auto p-5">
      <div class="mb-4 flex items-center justify-between border-b border-amber-900/30 pb-2">
        <h2 class="cinzel-title text-xl font-bold text-amber-950">Inventory</h2>
        <button class="wood-btn flex h-6 w-6 items-center justify-center rounded-full text-xs" @click="togglePanel('inventory')">✕</button>
      </div>

      <div class="grid grid-cols-5 gap-2">
        <div
          v-for="good in ALL_GOODS"
          :key="good"
          class="flex flex-col items-center gap-0.5 rounded border border-amber-900/20 bg-amber-100/20 py-2"
          :class="{ 'opacity-40': !stock[good] }"
          :title="resourceLabel(good)"
        >
          <component :is="RESOURCE_ICONS[good]" class="h-5 w-5 text-amber-900" />
          <span class="text-sm font-bold tabular-nums text-amber-950">{{ stock[good] ?? 0 }}</span>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-3 gap-2 border-t border-amber-900/30 pt-3 text-center">
        <div>
          <p class="text-2xl font-bold text-amber-950 tabular-nums">{{ buildingCount }}</p>
          <p class="text-[11px] font-bold uppercase tracking-wide text-amber-900/70">Buildings</p>
        </div>
        <div>
          <p class="text-2xl font-bold text-amber-950 tabular-nums">{{ militaryCount }}</p>
          <p class="text-[11px] font-bold uppercase tracking-wide text-amber-900/70">Military</p>
        </div>
        <div>
          <p class="text-2xl font-bold text-amber-950 tabular-nums">{{ soldiers }}</p>
          <p class="text-[11px] font-bold uppercase tracking-wide text-amber-900/70">Soldiers</p>
        </div>
      </div>
    </div>
  </div>
</template>
