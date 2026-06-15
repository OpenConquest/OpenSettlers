<script setup lang="ts">
/**
 * Floating detail card for the currently inspected building. Shows construction
 * progress, productivity, garrison or stored goods depending on the building
 * kind, and offers the contextual actions (demolish own building, attack enemy).
 */
import { computed } from "vue";
import { BUILDINGS, isMilitary } from "~/lib/buildings";
import { RESOURCE_ICONS, playerColor } from "~/lib/palette";

const { state, session, actions } = useGameSession();
const { selectedBuildingId } = useGameUi();

const building = computed(() =>
  state.value?.buildings.find((b) => b.id === selectedBuildingId.value) ?? null,
);

const meta = computed(() => (building.value?.name ? BUILDINGS[building.value.name] : null));
const isOwn = computed(() => building.value?.playerId === session.playerId);
const stored = computed(() => Object.entries(building.value?.storedResources ?? {}));

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
</script>

<template>
  <div
    v-if="building && meta"
    class="absolute bottom-4 right-4 w-72 rounded-lg border border-border bg-card/95 p-3 shadow-lg backdrop-blur"
  >
    <div class="mb-2 flex items-start justify-between">
      <div class="flex items-center gap-2">
        <span class="text-2xl">{{ meta.icon }}</span>
        <div>
          <p class="font-semibold leading-tight">{{ meta.label }}</p>
          <p class="flex items-center gap-1 text-xs text-muted-foreground">
            <span
              class="inline-block size-2 rounded-full"
              :style="{ backgroundColor: playerColor(building.playerId) }"
            />
            Player {{ building.playerId + 1 }}
          </p>
        </div>
      </div>
      <button class="text-muted-foreground hover:text-foreground" @click="close">✕</button>
    </div>

    <div class="space-y-1.5 text-sm">
      <p v-if="building.underConstruction" class="text-muted-foreground">
        Under construction — groundwork {{ building.groundworkProgress ?? 0 }}%, walls
        {{ building.buildingProgress ?? 0 }}%
      </p>

      <template v-else>
        <p v-if="building.productivity != null" class="flex justify-between">
          <span class="text-muted-foreground">Productivity</span>
          <span class="font-medium">{{ building.productivity }}%</span>
        </p>
        <p v-if="building.garrison != null" class="flex justify-between">
          <span class="text-muted-foreground">Garrison</span>
          <span class="font-medium">{{ building.garrison }} / {{ building.maxGarrison }}</span>
        </p>
        <p v-if="building.coins != null && isMilitary(building.name!)" class="flex justify-between">
          <span class="text-muted-foreground">Gold coins</span>
          <span class="font-medium">{{ building.coins }}</span>
        </p>

        <div v-if="stored.length" class="flex flex-wrap gap-1.5 pt-1">
          <span
            v-for="[res, qty] in stored"
            :key="res"
            class="flex items-center gap-0.5 rounded bg-muted px-1.5 py-0.5 text-xs"
          >
            {{ RESOURCE_ICONS[res as keyof typeof RESOURCE_ICONS] }} {{ qty }}
          </span>
        </div>
      </template>
    </div>

    <div class="mt-3 flex gap-2">
      <Button
        v-if="isOwn && building.name !== 'HEADQUARTERS'"
        variant="destructive"
        size="sm"
        class="flex-1"
        @click="demolish"
      >
        Demolish
      </Button>
      <Button v-else-if="!isOwn" variant="destructive" size="sm" class="flex-1" @click="attack">
        Attack
      </Button>
    </div>
  </div>
</template>
