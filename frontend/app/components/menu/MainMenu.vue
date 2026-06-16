<script setup lang="ts">
/**
 * Landing screen: create a new game (human + AI player counts), rejoin a running
 * game, or restore a saved one. On success it navigates to `/game/{id}` with the
 * chosen player slot.
 */
import { computed, onMounted, ref, watch } from "vue";
import { toast } from "vue-sonner";
import { Archive, Castle, Eye, LogIn, Play, RefreshCw, Users } from "@lucide/vue";
import { PLAYER_COLORS } from "~/lib/palette";
import type { SaveSummary } from "~/types/game";

const api = useGameApi();
const router = useRouter();

const playerCount = ref(2);
const aiPlayers = ref(1);
const creating = ref(false);
const refreshing = ref(false);
/** Backend reachability: `null` while first checking, then `true`/`false`. */
const online = ref<boolean | null>(null);

const runningGames = ref<string[]>([]);
const saves = ref<SaveSummary[]>([]);

const playerOptions = [1, 2, 3, 4];
/** AI can take any seat but the first: at least one human slot is kept. */
const aiOptions = computed(() => Array.from({ length: playerCount.value }, (_, i) => i));

watch(playerCount, (n) => {
  if (aiPlayers.value > n - 1) aiPlayers.value = Math.max(0, n - 1);
});

/** Seat preview: seat 0 is you, the other human seats are open, the rest AI. */
const lineup = computed(() => {
  const humans = playerCount.value - aiPlayers.value;
  return Array.from({ length: playerCount.value }, (_, i) => ({
    color: PLAYER_COLORS[i % PLAYER_COLORS.length]!,
    kind: i === 0 ? "you" : i < humans ? "open" : "ai",
  }));
});

async function refresh(): Promise<void> {
  refreshing.value = true;
  try {
    [runningGames.value, saves.value] = await Promise.all([api.listGames(), api.listSaves()]);
    online.value = true;
  } catch {
    online.value = false;
  } finally {
    refreshing.value = false;
  }
}

