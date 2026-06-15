/**
 * Keyboard shortcuts for the in-game view, in the spirit of the classic
 * Settlers control scheme: number keys arm the general tools, and Escape backs
 * out of whatever is currently active (a half-drawn road, an armed tool, or the
 * selection panel).
 *
 * Installed by the game page for its lifetime via {@link useHotkeys}; the
 * listener is attached to `window` and removed automatically on unmount.
 */
import { onBeforeUnmount, onMounted } from "vue";
import type { ToolKind } from "~/composables/useGameUi";

/** Digit key → general tool, mirroring the order shown in the build palette. */
const DIGIT_TOOLS: Record<string, ToolKind> = {
  "1": "inspect",
  "2": "flag",
  "3": "road",
  "4": "geologist",
  "5": "destroy",
  "6": "attack",
};

export function useHotkeys(): void {
  const { tool, selectedBuildingId, roadDraft, setTool, resetTool } = useGameUi();

  function onKeyDown(e: KeyboardEvent): void {
    // Never hijack typing in inputs (e.g. the save-name prompt).
    const target = e.target as HTMLElement | null;
    if (target && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName))) {
      return;
    }

    if (e.key === "Escape") {
      // Peel back one layer of state per press: road draft → tool → selection.
      if (roadDraft.value) roadDraft.value = null;
      else if (tool.value.kind !== "inspect") resetTool();
      else selectedBuildingId.value = null;
      return;
    }

    const kind = DIGIT_TOOLS[e.key];
    if (kind) {
      setTool({ kind });
      e.preventDefault();
    }
  }

  onMounted(() => window.addEventListener("keydown", onKeyDown));
  onBeforeUnmount(() => window.removeEventListener("keydown", onKeyDown));
}
