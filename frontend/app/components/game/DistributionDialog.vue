<script setup lang="ts">
/**
 * The distribution window of the original game: for each scarce good with
 * several consumers, the player ranks which buildings get served first. Reading
 * the authoritative order from the server state and sending {@link setDistribution}
 * on every reorder keeps the dialog in sync with the engine.
 */
import { computed } from "vue";
import { BUILDINGS } from "~/lib/buildings";
import { RESOURCE_ICONS, resourceLabel } from "~/lib/palette";
import type { BuildingName, ResourceType } from "~/types/game";

const { state, session, actions } = useGameSession();
const { openPanel, togglePanel } = useGameUi();

/** Fixed display order of the contested goods. */
const GOODS: ResourceType[] = ["COAL", "IRON", "WHEAT", "WATER"];

const visible = computed(() => openPanel.value === "distribution" && session.playerId != null);

/** The viewer's current priority table from the live state. */
const distribution = computed(() => state.value?.distribution ?? {});

const goods = computed(() => GOODS.filter((g) => (distribution.value[g]?.length ?? 0) > 0));

/** Moves a consumer up (`-1`) or down (`+1`) and pushes the new order to the server. */
function move(good: ResourceType, index: number, dir: -1 | 1): void {
  const order = [...(distribution.value[good] ?? [])];
  const j = index + dir;
  if (j < 0 || j >= order.length) return;
  [order[index], order[j]] = [order[j]!, order[index]!] as [BuildingName, BuildingName];
  actions.setDistribution(good, order);
}
</script>

<template>
  <div
    v-if="visible"
    class="pointer-events-auto fixed inset-0 z-[60] flex items-center justify-center bg-black/50"
    @click.self="togglePanel('distribution')"
  >
    <div class="wood-panel max-h-[80vh] w-[34rem] overflow-y-auto p-5">
      <div class="mb-4 flex items-center justify-between border-b border-amber-900/30 pb-2">
        <h2 class="cinzel-title text-xl font-bold text-amber-950">Distribution</h2>
        <button class="wood-btn flex h-6 w-6 items-center justify-center rounded-full text-xs" @click="togglePanel('distribution')">✕</button>
      </div>

      <p class="mb-4 text-xs font-bold text-amber-900/80">
        When a good is scarce, buildings higher in the list are supplied first.
      </p>

      <div class="space-y-4">
        <div v-for="good in goods" :key="good" class="rounded border border-amber-900/30 bg-amber-950/5 p-3">
          <div class="mb-2 flex items-center gap-2">
            <component :is="RESOURCE_ICONS[good]" class="h-5 w-5 text-amber-900" />
            <span class="cinzel-title font-bold text-amber-950">{{ resourceLabel(good) }}</span>
          </div>
          <ul class="space-y-1.5">
            <li
              v-for="(name, i) in distribution[good]"
              :key="name"
              class="flex items-center gap-2 rounded border border-amber-900/20 bg-amber-100/30 px-2 py-1"
            >
              <span class="w-4 text-center text-xs font-bold text-amber-900/70">{{ i + 1 }}</span>
              <component :is="BUILDINGS[name].icon" class="h-4 w-4 text-amber-900" />
              <span class="flex-1 text-sm font-bold text-amber-950">{{ BUILDINGS[name].label }}</span>
              <button
                class="wood-btn flex h-5 w-5 items-center justify-center rounded text-[10px]"
                :disabled="i === 0"
                :class="{ 'opacity-30': i === 0 }"
                @click="move(good, i, -1)"
              >▲</button>
              <button
                class="wood-btn flex h-5 w-5 items-center justify-center rounded text-[10px]"
                :disabled="i === (distribution[good]?.length ?? 0) - 1"
                :class="{ 'opacity-30': i === (distribution[good]?.length ?? 0) - 1 }"
                @click="move(good, i, 1)"
              >▼</button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>
