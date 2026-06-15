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
    class="flex items-center gap-4 border-b border-border bg-card/95 px-4 py-2 backdrop-blur"
  >
    <div class="flex items-center gap-2">
      <span class="text-lg font-semibold tracking-tight">Open Settlers</span>
      <Badge :variant="statusVariant">{{ statusLabel }}</Badge>
    </div>

    <div v-if="session.playerId != null" class="flex items-center gap-1.5 text-sm">
      <span
        class="inline-block size-3 rounded-full"
        :style="{ backgroundColor: playerColor(session.playerId) }"
      />
      <span class="text-muted-foreground">Player {{ session.playerId + 1 }}</span>
    </div>
    <Badge v-else variant="outline">Spectator</Badge>

    <Separator orientation="vertical" class="h-6" />

    <ul class="flex flex-1 items-center gap-3 overflow-x-auto text-sm">
      <li v-for="res in TRACKED" :key="res" class="flex items-center gap-1 tabular-nums">
        <span :title="res">{{ RESOURCE_ICONS[res] }}</span>
        <span class="font-medium">{{ stock[res] ?? 0 }}</span>
      </li>
    </ul>

    <span class="text-xs text-muted-foreground tabular-nums">tick {{ tick }}</span>

    <div class="flex items-center gap-2">
      <Button variant="outline" size="sm" @click="emit('save')">Save</Button>
      <Button variant="ghost" size="sm" @click="emit('leave')">Leave</Button>
    </div>
  </header>
</template>
