<script setup lang="ts">
/**
 * Game route. Reads the game id from the path and the player slot from the query
 * (`?player=N`, or `?spectate=1`), opens the live session, and lays out the HUD,
 * build palette, canvas, minimap and overlays around it.
 */
import { onBeforeUnmount, onMounted } from "vue";
import { toast } from "vue-sonner";

const route = useRoute();
const router = useRouter();
const api = useGameApi();
const { connect, disconnect } = useGameSession();
const { resetTool } = useGameUi();

// Settlers-style keyboard control (number keys arm tools, Escape backs out).
useHotkeys();

const gameId = route.params.id as string;
const playerId = route.query.spectate ? null : Number(route.query.player ?? 0);

useHead({ title: "Open Settlers — In game" });

async function save(): Promise<void> {
  const name = window.prompt("Save name", `game-${new Date().toISOString().slice(0, 16)}`);
  if (!name) return;
  try {
    await api.saveGame(gameId, name);
    toast.success("Game saved.");
  } catch {
    toast.error("Save failed.");
  }
}

function leave(): void {
  router.push("/");
}

onMounted(() => {
  resetTool();
  connect(gameId, playerId);
});

onBeforeUnmount(disconnect);
</script>

<template>
  <div class="flex h-screen flex-col overflow-hidden">
    <GameHud @save="save" @leave="leave" />
    <div class="flex min-h-0 flex-1">
      <BuildPalette />
      <div class="relative min-w-0 flex-1">
        <GameCanvas />
        <Minimap />
        <SelectionPanel />
      </div>
    </div>
    <GameOverDialog @leave="leave" />
  </div>
</template>
