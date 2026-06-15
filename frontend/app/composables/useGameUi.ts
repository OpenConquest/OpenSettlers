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
  | "destroy"
  | "attack";

export interface Tool {
  kind: ToolKind;
  /** Building to place when `kind === "build"`. */
  building?: BuildingName;
}

/** A road being laid out flag-to-flag, segment by segment. */
export interface RoadDraft {
  startFlagId: string;
  start: Coordinates;
  /** Intermediate tiles already committed (flag endpoints excluded). */
  path: Coordinates[];
}

const tool = ref<Tool>({ kind: "inspect" });
const selectedBuildingId = ref<string | null>(null);
const roadDraft = shallowRef<RoadDraft | null>(null);

function setTool(next: Tool): void {
  tool.value = next;
  // Switching away from road drawing abandons any half-drawn road.
  if (next.kind !== "road") roadDraft.value = null;
  if (next.kind !== "inspect") selectedBuildingId.value = null;
}

function resetTool(): void {
  setTool({ kind: "inspect" });
}

export function useGameUi() {
  return { tool, selectedBuildingId, roadDraft, setTool, resetTool };
}