async function createGame(): Promise<void> {
  creating.value = true;
  try {
    const res = await api.createGame({ playerCount: playerCount.value, aiPlayers: aiPlayers.value });
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

const shortId = (id: string): string => id.slice(0, 8);

function saveMeta(s: SaveSummary): string {
  if (s.createdAt) return new Date(s.createdAt).toLocaleString();
  if (s.tick != null) return `tick ${s.tick}`;
  return "saved game";
}

const seatLabel = (kind: string): string => (kind === "you" ? "You" : kind === "ai" ? "AI" : "Open");

onMounted(refresh);
</script>

<template>
  <div class="home">
    <div class="vignette" />

    <main class="board">
      <span class="stud tl" /><span class="stud tr" /><span class="stud bl" /><span class="stud br" />

      <div class="board-face">
      <header class="board-header">
        <div class="crest"><Castle :size="32" :stroke-width="1.6" /></div>
        <h1 class="title">Open Settlers</h1>
        <p class="tagline">Real-time settlement &amp; conquest</p>
      </header>

      <div class="cards">
        <!-- New game -->
        <section class="card">
          <h2 class="card-title"><Users class="i" :size="20" /> New Game</h2>

          <div class="field">
            <span class="field-label">Players</span>
            <div class="seg">
              <button
                v-for="n in playerOptions"
                :key="n"
                class="seg-btn"
                :class="{ active: playerCount === n }"
                @click="playerCount = n"
              >{{ n }}</button>
            </div>
          </div>

          <div class="field">
            <span class="field-label">AI opponents</span>
            <div class="seg">
              <button
                v-for="n in aiOptions"
                :key="n"
                class="seg-btn"
                :class="{ active: aiPlayers === n }"
                @click="aiPlayers = n"
              >{{ n }}</button>
            </div>
          </div>

          <div class="lineup">
            <span class="field-label">Lineup</span>
            <div class="seats">
              <span v-for="(p, i) in lineup" :key="i" class="seat" :class="p.kind">
                <span class="seat-dot" :style="{ background: p.color }" />
                <span class="seat-tag">{{ seatLabel(p.kind) }}</span>
              </span>
            </div>
          </div>

          <button class="cta" :disabled="creating" @click="createGame">
            <span class="cta-inner"><Play class="i" :size="20" /> {{ creating ? "Creating…" : "Create & Play" }}</span>
          </button>
        </section>

        <!-- Continue -->
        <section class="card">
          <h2 class="card-title"><LogIn class="i" :size="20" /> Continue</h2>

          <div class="sub">
            <h3 class="sub-title">Running games <span class="count">{{ runningGames.length }}</span></h3>
            <ul v-if="runningGames.length" class="rows">
              <li v-for="id in runningGames" :key="id" class="row">
                <div class="row-main">
                  <span class="row-title mono">{{ shortId(id) }}</span>
                  <span class="row-meta live">● live</span>
                </div>
                <div class="row-actions">
                  <button class="chip" @click="joinAsSpectator(id)"><Eye class="i" :size="14" /> Spectate</button>
                  <button class="chip gold" @click="enterGame(id, 0)"><LogIn class="i" :size="14" /> Join</button>
                </div>
              </li>
            </ul>
            <p v-else class="empty">No games running — start one above.</p>
          </div>

          <div class="sub">
            <h3 class="sub-title">Saved games <span class="count">{{ saves.length }}</span></h3>
            <ul v-if="saves.length" class="rows">
              <li v-for="s in saves" :key="s.id" class="row">
                <div class="row-main">
                  <span class="row-title">{{ s.name }}</span>
                  <span class="row-meta">{{ saveMeta(s) }}</span>
                </div>
                <button class="chip gold" @click="loadSave(s.id)"><Archive class="i" :size="14" /> Load</button>
              </li>
            </ul>
            <p v-else class="empty">No saved games yet.</p>
          </div>

          <p v-if="online === false" class="offline-hint">Backend offline — start it to list and load games.</p>
        </section>
      </div>

      <footer class="board-footer">An open-source homage to The Settlers II</footer>
      </div>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=MedievalSharp&family=Cinzel:wght@600;800&display=swap');

.home {
  min-height: 100vh;
  width: 100%;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background-image: url('/images/bg.jpg');
  background-size: cover;
  background-position: center;
  font-family: 'MedievalSharp', serif;
  color: #3b2a1a;
}

.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, transparent 25%, rgba(0, 0, 0, 0.65) 100%);
  pointer-events: none;
}

/* ----------------------------------- Board ----------------------------------- */

/* Outer layer: the wooden frame. Its 16px padding is the visible frame; the
   parchment face sits on top inside it (a child, not a negative-z pseudo, so
   nothing paints over the parchment). */
.board {
  position: relative;
  width: 100%;
  max-width: 760px;
  padding: 16px;
  z-index: 10;
  border-radius: 18px;
  background-image: url('/images/wood.jpg');
  background-size: 300px;
  border: 2px solid #2a1608;
  box-shadow: 0 28px 55px -12px rgba(0, 0, 0, 0.75), inset 0 0 14px rgba(0, 0, 0, 0.85);
  animation: floatIn 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  opacity: 0;
  transform: translateY(28px) scale(0.97);
}

/* The glowing brass ring around the frame. */
.board::after {
  content: '';
  position: absolute;
  inset: -8px;
  z-index: -1;
  border-radius: 22px;
  border: 4px solid #b8860b;
  box-shadow: 0 0 18px rgba(218, 165, 32, 0.65);
  opacity: 0.85;
}

/* Inner layer: the parchment writing surface. A light cream wash over the
   mid-toned parchment photo keeps the dark text readable (as the in-game panels do). */
.board-face {
  position: relative;
  border-radius: 12px;
  padding: 34px 38px 26px;
  background-color: #e9d8b1;
  background-image:
    linear-gradient(rgba(247, 235, 208, 0.9), rgba(233, 215, 179, 0.93)),
    url('/images/parchment.jpg');
  background-size: cover;
  background-position: center;
  box-shadow: inset 0 0 35px rgba(139, 69, 19, 0.22);
}

