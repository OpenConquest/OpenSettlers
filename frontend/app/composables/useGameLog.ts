/**
 * A small client-side activity log, shown top-left in the spirit of the
 * Settlers II message list. It records the orders the player issues (build a
 * house, plant a flag, lay a road, …) so there is always feedback on the last
 * few actions. It is purely cosmetic local state — no server round-trip.
 */
import { ref } from "vue";

/** Visual tone of a log line, driving its bullet colour. */
export type LogTone = "info" | "good" | "warn" | "bad";

export interface LogEntry {
  id: number;
  text: string;
  tone: LogTone;
}

/** How many recent lines to keep on screen. */
const MAX_ENTRIES = 7;

const entries = ref<LogEntry[]>([]);
let seq = 0;

/** Appends a line to the log, trimming the oldest beyond {@link MAX_ENTRIES}. */
function log(text: string, tone: LogTone = "info"): void {
  entries.value = [...entries.value, { id: ++seq, text, tone }].slice(-MAX_ENTRIES);
}

/** Clears the log, e.g. when joining a new game. */
function clearLog(): void {
  entries.value = [];
}

export function useGameLog() {
  return { entries, log, clearLog };
}
