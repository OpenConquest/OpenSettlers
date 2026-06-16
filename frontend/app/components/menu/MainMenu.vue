<script setup lang="ts">
/**
 * Landing screen: create a new game (human + AI player counts), rejoin a running
 * game, or restore a saved one. On success it navigates to `/game/{id}` with the
 * chosen player slot.
 */
import { onMounted, ref } from "vue";
import { toast } from "vue-sonner";
import type { SaveSummary } from "~/types/game";

const api = useGameApi();
const router = useRouter();

const playerCount = ref(2);
const aiPlayers = ref(1);
const creating = ref(false);

const runningGames = ref<string[]>([]);
const saves = ref<SaveSummary[]>([]);

async function refresh(): Promise<void> {
  try {
    [runningGames.value, saves.value] = await Promise.all([api.listGames(), api.listSaves()]);
  } catch {
    // Backend offline: leave the lists empty, the create flow surfaces errors.
  }
}

async function createGame(): Promise<void> {
  creating.value = true;
  try {
    const res = await api.createGame({
      playerCount: playerCount.value,
      aiPlayers: Math.min(aiPlayers.value, playerCount.value - 1 < 0 ? 0 : aiPlayers.value),
    });
    enterGame(res.gameId, 0);
  } catch {
    toast.error("Could not create the game. Is the backend running?");
  } finally {
    creating.value = false;
  }
}

async function loadSave(saveId: number): Promise<void> {
  try {
    const res = await api.loadSave(saveId);
    enterGame(res.gameId, 0);
  } catch {
    toast.error("Could not load this save.");
  }
}

function enterGame(gameId: string, playerId: number): void {
  router.push({ path: `/game/${gameId}`, query: { player: String(playerId) } });
}

function joinAsSpectator(gameId: string): void {
  router.push({ path: `/game/${gameId}`, query: { spectate: "1" } });
}

onMounted(refresh);
</script>

<template>
  <div class="settlers-menu-container">
    <div class="background-overlay"></div>
    <main class="menu-board">
      <header class="menu-header">
        <h1>Open Settlers</h1>
        <p>A real-time settlement &amp; conquest game</p>
      </header>

      <div class="menu-content">
        <section class="menu-section new-game-section">
          <h2>New Game</h2>
          
          <div class="settings-grid">
            <div class="setting-item">
              <label>
                <span class="label-text">Players</span>
                <span class="value-badge">{{ playerCount }}</span>
              </label>
              <input
                v-model.number="playerCount"
                type="range"
                min="1"
                max="4"
                class="settlers-slider"
              />
            </div>
            
            <div class="setting-item">
              <label>
                <span class="label-text">AI Opponents</span>
                <span class="value-badge">{{ aiPlayers }}</span>
              </label>
              <input
                v-model.number="aiPlayers"
                type="range"
                min="0"
                :max="playerCount"
                class="settlers-slider"
              />
            </div>
          </div>
          
          <button class="settlers-btn primary-btn" :disabled="creating" @click="createGame">
            <span class="btn-inner">{{ creating ? "Creating…" : "Create & Play" }}</span>
          </button>
        </section>

        <section v-if="runningGames.length" class="menu-section">
          <h2>Running Games</h2>
          <ul class="game-list">
            <li v-for="id in runningGames" :key="id" class="list-item">
              <span class="game-id">{{ id }}</span>
              <div class="actions">
                <button class="settlers-btn secondary-btn" @click="joinAsSpectator(id)">
                  <span class="btn-inner">Spectate</span>
                </button>
                <button class="settlers-btn primary-btn small-btn" @click="enterGame(id, 0)">
                  <span class="btn-inner">Join</span>
                </button>
              </div>
            </li>
          </ul>
        </section>

        <section v-if="saves.length" class="menu-section">
          <h2>Saved Games</h2>
          <ul class="game-list">
            <li v-for="s in saves" :key="s.id" class="list-item">
              <span class="save-name">{{ s.name }}</span>
              <button class="settlers-btn secondary-btn" @click="loadSave(s.id)">
                <span class="btn-inner">Load</span>
              </button>
            </li>
          </ul>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=MedievalSharp&family=Cinzel:wght@600;800&display=swap');

.settlers-menu-container {
  min-height: 100vh;
  width: 100%;
  background-image: url('/images/bg.jpg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-family: 'MedievalSharp', serif;
  color: #3b2a1a;
  overflow-x: hidden;
}

.background-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, transparent 20%, rgba(0, 0, 0, 0.6) 100%);
  pointer-events: none;
}

.menu-board {
  position: relative;
  width: 100%;
  max-width: 650px;
  background-image: url('/images/parchment.jpg');
  background-size: cover;
  background-position: center;
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7), inset 0 0 40px rgba(139, 69, 19, 0.3);
  padding: 40px;
  z-index: 10;
  margin: 20px;
  animation: floatIn 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  opacity: 0;
  transform: translateY(30px) scale(0.95);
}

.menu-board::before {
  content: '';
  position: absolute;
  inset: -15px;
  background-image: url('/images/wood.jpg');
  background-size: 300px;
  z-index: -1;
  border-radius: 18px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.6), inset 0 0 10px rgba(0,0,0,0.8);
  border: 2px solid #2a1608;
}

