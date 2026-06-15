<script setup lang="ts">
/**
 * Left-hand command palette: the general tools (inspect / flag / road /
 * geologist / destroy / attack) on top, then the catalogue of placeable
 * buildings grouped by category. Selecting an entry arms the matching tool in
 * {@link useGameUi}; the canvas then applies it on the next click.
 */
import { ref } from "vue";
import {
  BUILDING_CATEGORIES,
  placeableBuildings,
  type BuildingCategory,
} from "~/lib/buildings";
import type { ToolKind } from "~/composables/useGameUi";
import type { BuildingName } from "~/types/game";

const { tool, setTool } = useGameUi();

const GENERAL_TOOLS: { kind: ToolKind; icon: string; label: string }[] = [
  { kind: "inspect", icon: "🖱️", label: "Inspect / Pan" },
  { kind: "flag", icon: "🚩", label: "Place flag" },
  { kind: "road", icon: "🛣️", label: "Build road" },
  { kind: "geologist", icon: "🔍", label: "Send geologist" },
  { kind: "destroy", icon: "💥", label: "Demolish" },
  { kind: "attack", icon: "⚔️", label: "Attack" },
];

const openCategory = ref<BuildingCategory | null>("extraction");

function isToolActive(kind: ToolKind): boolean {
  return tool.value.kind === kind && kind !== "build";
}

function isBuildingActive(name: BuildingName): boolean {
  return tool.value.kind === "build" && tool.value.building === name;
}

function toggleCategory(id: BuildingCategory): void {
  openCategory.value = openCategory.value === id ? null : id;
}
</script>

<template>
  <aside
    class="flex w-60 shrink-0 flex-col gap-3 overflow-y-auto border-r border-border bg-card/95 p-3 backdrop-blur"
  >
    <div>
      <h2 class="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        Tools
      </h2>
      <div class="grid grid-cols-3 gap-1.5">
        <button
          v-for="t in GENERAL_TOOLS"
          :key="t.kind"
          :title="t.label"
          class="flex aspect-square flex-col items-center justify-center rounded-md border text-lg transition-colors"
          :class="
            isToolActive(t.kind)
              ? 'border-primary bg-primary/15 text-primary'
              : 'border-border bg-background hover:bg-accent'
          "
          @click="setTool({ kind: t.kind })"
        >
          <span>{{ t.icon }}</span>
        </button>
      </div>
    </div>

    <div class="space-y-1.5">
      <h2 class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        Buildings
      </h2>
      <div v-for="cat in BUILDING_CATEGORIES" :key="cat.id" class="rounded-md border border-border">
        <button
          class="flex w-full items-center justify-between px-2.5 py-1.5 text-sm font-medium hover:bg-accent"
          @click="toggleCategory(cat.id)"
        >
          <span>{{ cat.label }}</span>
          <span class="text-xs text-muted-foreground">
            {{ openCategory === cat.id ? "−" : "+" }}
          </span>
        </button>
        <div v-if="openCategory === cat.id" class="grid grid-cols-4 gap-1 p-1.5 pt-0">
          <button
            v-for="b in placeableBuildings(cat.id)"
            :key="b.name"
            :title="`${b.label} — ${b.description}`"
            class="flex aspect-square items-center justify-center rounded-md border text-lg transition-colors"
            :class="
              isBuildingActive(b.name)
                ? 'border-primary bg-primary/15'
                : 'border-transparent hover:border-border hover:bg-accent'
            "
            @click="setTool({ kind: 'build', building: b.name })"
          >
            {{ b.icon }}
          </button>
        </div>
      </div>
    </div>

    <p class="mt-auto text-[11px] leading-relaxed text-muted-foreground">
      Drag (or hold <kbd>Shift</kbd>) to pan, scroll to zoom. Keys
      <kbd>1</kbd>–<kbd>6</kbd> pick a tool, <kbd>Esc</kbd> cancels. Roads are
      drawn one tile at a time from flag to flag.
    </p>
  </aside>
</template>
