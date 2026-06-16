<script setup lang="ts">
/**
 * Top-left activity log, styled after the Settlers II message list: translucent
 * dark rows with a coloured bullet. Fed by {@link useGameLog}.
 */
import type { LogTone } from "~/composables/useGameLog";

const { entries } = useGameLog();

const BULLET: Record<LogTone, string> = {
  info: "bg-amber-300",
  good: "bg-emerald-400",
  warn: "bg-yellow-400",
  bad: "bg-red-400",
};
</script>

<template>
  <div class="pointer-events-none flex w-72 flex-col items-end space-y-1">
    <TransitionGroup name="log">
      <div
        v-for="e in entries"
        :key="e.id"
        class="flex w-fit max-w-full items-center gap-2 rounded-sm border border-black/40 bg-black/55 px-2.5 py-1 text-[12px] font-semibold text-amber-50 shadow-[0_2px_6px_rgba(0,0,0,0.6)] backdrop-blur-sm"
      >
        <span :class="['size-2 shrink-0 rounded-full shadow', BULLET[e.tone]]" />
        <span class="truncate drop-shadow-[0_1px_1px_rgba(0,0,0,0.9)]">{{ e.text }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.log-enter-active,
.log-leave-active {
  transition: all 0.25s ease;
}
.log-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}
.log-leave-to {
  opacity: 0;
}
</style>