.menu-board::after {
  content: '';
  position: absolute;
  inset: -22px;
  border: 4px solid #b8860b;
  border-radius: 22px;
  z-index: -2;
  box-shadow: 0 0 15px #daa520;
  opacity: 0.8;
}

@keyframes floatIn {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.menu-header {
  text-align: center;
  margin-bottom: 30px;
  border-bottom: 2px solid rgba(139, 69, 19, 0.3);
  padding-bottom: 20px;
  position: relative;
}

.menu-header::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 10%;
  width: 80%;
  height: 6px;
  background: url('/images/wood.jpg');
  border-radius: 3px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.menu-header h1 {
  font-family: 'Cinzel', serif;
  font-size: 3.5rem;
  font-weight: 800;
  color: #8b0000;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.4), 0 0 10px rgba(218, 165, 32, 0.5);
  margin: 0;
  letter-spacing: 2px;
}

.menu-header p {
  font-size: 1.2rem;
  color: #4a3320;
  margin-top: 10px;
  font-style: italic;
  font-weight: bold;
}

.menu-content {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.menu-section {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(139, 69, 19, 0.2);
  border-radius: 8px;
  padding: 20px;
  box-shadow: inset 0 0 15px rgba(139, 69, 19, 0.1);
}

.menu-section h2 {
  font-family: 'Cinzel', serif;
  font-size: 1.6rem;
  color: #5c3a21;
  margin-top: 0;
  margin-bottom: 15px;
  border-bottom: 1px dashed rgba(139, 69, 19, 0.4);
  padding-bottom: 5px;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 25px;
}

@media (max-width: 500px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.setting-item label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.2rem;
  font-weight: bold;
}

.value-badge {
  background-image: url('/images/wood.jpg');
  color: #f4e8c1;
  padding: 2px 12px;
  border-radius: 12px;
  font-family: 'Cinzel', serif;
  box-shadow: inset 0 0 5px rgba(0,0,0,0.8), 0 2px 4px rgba(0,0,0,0.3);
  border: 1px solid #2a1608;
}

.settlers-slider {
  -webkit-appearance: none;
  width: 100%;
  height: 12px;
  border-radius: 6px;
  background-image: url('/images/wood.jpg');
  outline: none;
  box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.8), 0 1px 1px rgba(255,255,255,0.4);
  border: 1px solid #2a1608;
}

.settlers-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #ffd700, #b8860b);
  border: 2px solid #5c3a21;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.5), inset 0 2px 4px rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: transform 0.1s;
}

.settlers-slider::-webkit-slider-thumb:hover {
  transform: scale(1.15);
}

.settlers-btn {
  width: 100%;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  outline: none;
  border-radius: 6px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.4);
  transition: all 0.2s ease;
  position: relative;
}

.settlers-btn::before {
  content: '';
  position: absolute;
  inset: -2px;
  background: linear-gradient(to bottom, #f4e8c1, #b8860b);
  border-radius: 8px;
  z-index: -1;
  opacity: 0.8;
}

.btn-inner {
  display: block;
  padding: 12px 24px;
  background-image: url('/images/wood.jpg');
  background-size: cover;
  border-radius: 6px;
  color: #f4e8c1;
  font-family: 'Cinzel', serif;
  font-size: 1.3rem;
  font-weight: bold;
  text-shadow: 1px 1px 2px black;
  box-shadow: inset 0 2px 5px rgba(255,255,255,0.2), inset 0 -2px 5px rgba(0,0,0,0.5);
  border: 1px solid #2a1608;
  transition: all 0.2s ease;
}

.settlers-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.5), 0 0 15px rgba(218, 165, 32, 0.4);
}

.settlers-btn:hover .btn-inner {
  filter: brightness(1.2);
  color: #fff;
}

.settlers-btn:active {
  transform: translateY(1px);
  box-shadow: 0 2px 3px rgba(0, 0, 0, 0.4);
}

.settlers-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.settlers-btn:disabled .btn-inner {
  filter: grayscale(0.8);
}

.secondary-btn .btn-inner {
  background-image: none;
  background-color: #d2b48c;
  color: #3b2a1a;
  text-shadow: none;
  border-color: #8b4513;
  box-shadow: inset 0 2px 5px rgba(255,255,255,0.5), inset 0 -2px 5px rgba(139,69,19,0.3);
}

.secondary-btn::before {
  background: #8b4513;
}

.secondary-btn:hover .btn-inner {
  background-color: #e6ccaa;
  color: #2a1608;
}

.small-btn .btn-inner {
  padding: 8px 16px;
  font-size: 1rem;
}

.game-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(244, 232, 193, 0.5);
  padding: 10px 15px;
  border-radius: 6px;
  border: 1px solid rgba(139, 69, 19, 0.3);
  transition: background 0.2s;
}

.list-item:hover {
  background: rgba(244, 232, 193, 0.8);
}

.game-id {
  font-family: monospace;
  font-size: 1rem;
  color: #5c3a21;
  background: rgba(255, 255, 255, 0.4);
  padding: 2px 6px;
  border-radius: 4px;
}

.save-name {
  font-size: 1.2rem;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
