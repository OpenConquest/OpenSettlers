<script setup lang="ts">
/**
 * Modal shown when the server emits a `GAME_OVER` message, announcing the winner
 * (relative to the local player when possible) and returning to the main menu.
 */
import { computed } from "vue";

const emit = defineEmits<{ (e: "leave"): void }>();

const { gameOver, session } = useGameSession();

const open = computed(() => gameOver.value != null);

const title = computed(() => {
  const winner = gameOver.value?.winner;
  if (winner == null) return "Game over";
  if (session.playerId != null && winner === session.playerId) return "Victory!";
  if (session.playerId != null) return "Defeat";
  return `Player ${winner + 1} wins`;
});

const detail = computed(() => {
  const go = gameOver.value;
  if (!go) return "";
  const winner = go.winner != null ? `Player ${go.winner + 1}` : "Nobody";
  return `${winner} conquered the island after ${go.tick} ticks.`;
});
</script>

<template>
  <Dialog :open="open">
    <DialogContent class="sm:max-w-md" @pointer-down-outside.prevent @escape-key-down.prevent>
      <DialogHeader>
        <DialogTitle class="text-2xl">{{ title }}</DialogTitle>
        <DialogDescription>{{ detail }}</DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button class="w-full" @click="emit('leave')">Back to menu</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
