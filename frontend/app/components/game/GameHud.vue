<script setup lang="ts">
/**
 * Top status bar: connection state, current tick, the local player's headquarters
 * stock summary, and the global game actions (save, leave). Stock is read from
 * the player's storage buildings in the live state.
 */
import { computed } from "vue";
import { RESOURCE_ICONS, playerColor } from "~/lib/palette";
import type { ResourceType } from "~/types/game";

const emit = defineEmits<{ (e: "save"): void; (e: "leave"): void }>();

const { status, tick, state, session } = useGameSession();
const { togglePanel, toggleMinimap } = useGameUi();

/** Key resources to surface in the top bar (a curated subset). */
const TRACKED: ResourceType[] = ["PLANK", "STONE", "LOG", "COAL", "IRON", "GOLD", "BREAD", "COIN"];

/** Aggregated stock across the local player's storage buildings. */
const stock = computed<Record<string, number>>(() => {
  const totals: Record<string, number> = {};
  for (const b of state.value?.buildings ?? []) {
    if (b.playerId !== session.playerId || !b.storedResources) continue;
    for (const [res, qty] of Object.entries(b.storedResources)) {
      totals[res] = (totals[res] ?? 0) + qty;
    }
  }
  return totals;
});

const statusLabel = computed(() => {
  switch (status.value) {
    case "open":
      return "Connected";
    case "connecting":
      return "Connecting…";
    case "error":
      return "Connection error";
    case "closed":
      return "Disconnected";
    default:
      return "Idle";
  }
});

const statusVariant = computed(() =>
  status.value === "open" ? "default" : status.value === "error" ? "destructive" : "secondary",
);
</script>

<template>
  <header
    class="flex h-12 w-full items-center justify-between gap-6 px-10 text-[13px] font-bold bg-black/40 backdrop-blur-md border-b-2 border-[#b8860b]/70 shadow-md"
  >
    <div class="flex items-center gap-2">
      <span class="cinzel-title text-base text-[#f4e8c1] drop-shadow-sm">Open Settlers</span>
      <span
        class="rounded border border-black/40 bg-black/30 px-2 py-0.5 text-[11px] text-[#f4e8c1]/80"
        :class="{ 'text-red-400': status === 'error' }"
      >
        {{ statusLabel }}
      </span>
    </div>

    <div v-if="session.playerId != null" class="flex items-center gap-2 text-[13px] drop-shadow-sm text-[#f4e8c1]">
      <span
        class="inline-block size-3 rounded-full border border-black/50 shadow-inner"
        :style="{ backgroundColor: playerColor(session.playerId) }"
      />
      <span>Player {{ session.playerId + 1 }}</span>
    </div>
    <span v-else class="text-[#f4e8c1]/70">Spectator</span>

    <div class="h-6 w-px bg-black/40 shadow-[1px_0_0_rgba(255,255,255,0.1)]" />

    <!-- Resources removed per user request -->
    <div class="flex-1"></div>

    <div class="h-6 w-px bg-black/40 shadow-[1px_0_0_rgba(255,255,255,0.1)]" />

    <span class="text-[11px] text-[#f4e8c1]/70 tabular-nums">tick {{ tick }}</span>

    <div class="flex items-center gap-2">
      <button
        v-if="session.playerId != null"
        class="brass-btn h-8 px-3 text-[11px] shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
        title="Inventory & empire overview"
        @click="togglePanel('inventory')"
      >Goods</button>
      <button
        v-if="session.playerId != null"
        class="brass-btn h-8 px-3 text-[11px] shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
        title="Set distribution priorities"
        @click="togglePanel('distribution')"
      >Distribution</button>
      <button
        v-if="session.playerId != null"
        class="brass-btn h-8 px-3 text-[11px] shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
        title="Set military strength"
        @click="togglePanel('military')"
      >Military</button>
      <button class="brass-btn h-8 px-4 text-[11px] shadow-[0_2px_4px_rgba(0,0,0,0.5)]" @click="emit('save')">Save</button>
      <button class="brass-btn h-8 px-4 text-[11px] shadow-[0_2px_4px_rgba(0,0,0,0.5)]" @click="emit('leave')">Leave</button>
      <div class="h-6 w-px bg-black/40 mx-2 shadow-[1px_0_0_rgba(255,255,255,0.1)]" />
      <button class="brass-btn h-8 px-4 text-[11px] font-extrabold text-amber-900 shadow-[0_2px_4px_rgba(0,0,0,0.5)]" @click="toggleMinimap()">
        MAP
      </button>
    </div>
  </header>
</template>
