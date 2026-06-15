/**
 * Thin typed wrapper around the backend's REST endpoints
 * (`fr.opensettlers.controller.GameController` / `SaveController`).
 *
 * Every call returns a typed promise and throws on a non-2xx response so the
 * caller can surface errors with a toast.
 */
import type {
  CreateGameRequest,
  GameCreatedResponse,
  LoadResponse,
  SaveResponse,
  SaveSummary,
} from "~/types/game";

export function useGameApi() {
  const { public: cfg } = useRuntimeConfig();
  const base = cfg.apiBase;

  /** Creates a new game and starts its loop. */
  function createGame(request: CreateGameRequest): Promise<GameCreatedResponse> {
    return $fetch<GameCreatedResponse>(`${base}/games`, {
      method: "POST",
      body: request,
    });
  }

  /** Lists the ids of all running games. */
  function listGames(): Promise<string[]> {
    return $fetch<string[]>(`${base}/games`);
  }

  /** Stops a running game and discards its session. */
  function stopGame(gameId: string): Promise<void> {
    return $fetch<void>(`${base}/games/${gameId}`, { method: "DELETE" });
  }

  /** Saves a snapshot of a running game. */
  function saveGame(gameId: string, name: string): Promise<SaveResponse> {
    return $fetch<SaveResponse>(`${base}/games/${gameId}/save`, {
      method: "POST",
      body: { name },
    });
  }

  /** Lists every saved game. */
  function listSaves(): Promise<SaveSummary[]> {
    return $fetch<SaveSummary[]>(`${base}/saves`);
  }

  /** Restores a saved game and starts it as a fresh running game. */
  function loadSave(saveId: number): Promise<LoadResponse> {
    return $fetch<LoadResponse>(`${base}/saves/${saveId}/load`, {
      method: "POST",
    });
  }

  return { createGame, listGames, stopGame, saveGame, listSaves, loadSave };
}
