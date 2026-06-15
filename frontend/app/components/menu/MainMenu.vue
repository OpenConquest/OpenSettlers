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
  <main class="mx-auto flex min-h-screen max-w-3xl flex-col gap-8 px-6 py-12">
    <header class="text-center">
      <h1 class="text-4xl font-bold tracking-tight">Open Settlers</h1>
      <p class="mt-2 text-muted-foreground">
        A real-time settlement &amp; conquest game — build your economy, claim
        territory, defeat your rivals.
      </p>
    </header>

    <section class="rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-4 text-lg font-semibold">New game</h2>
      <div class="grid gap-5 sm:grid-cols-2">
        <label class="space-y-2">
          <span class="flex items-center justify-between text-sm font-medium">
            Players <span class="text-muted-foreground">{{ playerCount }}</span>
          </span>
          <input
            v-model.number="playerCount"
            type="range"
            min="1"
            max="4"
            class="w-full accent-primary"
          />
        </label>
        <label class="space-y-2">
          <span class="flex items-center justify-between text-sm font-medium">
            AI opponents <span class="text-muted-foreground">{{ aiPlayers }}</span>
          </span>
          <input
            v-model.number="aiPlayers"
            type="range"
            min="0"
            :max="playerCount"
            class="w-full accent-primary"
          />
        </label>
      </div>
      <Button class="mt-6 w-full" :disabled="creating" @click="createGame">
        {{ creating ? "Creating…" : "Create &amp; play" }}
      </Button>
    </section>

    <section v-if="runningGames.length" class="rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-lg font-semibold">Running games</h2>
      <ul class="divide-y divide-border">
        <li v-for="id in runningGames" :key="id" class="flex items-center justify-between py-2">
          <code class="text-xs text-muted-foreground">{{ id }}</code>
          <div class="flex gap-2">
            <Button size="sm" variant="outline" @click="joinAsSpectator(id)">Spectate</Button>
            <Button size="sm" @click="enterGame(id, 0)">Join</Button>
          </div>
        </li>
      </ul>
    </section>

    <section v-if="saves.length" class="rounded-xl border border-border bg-card p-6 shadow-sm">
      <h2 class="mb-3 text-lg font-semibold">Saved games</h2>
      <ul class="divide-y divide-border">
        <li v-for="s in saves" :key="s.id" class="flex items-center justify-between py-2">
          <span class="text-sm">{{ s.name }}</span>
          <Button size="sm" variant="outline" @click="loadSave(s.id)">Load</Button>
        </li>
      </ul>
    </section>
  </main>
</template>
