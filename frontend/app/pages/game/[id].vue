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
const { clearLog, log } = useGameLog();

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
  clearLog();
  connect(gameId, playerId);
  log("Joined the game.", "good");
});

onBeforeUnmount(disconnect);
</script>

<template>
  <div class="relative h-screen w-screen overflow-hidden bg-black font-serif">
    <!-- Game Canvas covers the entire screen -->
    <div class="absolute inset-0">
      <GameCanvas />
    </div>

    <!-- UI Overlay container, passes clicks through unless hitting a UI element -->
    <div class="pointer-events-none absolute inset-0">
      
      <!-- Top Bar HUD -->
      <GameHud class="absolute top-0 left-0 right-0" @save="save" @leave="leave" />
      
      <!-- Minimap (Top Left) -->
      <Minimap class="absolute top-12 left-12" />

      <!-- Message log (Top Right) -->
      <MessageLog class="absolute right-12 top-14" />

      <!-- Bottom control bar (full width) -->
      <div class="absolute bottom-0 left-0 right-0">
        <BuildPalette />
      </div>

      <!-- Selection Panel (Bottom Left) -->
      <SelectionPanel class="absolute bottom-12 left-12" />

      <TileInspector />
    </div>

    <!-- Full screen ornamental wooden borders (pointer-events-none so we can click through) -->
    <div class="pointer-events-none absolute inset-0 z-50">
      <!-- Left pillar -->
      <div class="absolute top-0 bottom-0 left-0 w-8 bg-[url('/images/wood.jpg')] bg-cover border-r-2 border-black shadow-[inset_-4px_0_10px_rgba(0,0,0,0.9)]"></div>
      <!-- Right pillar -->
      <div class="absolute top-0 bottom-0 right-0 w-8 bg-[url('/images/wood.jpg')] bg-cover border-l-2 border-black shadow-[inset_4px_0_10px_rgba(0,0,0,0.9)]"></div>
    </div>

    <!-- Global overlay dialogs (self-gated by the active panel). -->
    <DistributionDialog />
    <InventoryPanel />
    <MilitaryDialog />

    <GameOverDialog @leave="leave" />
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=MedievalSharp&family=Cinzel:wght@600;800&display=swap');

/* Shared wooden panel styles for the in-game UI */
.wood-panel {
  pointer-events: auto;
  position: relative;
  /* A light cream wash over the parchment photo keeps dark text readable
     everywhere, instead of fighting the texture's mid-tones. */
  background-image:
    linear-gradient(rgba(247, 235, 208, 0.82), rgba(236, 219, 184, 0.86)),
    url('/images/parchment.jpg');
  background-size: cover;
  border-radius: 6px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.8), inset 0 0 20px rgba(139, 69, 19, 0.25);
  color: #2a1c0c;
  font-family: 'MedievalSharp', serif;
  text-shadow: 0 1px 0 rgba(255, 248, 230, 0.4);
  z-index: 10;
}

.wood-panel::before {
  content: '';
  position: absolute;
  inset: -12px;
  background-image: url('/images/wood.jpg');
  background-size: 200px;
  border-radius: 12px;
  z-index: -1;
  box-shadow: inset 0 0 8px rgba(0,0,0,0.9), 0 5px 15px rgba(0,0,0,0.6);
  border: 2px solid #2a1608;
}

.wood-panel::after {
  content: '';
  position: absolute;
  inset: -18px;
  border: 3px solid #b8860b;
  border-radius: 16px;
  z-index: -2;
  box-shadow: 0 0 10px #daa520;
  opacity: 0.7;
}

.cinzel-title {
  font-family: 'Cinzel', serif;
}

.wood-btn {
  pointer-events: auto;
  position: relative;
  /* Darken the wood grain a touch so the cream label always has contrast. */
  background-image:
    linear-gradient(rgba(38, 21, 8, 0.32), rgba(18, 9, 3, 0.46)),
    url('/images/wood.jpg');
  background-size: cover;
  color: #f8efd6;
  border: 1px solid #2a1608;
  border-radius: 4px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.8);
  box-shadow: inset 0 1px 3px rgba(255,255,255,0.2), inset 0 -2px 5px rgba(0,0,0,0.6), 0 2px 4px rgba(0,0,0,0.5);
  transition: all 0.1s;
}

.wood-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.15);
  box-shadow: inset 0 1px 3px rgba(255,255,255,0.3), inset 0 -2px 5px rgba(0,0,0,0.6), 0 4px 8px rgba(0,0,0,0.6);
}

.wood-btn:active {
  transform: translateY(1px);
  box-shadow: inset 0 2px 5px rgba(0,0,0,0.8), 0 1px 2px rgba(0,0,0,0.5);
}

.wood-btn.active {
  box-shadow: inset 0 2px 8px rgba(0,0,0,0.9), 0 0 8px rgba(218, 165, 32, 0.8);
  border-color: #daa520;
}

/* Pure dark wooden panel, mostly for borders and top/bottom bars */
.dark-wood {
  pointer-events: auto;
  position: relative;
  /* Deepen the wood so the cream text and resource icons read clearly. */
  background-image:
    linear-gradient(rgba(33, 18, 6, 0.55), rgba(18, 9, 3, 0.66)),
    url('/images/wood.jpg');
  background-size: 200px;
  color: #f7ecd2;
  text-shadow: 0 1px 2px rgba(0,0,0,0.85);
  box-shadow: inset 0 0 8px rgba(0,0,0,0.9), 0 5px 15px rgba(0,0,0,0.6);
  border: 2px solid #2a1608;
}

/* Round brass control, the signature button of the Settlers II toolbar. */
.brass-btn {
  pointer-events: auto;
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  color: #2a1608;
  background:
    radial-gradient(circle at 35% 28%, #ffeaa6 0%, #ecb84e 40%, #b07d27 72%, #6e4a14 100%);
  border: 2px solid #3a2408;
  box-shadow:
    0 0 0 2px #d9a441,
    inset 0 2px 4px rgba(255, 255, 255, 0.55),
    inset 0 -5px 9px rgba(0, 0, 0, 0.45),
    0 3px 6px rgba(0, 0, 0, 0.6);
  transition: transform 0.08s ease, filter 0.08s ease;
}

.brass-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.08);
}

.brass-btn:active {
  transform: translateY(1px);
}

.brass-btn.active {
  color: #1c0e02;
  box-shadow:
    0 0 0 2px #ffe08a,
    0 0 16px rgba(255, 200, 80, 0.95),
    inset 0 2px 4px rgba(255, 255, 255, 0.55),
    inset 0 -5px 9px rgba(0, 0, 0, 0.45),
    0 3px 6px rgba(0, 0, 0, 0.6);
}
</style>
