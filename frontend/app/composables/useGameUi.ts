/**
 * Local UI interaction state for the game view: the active tool, the currently
 * selected building, and the in-progress road being drawn. Kept separate from
 * {@link useGameSession} (server state) so the rendering and input layers stay
 * decoupled.
 */
import { ref, shallowRef } from "vue";
import type { BuildingName, Coordinates } from "~/types/game";

/** What a left-click currently does on the map. */
export type ToolKind =
  | "inspect"
  | "flag"
  | "build"
  | "road"
  | "geologist"
  | "scout"
  | "destroy"
  | "attack";

export interface Tool {
  kind: ToolKind;
  /** Building to place when `kind === "build"`. */
  building?: BuildingName;
}

/**
 * A road being laid out. Only the chosen start flag is held here; the path to
 * the second flag is auto-routed (shortest buildable path) and previewed live by
 * {@link GameCanvas} as the cursor moves.
 */
export interface RoadDraft {
  startFlagId: string;
  start: Coordinates;
}

/** The global overlay dialogs that can be open (one at a time). */
export type GamePanel = "distribution" | "inventory" | "military";

const tool = ref<Tool>({ kind: "inspect" });
const selectedBuildingId = ref<string | null>(null);
const selectedTile = ref<Coordinates | null>(null);
const roadDraft = shallowRef<RoadDraft | null>(null);
const openPanel = ref<GamePanel | null>(null);

/**
 * How many soldiers the attack tool sends. `0` means "all available" (the
 * Settlers II default); a positive value caps the raid, drawing from the
 * closest garrisons first.
 */
const attackerCount = ref(0);

/** Opens a global panel, or closes it if it is already the open one. */
function togglePanel(panel: GamePanel): void {
  openPanel.value = openPanel.value === panel ? null : panel;
}

function setTool(next: Tool): void {
  tool.value = next;
  // Switching away from road drawing abandons any half-drawn road.
  if (next.kind !== "road") roadDraft.value = null;
  if (next.kind !== "inspect") {
    selectedBuildingId.value = null;
    selectedTile.value = null;
  }
}

function resetTool(): void {
  setTool({ kind: "inspect" });
}

const isMinimapOpen = ref(false);

function toggleMinimap(): void {
  isMinimapOpen.value = !isMinimapOpen.value;
}

export function useGameUi() {
  return { tool, selectedBuildingId, selectedTile, roadDraft, openPanel, attackerCount, isMinimapOpen, togglePanel, toggleMinimap, setTool, resetTool };
}