.stud {
  position: absolute;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  z-index: 2;
  background: radial-gradient(circle at 35% 30%, #ffe9a6 0%, #e0b54e 45%, #8a5e1c 100%);
  border: 1px solid #2a1608;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.6), inset 0 1px 2px rgba(255, 255, 255, 0.6);
}
.stud.tl { top: 8px; left: 8px; }
.stud.tr { top: 8px; right: 8px; }
.stud.bl { bottom: 8px; left: 8px; }
.stud.br { bottom: 8px; right: 8px; }

@keyframes floatIn {
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ---------------------------------- Header ----------------------------------- */

.board-header {
  text-align: center;
  position: relative;
  padding-bottom: 22px;
  margin-bottom: 24px;
  border-bottom: 2px solid rgba(139, 69, 19, 0.3);
}

.crest {
  width: 64px;
  height: 64px;
  margin: 0 auto 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #3a2408;
  background: radial-gradient(circle at 35% 28%, #ffeaa6 0%, #ecb84e 42%, #b07d27 74%, #6e4a14 100%);
  border: 2px solid #2a1608;
  box-shadow: 0 0 0 3px #d9a441, 0 6px 14px rgba(0, 0, 0, 0.55), inset 0 2px 5px rgba(255, 255, 255, 0.55);
}

.title {
  margin: 0;
  font-family: 'Cinzel', serif;
  font-size: 3rem;
  font-weight: 800;
  letter-spacing: 2px;
  color: #8b0000;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.4), 0 0 12px rgba(218, 165, 32, 0.5);
}

.tagline {
  margin: 4px 0 0;
  font-size: 1.05rem;
  font-style: italic;
  font-weight: bold;
  color: #5c3a21;
}

.status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 14px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: bold;
  background: rgba(42, 22, 8, 0.12);
  border: 1px solid rgba(139, 69, 19, 0.35);
  color: #4a3320;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 6px currentColor;
}
.status-pill.up { color: #2f7d32; }
.status-pill.down { color: #9c2b2b; }
.status-pill.checking { color: #9a6b1a; }
.status-pill .status-dot { background: currentColor; }

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
  color: #2a1608;
  background: radial-gradient(circle at 35% 28%, #ffeaa6 0%, #ecb84e 45%, #b07d27 78%, #6e4a14 100%);
  border: 1px solid #2a1608;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.45), inset 0 1px 2px rgba(255, 255, 255, 0.55);
  transition: transform 0.12s ease, filter 0.12s ease;
}
.icon-btn:hover { transform: translateY(-1px); filter: brightness(1.08); }
.icon-btn.spin :deep(svg) { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ----------------------------------- Cards ----------------------------------- */

.cards { display: flex; flex-direction: column; gap: 20px; }

.card {
  padding: 22px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(139, 69, 19, 0.22);
  box-shadow: inset 0 0 16px rgba(139, 69, 19, 0.1);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 16px;
  padding-bottom: 8px;
  font-family: 'Cinzel', serif;
  font-size: 1.5rem;
  color: #5c3a21;
  border-bottom: 1px dashed rgba(139, 69, 19, 0.4);
}
.card-title .i { color: #8b5a2b; }

/* Fields & segmented selectors */

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.field-label { font-size: 1.1rem; font-weight: bold; color: #4a3320; }

.seg {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 10px;
  background-image: url('/images/wood.jpg');
  background-size: 160px;
  border: 1px solid #2a1608;
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.75);
}
.seg-btn {
  min-width: 38px;
  padding: 6px 13px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #e8d9b0;
  font-family: 'Cinzel', serif;
  font-size: 1.05rem;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.12s ease, color 0.12s ease, box-shadow 0.12s ease;
}
.seg-btn:hover { background: rgba(255, 255, 255, 0.1); }
.seg-btn.active {
  color: #2a1608;
  background: radial-gradient(circle at 35% 28%, #ffeaa6 0%, #ecb84e 45%, #b07d27 100%);
  box-shadow: 0 0 10px rgba(255, 200, 80, 0.6), inset 0 1px 2px rgba(255, 255, 255, 0.55);
}

/* Lineup preview */

.lineup {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 6px 0 20px;
}
.seats { display: flex; flex-wrap: wrap; gap: 12px; }
.seat { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.seat-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #2a1608;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.5), inset 0 1px 2px rgba(255, 255, 255, 0.4);
}
.seat.open .seat-dot { opacity: 0.45; }
.seat.you .seat-dot { box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.7), 0 0 8px rgba(255, 255, 255, 0.5); }
.seat-tag { font-size: 0.7rem; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; color: #5c3a21; }
.seat.ai .seat-tag { color: #9c2b2b; }

/* Primary call to action */

.cta {
  width: 100%;
  border: none;
  padding: 0;
  background: none;
  cursor: pointer;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.4);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.cta-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 14px 24px;
  border-radius: 8px;
  background-image: linear-gradient(rgba(38, 21, 8, 0.28), rgba(18, 9, 3, 0.42)), url('/images/wood.jpg');
  background-size: cover;
  color: #f8efd6;
  font-family: 'Cinzel', serif;
  font-size: 1.3rem;
  font-weight: bold;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.85);
  border: 1px solid #2a1608;
  box-shadow: 0 0 0 2px #b8860b, inset 0 2px 5px rgba(255, 255, 255, 0.2), inset 0 -2px 6px rgba(0, 0, 0, 0.55);
  transition: filter 0.18s ease;
}
.cta:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0, 0, 0, 0.5), 0 0 18px rgba(218, 165, 32, 0.45); }
.cta:hover .cta-inner { filter: brightness(1.12); }
.cta:active { transform: translateY(1px); }
.cta:disabled { opacity: 0.6; cursor: not-allowed; transform: none; box-shadow: none; }
.cta:disabled .cta-inner { filter: grayscale(0.7); }

/* Lobby lists */

.sub { margin-bottom: 16px; }
.sub:last-of-type { margin-bottom: 0; }
.sub-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 9px;
  font-family: 'Cinzel', serif;
  font-size: 1.12rem;
  color: #5c3a21;
}
.count {
  font-size: 0.75rem;
  font-weight: bold;
  padding: 1px 8px;
  border-radius: 999px;
  color: #f4e8c1;
  background: #8b5a2b;
  border: 1px solid #5c3a21;
}

.rows { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 14px;
  border-radius: 8px;
  background: rgba(244, 232, 193, 0.5);
  border: 1px solid rgba(139, 69, 19, 0.3);
  transition: background 0.15s ease, transform 0.15s ease;
}
.row:hover { background: rgba(244, 232, 193, 0.85); transform: translateY(-1px); }
.row-main { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.row-title { font-size: 1.05rem; font-weight: bold; color: #3b2a1a; overflow: hidden; text-overflow: ellipsis; }
.row-title.mono { font-family: 'Courier New', monospace; letter-spacing: 1px; }
.row-meta { font-size: 0.75rem; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; color: #6b4a2c; }
.row-meta.live { color: #2f7d32; }
.row-actions { display: flex; gap: 8px; flex-shrink: 0; }

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 7px;
  cursor: pointer;
  font-family: 'Cinzel', serif;
  font-size: 0.9rem;
  font-weight: bold;
  color: #3b2a1a;
  background: #d2b48c;
  border: 1px solid #8b4513;
  box-shadow: inset 0 1px 3px rgba(255, 255, 255, 0.5), 0 2px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.12s ease, filter 0.12s ease, background 0.12s ease;
}
.chip:hover { transform: translateY(-1px); background: #e6ccaa; }
.chip.gold {
  color: #2a1608;
  border-color: #5c3a21;
  background: radial-gradient(circle at 35% 28%, #ffeaa6 0%, #ecb84e 50%, #b07d27 100%);
}
.chip.gold:hover { filter: brightness(1.08); background: radial-gradient(circle at 35% 28%, #fff0bf 0%, #f0c25e 50%, #c08a2f 100%); }

.empty {
  margin: 0;
  padding: 14px;
  text-align: center;
  font-style: italic;
  color: #6b4a2c;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px dashed rgba(139, 69, 19, 0.3);
}

.offline-hint {
  margin: 12px 0 0;
  text-align: center;
  font-size: 0.9rem;
  font-weight: bold;
  color: #9c2b2b;
}

.board-footer {
  margin-top: 22px;
  text-align: center;
  font-size: 0.82rem;
  font-style: italic;
  color: rgba(74, 51, 32, 0.7);
}

/* --------------------------------- Responsive -------------------------------- */

@media (max-width: 560px) {
  .board { padding: 26px 22px 22px; }
  .title { font-size: 2.3rem; }
  .field, .lineup { flex-direction: column; align-items: flex-start; gap: 10px; }
  .row { flex-wrap: wrap; }
}
</style>
