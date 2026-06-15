/**
 * Live game session: owns the WebSocket connection to `/game/{gameId}`, decodes
 * the incoming {@link ServerMessage}s into reactive state, and sends the local
 * player's {@link ClientMessage}s back up.
 *
 * Implemented as a module-level singleton because only one game is ever viewed
 * at a time; the game page drives {@link connect}/{@link disconnect} and every
 * child component reads the same reactive refs.
 */
import { reactive, ref, shallowRef } from "vue";
import { hexKey } from "~/lib/hex";
import type {
  BuildingName,
  ClientMessage,
  Coordinates,
  GameOverMessage,
  MapMessage,
  ServerMessage,
  StateMessage,
  TileDto,
} from "~/types/game";

export type ConnectionStatus = "idle" | "connecting" | "open" | "closed" | "error";

/** Indexed static map: tiles keyed by `"x,y"` for O(1) lookup while rendering. */
interface GameMap {
  size: number;
  tiles: Map<string, TileDto>;
}

const status = ref<ConnectionStatus>("idle");
const map = shallowRef<GameMap | null>(null);
const state = shallowRef<StateMessage | null>(null);
const gameOver = ref<GameOverMessage | null>(null);
const tick = ref(0);

/** Identity of the connection: which game and as which player (null = spectator). */
const session = reactive<{ gameId: string | null; playerId: number | null }>({
  gameId: null,
  playerId: null,
});

let socket: WebSocket | null = null;

/** Reconnection bookkeeping: the server loop can outlive a dropped socket. */
const MAX_RECONNECT_ATTEMPTS = 5;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectAttempts = 0;
/** Set while {@link disconnect} is tearing down, to suppress auto-reconnect. */
let closing = false;

function handleMessage(raw: string): void {
  let msg: ServerMessage;
  try {
    msg = JSON.parse(raw) as ServerMessage;
  } catch {
    return;
  }

  switch (msg.type) {
    case "MAP": {
      const m = msg as MapMessage;
      const tiles = new Map<string, TileDto>();
      for (const t of m.tiles) tiles.set(hexKey(t.x, t.y), t);
      map.value = { size: m.size, tiles };
      break;
    }
    case "STATE": {
      state.value = msg as StateMessage;
      tick.value = msg.tick;
      break;
    }
    case "GAME_OVER": {
      gameOver.value = msg as GameOverMessage;
      break;
    }
  }
}

/** (Re)opens the WebSocket for the current {@link session} identity. Cached map
 *  and state are left untouched so a reconnect does not blank the screen. */
function openSocket(): void {
  const { public: cfg } = useRuntimeConfig();
  const query = session.playerId != null ? `?playerId=${session.playerId}` : "";
  const url = `${cfg.wsBase}/game/${session.gameId}${query}`;

  status.value = "connecting";
  socket = new WebSocket(url);
  socket.onopen = () => {
    status.value = "open";
    reconnectAttempts = 0;
  };
  socket.onmessage = (e) => handleMessage(e.data as string);
  socket.onerror = () => (status.value = "error");
  socket.onclose = () => {
    socket = null;
    if (closing) return;
    status.value = "closed";
    scheduleReconnect();
  };
}

/** Backs off and retries the connection a few times after an unexpected drop. */
function scheduleReconnect(): void {
  if (closing || gameOver.value || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return;
  const delay = Math.min(8000, 500 * 2 ** reconnectAttempts);
  reconnectAttempts++;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    if (!closing && session.gameId) openSocket();
  }, delay);
}

/**
 * Opens the WebSocket for a game. Re-connecting closes any previous socket and
 * resets the cached state.
 *
 * @param gameId   the game to join
 * @param playerId the player to control, or `null` to spectate
 */
function connect(gameId: string, playerId: number | null): void {
  disconnect();

  closing = false;
  reconnectAttempts = 0;
  session.gameId = gameId;
  session.playerId = playerId;
  gameOver.value = null;

  openSocket();
}

/** Closes the current connection and clears cached state. */
function disconnect(): void {
  closing = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (socket) {
    socket.onclose = null;
    socket.close();
    socket = null;
  }
  status.value = "idle";
  map.value = null;
  state.value = null;
}

/** Sends a raw client message, no-op when no player is bound or socket closed. */
function send(message: Omit<ClientMessage, "playerId">): void {
  if (!socket || socket.readyState !== WebSocket.OPEN || session.playerId == null) return;
  const payload: ClientMessage = { ...message, playerId: session.playerId };
  socket.send(JSON.stringify(payload));
}

/* ----------------------------- Action helpers ---------------------------- */

const actions = {
  build: (buildingName: BuildingName, position: Coordinates) =>
    send({ type: "BUILD_BUILDING", buildingName, position }),
  destroy: (targetId: string) => send({ type: "DESTROY_BUILDING", targetId }),
  placeFlag: (position: Coordinates) => send({ type: "PLACE_FLAG", position }),
  linkFlags: (flagIdA: string, flagIdB: string, path: Coordinates[]) =>
    send({ type: "LINK_FLAGS", flagIdA, flagIdB, path }),
  attack: (targetId: string) => send({ type: "ATTACK_BUILDING", targetId }),
  sendGeologist: (flagId: string) => send({ type: "SEND_GEOLOGIST", targetId: flagId }),
};

export function useGameSession() {
  return {
    status,
    map,
    state,
    gameOver,
    tick,
    session,
    connect,
    disconnect,
    actions,
  };
}
