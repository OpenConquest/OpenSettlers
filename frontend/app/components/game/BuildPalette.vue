<script setup lang="ts">
/**
 * Full-width bottom control bar (Settlers II style): a status line, the row of
 * round brass tool buttons (inspect / flag / road / geologist / destroy /
 * attack), and a keyboard reminder. Selecting a tool arms it in
 * {@link useGameUi}; the canvas then applies it on the next click. Buildings are
 * placed from the {@link TileInspector} after clicking a tile.
 */
import { computed } from "vue";
import { MousePointer2, Flag, Route, Telescope, Binoculars, Bomb, Swords, Minus, Plus } from "@lucide/vue";
import type { ToolKind } from "~/composables/useGameUi";

const { tool, roadDraft, attackerCount, setTool } = useGameUi();

/** Adjusts the attack party size, clamped at 0 (= send all available). */
function adjustAttackers(delta: number): void {
  attackerCount.value = Math.max(0, attackerCount.value + delta);
}

/** Context-sensitive one-liner shown in the help scroll for the active tool. */
const hint = computed(() => {
  switch (tool.value.kind) {
    case "road":
      return roadDraft.value
        ? "Click the target flag — the shortest buildable road is previewed (green = OK, red = blocked)."
        : "Click a flag to start a road, then click the flag you want to connect it to.";
    case "build":
      return "Click a tile to place the building. Esc cancels.";
    case "flag":
      return "Click a tile to plant a flag. Esc cancels.";
    case "geologist":
      return "Click one of your flags to send a geologist prospecting nearby.";
    case "scout":
      return "Click one of your flags to send a scout exploring the fog of war.";
    case "destroy":
      return "Click one of your buildings to demolish it.";
    case "attack":
      return attackerCount.value > 0
        ? `Click an enemy building to attack with up to ${attackerCount.value} soldiers.`
        : "Click an enemy building to attack with every available soldier.";
    default:
      return "Click a tile to inspect it and build. Drag to pan, scroll to zoom, 1–7 picks a tool.";
  }
});

const GENERAL_TOOLS: { kind: ToolKind; icon: any; label: string; key: number }[] = [
  { kind: "inspect", icon: MousePointer2, label: "Inspect / Pan", key: 1 },
  { kind: "flag", icon: Flag, label: "Place flag", key: 2 },
  { kind: "road", icon: Route, label: "Build road", key: 3 },
  { kind: "geologist", icon: Telescope, label: "Send geologist", key: 4 },
  { kind: "scout", icon: Binoculars, label: "Send scout", key: 5 },
  { kind: "destroy", icon: Bomb, label: "Demolish", key: 6 },
  { kind: "attack", icon: Swords, label: "Attack", key: 7 },
];

function isToolActive(kind: ToolKind): boolean {
  return tool.value.kind === kind;
}
</script>

<template>
  <!-- Full-width Settlers II control bar: status · tools · help. -->
  <div class="dark-wood pointer-events-auto flex w-full items-center gap-4 border-t-4 border-[#b8860b]/50 px-12 py-2">
    <!-- Left: the contextual hint, like the game's status line. -->
    <p class="hidden min-w-0 flex-1 truncate text-[12px] font-bold text-[#f4e8c1] md:block">
      {{ hint }}
    </p>

    <!-- Centre: round brass tool buttons. -->
    <div class="flex shrink-0 items-center gap-2.5">
      <button
        v-for="t in GENERAL_TOOLS"
        :key="t.kind"
        :title="`${t.label} (${t.key})`"
        class="brass-btn h-12 w-12"
        :class="{ active: isToolActive(t.kind) }"
        @click="setTool({ kind: t.kind })"
      >
        <component :is="t.icon" class="h-6 w-6" />
        <span class="absolute -bottom-1 -right-1 rounded-full bg-[#2a1608] px-1 text-[9px] font-bold text-amber-200 shadow">{{ t.key }}</span>
      </button>
    </div>

    <!-- Right: attack party stepper (only while attacking) or keyboard reminder. -->
    <div v-if="tool.kind === 'attack'" class="flex flex-1 items-center justify-end gap-2">
      <span class="text-[11px] font-bold text-[#f4e8c1]/80">Attackers</span>
      <button class="brass-btn h-8 w-8" title="Fewer attackers" @click="adjustAttackers(-1)">
        <Minus class="h-4 w-4" />
      </button>
      <span class="min-w-10 text-center text-[13px] font-bold text-amber-200">
        {{ attackerCount > 0 ? attackerCount : "All" }}
      </span>
      <button class="brass-btn h-8 w-8" title="More attackers" @click="adjustAttackers(1)">
        <Plus class="h-4 w-4" />
      </button>
    </div>
    <p v-else class="hidden flex-1 text-right text-[11px] font-bold text-[#f4e8c1]/70 lg:block">
      1–7 tools · Esc cancels · drag to pan
    </p>
  </div>
</template>
