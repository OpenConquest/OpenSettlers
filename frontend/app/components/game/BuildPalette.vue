<script setup lang="ts">
/**
 * Bottom bar with global game windows.
 */
const { togglePanel, toggleMinimap, setTool, tool } = useGameUi();

function toggleRoadTool(): void {
  tool.value.kind === 'road' ? setTool({ kind: 'inspect' }) : setTool({ kind: 'road' });
}

const emit = defineEmits<{
  (e: 'save'): void;
  (e: 'leave'): void;
}>();
</script>

<template>
  <div class="pointer-events-none flex w-full justify-center pb-4">
    <div class="pointer-events-auto flex items-center gap-6 rounded-full bg-black/60 backdrop-blur-md border-2 border-[#b8860b] px-8 py-2 shadow-[0_8px_32px_rgba(0,0,0,0.8)]">
      <div class="flex items-center gap-4">
        <button
          class="brass-btn h-10 px-5 text-sm font-bold shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
          title="Toggle minimap"
          @click="toggleMinimap()"
        >
          Map
        </button>
        <button
          class="brass-btn h-10 px-5 text-sm font-bold shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
          title="Inventory & empire overview"
          @click="togglePanel('inventory')"
        >
          Goods
        </button>
        <button
          class="brass-btn h-10 px-5 text-sm font-bold shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
          title="Set distribution priorities"
          @click="togglePanel('distribution')"
        >
          Distribution
        </button>
        <div class="h-6 w-px bg-black/40 shadow-[1px_0_0_rgba(255,255,255,0.1)]" />
        <button
          class="brass-btn h-10 px-5 text-sm font-bold shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
          :class="{ active: tool.kind === 'road' }"
          title="Build road"
          @click="toggleRoadTool()"
        >
          Road
        </button>
        <div class="h-6 w-px bg-black/40 shadow-[1px_0_0_rgba(255,255,255,0.1)]" />
        <button
          class="brass-btn h-10 px-5 text-sm font-bold shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
          title="Save game"
          @click="emit('save')"
        >
          Save
        </button>
        <button
          class="brass-btn h-10 px-5 text-sm font-bold shadow-[0_2px_4px_rgba(0,0,0,0.5)]"
          title="Leave game"
          @click="emit('leave')"
        >
          Leave
        </button>
      </div>
    </div>
  </div>
</template>
