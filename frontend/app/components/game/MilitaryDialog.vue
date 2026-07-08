<script setup lang="ts">
/**
 * The military window of the original game: a single strength control that sets
 * how fully the player's military buildings are garrisoned. Lower values keep
 * soldiers in reserve in the warehouses. The slider commits on release and the
 * authoritative value is read back from the server state.
 */
import { computed, ref, watch } from "vue";
import { useDraggable } from '@vueuse/core';
import { Swords } from "@lucide/vue";

const { state, session, actions } = useGameSession();
const { openPanel, togglePanel } = useGameUi();

const visible = computed(() => openPanel.value === "military" && session.playerId != null);

/** Local slider value; synced from the server only when the dialog opens. */
const value = ref(100);
watch(visible, (v) => {
  if (v && state.value?.militaryOccupation != null) value.value = state.value.militaryOccupation;
});

function onInput(e: Event): void {
  value.value = Number((e.target as HTMLInputElement).value);
}
function commit(): void {
  actions.setMilitary(value.value);
}

/** Total soldiers (garrisoned plus in the field) for context. */
const soldiers = computed(() => {
  let n = 0;
  for (const b of state.value?.buildings ?? []) if (b.playerId === session.playerId) n += b.garrison ?? 0;
  for (const s of state.value?.soldiers ?? []) if (s.playerId === session.playerId) n += 1;
  return n;
});

const el = ref<HTMLElement | null>(null);
const handle = ref<HTMLElement | null>(null);
const { style } = useDraggable(el, { initialValue: { x: 200, y: 200 }, handle });
</script>

<template>
  <div
    v-if="visible"
    ref="el"
    class="wood-panel pointer-events-auto fixed z-[60] w-[28rem] p-5"
    :style="style"
  >
    <div ref="handle" class="mb-4 flex items-center justify-between border-b border-amber-900/30 pb-2 cursor-move">
        <h2 class="cinzel-title flex items-center gap-2 text-xl font-bold text-amber-950">
          <Swords class="h-5 w-5" /> Military
        </h2>
        <button class="wood-btn flex h-6 w-6 items-center justify-center rounded-full text-xs" @click="togglePanel('military')">✕</button>
      </div>

      <p class="mb-4 text-xs font-bold text-amber-900/80">
        How fully your military buildings are staffed. Lower strength keeps soldiers in
        reserve in your warehouses; every building always holds at least one defender.
      </p>

      <div class="mb-2 flex items-baseline justify-between">
        <span class="text-sm font-bold text-amber-900">Occupation strength</span>
        <span class="cinzel-title text-2xl font-bold text-amber-950 tabular-nums">{{ value }}%</span>
      </div>
      <input
        type="range"
        min="0"
        max="100"
        step="10"
        :value="value"
        class="w-full accent-amber-800"
        @input="onInput"
        @change="commit"
      />
      <div class="mt-1 flex justify-between text-[10px] font-bold uppercase tracking-wide text-amber-900/60">
        <span>Reserve</span>
        <span>Full strength</span>
      </div>

      <p class="mt-4 border-t border-amber-900/30 pt-3 text-center text-sm font-bold text-amber-950">
        Soldiers in service: <span class="tabular-nums">{{ soldiers }}</span>
      </p>
  </div>
</template>
