/**
 * Procedural 2.5-D sprite painters for the game canvas.
 *
 * The world is rendered top-down on a flat hex grid, but every "standing" object
 * (buildings, trees, settlers, flags) is drawn *upright* — its base sits on the
 * tile centre and it rises toward −y — so the scene reads with a gentle Settlers
 * II sense of depth once the caller paints objects back-to-front (sorted by base
 * `y`). Every routine draws in **world space**: the caller has already applied
 * the camera transform, and `s` is the hex circum-radius ({@link HEX_SIZE}).
 *
 * Routines are pure: they only touch the passed {@link CanvasRenderingContext2D}
 * and never read game state, the camera or the DOM. Colours come from
 * {@link BUILDING_STYLE}; shapes are chosen by {@link BuildingStyle.archetype}.
 */
import { BUILDING_STYLE } from "~/lib/palette";
import type { BuildingName } from "~/types/game";

/* ------------------------------- Utilities -------------------------------- */

/** Deterministic pseudo-random in [0, 1) from integer coords (+ salt). Stable
 *  per tile so trees, rock scatter and timber framing never shimmer. */
export function hashRand(x: number, y: number, salt = 0): number {
  let h = (Math.imul(x | 0, 374761393) ^ Math.imul(y | 0, 668265263) ^ Math.imul(salt | 0, 2246822519)) >>> 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177) >>> 0;
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
}

/** Traces a closed polygon path from a flat `[x, y, x, y, …]` list. */
function polyPath(ctx: CanvasRenderingContext2D, pts: number[]): void {
  ctx.beginPath();
  ctx.moveTo(pts[0]!, pts[1]!);
  for (let i = 2; i < pts.length; i += 2) ctx.lineTo(pts[i]!, pts[i + 1]!);
  ctx.closePath();
}

/** Fills (and optionally strokes) a polygon in one call. */
function poly(
  ctx: CanvasRenderingContext2D,
  pts: number[],
  fill: string,
  stroke?: string,
  lw = 0.5,
): void {
  polyPath(ctx, pts);
  ctx.fillStyle = fill;
  ctx.fill();
  if (stroke) {
    ctx.strokeStyle = stroke;
    ctx.lineWidth = lw;
    ctx.stroke();
  }
}

/** A soft contact shadow ellipse under an object's footprint. */
function groundShadow(ctx: CanvasRenderingContext2D, cx: number, cy: number, rx: number, ry: number): void {
  ctx.fillStyle = "rgba(20, 30, 18, 0.26)";
  ctx.beginPath();
  ctx.ellipse(cx + rx * 0.12, cy + ry * 0.35, rx, ry, 0, 0, Math.PI * 2);
  ctx.fill();
}

/** Lightens or darkens a `#rrggbb` colour by `amt` ∈ [−1, 1]. */
export function shade(hex: string, amt: number): string {
  const n = parseInt(hex.slice(1), 16);
  const t = amt < 0 ? 0 : 255;
  const p = Math.abs(amt);
  const r = Math.round(((n >> 16) & 255) * (1 - p) + t * p);
  const g = Math.round(((n >> 8) & 255) * (1 - p) + t * p);
  const b = Math.round((n & 255) * (1 - p) + t * p);
  return `#${((1 << 24) | (r << 16) | (g << 8) | b).toString(16).slice(1)}`;
}

const OUTLINE = "rgba(40, 28, 18, 0.55)";

/* --------------------------------- Nature --------------------------------- */

/**
 * A single tree: trunk plus two or three stacked canopy tufts. The `seed` jitters
 * the size and hue so a cluster never looks tiled; `broad` draws a rounded
 * deciduous crown instead of a conifer.
 */
export function drawTree(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, seed: number, broad = false): void {
  const h = s * (0.9 + seed * 0.5);
  const w = s * (0.5 + seed * 0.18);
  groundShadow(ctx, cx, cy, w * 0.7, s * 0.16);
  // Trunk.
  ctx.fillStyle = "#5a3d22";
  ctx.fillRect(cx - s * 0.06, cy - h * 0.45, s * 0.12, h * 0.45);
  const green = broad ? "#4f9a37" : "#3f7d2c";
  if (broad) {
    ctx.fillStyle = shade(green, -0.08 + seed * 0.12);
    ctx.beginPath();
    ctx.arc(cx, cy - h * 0.62, w, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = shade(green, 0.16);
    ctx.beginPath();
    ctx.arc(cx - w * 0.3, cy - h * 0.78, w * 0.55, 0, Math.PI * 2);
    ctx.fill();
  } else {
    // Conifer: three layered triangles, lighter toward the top.
    for (let i = 0; i < 3; i++) {
      const ty = cy - h * (0.35 + i * 0.22);
      const tw = w * (1 - i * 0.24);
      poly(ctx, [cx - tw, ty, cx + tw, ty, cx, ty - h * 0.34], shade(green, -0.05 + i * 0.12));
    }
  }
}

/** A cluster of angular rocks, optionally tinted for an ore seam (coal/iron/gold). */
export function drawRocks(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, seed: number, tint?: string): void {
  groundShadow(ctx, cx, cy, s * 0.55, s * 0.16);
  const n = 2 + Math.floor(seed * 2);
  for (let i = 0; i < n; i++) {
    const r = hashRand(cx, cy, i + 1);
    const ox = (r - 0.5) * s * 0.7;
    const oy = (hashRand(cx, cy, i + 5) - 0.5) * s * 0.3;
    const rw = s * (0.26 + r * 0.18);
    const base = tint ?? "#9a9488";
    poly(
      ctx,
      [cx + ox - rw, cy + oy, cx + ox - rw * 0.4, cy + oy - rw, cx + ox + rw * 0.6, cy + oy - rw * 0.8, cx + ox + rw, cy + oy],
      shade(base, -0.05 + r * 0.1),
      OUTLINE,
      0.4,
    );
    if (tint) {
      // A little mineral glint.
      ctx.fillStyle = shade(tint, 0.4);
      ctx.beginPath();
      ctx.arc(cx + ox, cy + oy - rw * 0.4, rw * 0.16, 0, Math.PI * 2);
      ctx.fill();
    }
  }
}

/** A tuft of ripening wheat: a few golden stalks fanning out. */
export function drawWheat(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, seed: number): void {
  ctx.strokeStyle = "#d8a72a";
  ctx.lineWidth = 0.5;
  for (let i = 0; i < 5; i++) {
    const a = (-0.4 + i * 0.2) + (seed - 0.5) * 0.3;
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.sin(a) * s * 0.5, cy - s * 0.55);
    ctx.stroke();
  }
}

/** Faint expanding ripple marking a fishing spot. */
export function drawFishRipple(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, phase: number): void {
  ctx.strokeStyle = "rgba(220, 240, 255, 0.35)";
  ctx.lineWidth = 0.4;
  const r = (s * 0.2) + ((phase * 0.4) % 1) * s * 0.4;
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.stroke();
}

/* --------------------------------- Markers -------------------------------- */

/**
 * A flag on a pole with a player-coloured pennant that flutters off `phase`, and
 * up to six little crates for the goods queued at the flag.
 */
export function drawFlag(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  s: number,
  color: string,
  resourceCount: number,
  phase: number,
): void {
  groundShadow(ctx, cx, cy, s * 0.18, s * 0.08);
  const top = cy - s * 0.95;
  ctx.strokeStyle = "#3a2e22";
  ctx.lineWidth = 0.7;
  ctx.beginPath();
  ctx.moveTo(cx, cy);
  ctx.lineTo(cx, top);
  ctx.stroke();
  const wave = Math.sin(phase * 0.6 + cx) * s * 0.06;
  poly(
    ctx,
    [cx, top, cx + s * 0.5, top + s * 0.08 + wave, cx, top + s * 0.32],
    color,
    shade(color, -0.3),
    0.4,
  );
  for (let i = 0; i < Math.min(resourceCount, 6); i++) {
    ctx.fillStyle = "#caa15a";
    ctx.strokeStyle = OUTLINE;
    ctx.lineWidth = 0.3;
    const bx = cx - s * 0.5 + (i % 3) * s * 0.34;
    const by = cy - s * 0.05 + Math.floor(i / 3) * s * 0.3;
    ctx.fillRect(bx, by, s * 0.26, s * 0.26);
    ctx.strokeRect(bx, by, s * 0.26, s * 0.26);
  }
}

/** A surveyor's signpost: a panel on a post, tinted by the ore found (or grey
 *  with a cross when the geologist found nothing). */
export function drawSign(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, oreColor: string | null): void {
  ctx.fillStyle = "#5a4326";
  ctx.fillRect(cx - s * 0.04, cy - s * 0.5, s * 0.08, s * 0.5);
  const w = s * 0.42;
  if (oreColor) {
    poly(ctx, [cx - w, cy - s * 0.7, cx + w, cy - s * 0.7, cx + w, cy - s * 0.42, cx - w, cy - s * 0.42], oreColor, OUTLINE, 0.4);
  } else {
    poly(ctx, [cx - w, cy - s * 0.7, cx + w, cy - s * 0.7, cx + w, cy - s * 0.42, cx - w, cy - s * 0.42], "#7a7468", OUTLINE, 0.4);
    ctx.strokeStyle = "#3a3a3a";
    ctx.lineWidth = 0.5;
    ctx.beginPath();
    ctx.moveTo(cx - w * 0.5, cy - s * 0.65);
    ctx.lineTo(cx + w * 0.5, cy - s * 0.47);
    ctx.moveTo(cx + w * 0.5, cy - s * 0.65);
    ctx.lineTo(cx - w * 0.5, cy - s * 0.47);
    ctx.stroke();
  }
}

/* --------------------------------- Settlers ------------------------------- */

/** Kinds of mobile unit, each drawn slightly differently. */
export type SettlerKind = "carrier" | "worker" | "soldier" | "donkey";

/**
 * A tiny figure: a coloured body and a pale head. Soldiers carry the player
 * colour and a darker outline; donkeys are drawn as a small four-legged pack
 * animal. Kept deliberately minimal so dozens read clearly at map scale.
 */
export function drawSettler(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  s: number,
  color: string,
  kind: SettlerKind,
): void {
  groundShadow(ctx, cx, cy, s * 0.18, s * 0.07);
  if (kind === "donkey") {
    ctx.fillStyle = "#9c7b4a";
    ctx.fillRect(cx - s * 0.26, cy - s * 0.32, s * 0.52, s * 0.22);
    ctx.fillRect(cx + s * 0.18, cy - s * 0.42, s * 0.16, s * 0.16); // head
    ctx.fillStyle = "#6e5230";
    ctx.fillRect(cx - s * 0.2, cy - s * 0.12, s * 0.06, s * 0.12);
    ctx.fillRect(cx + s * 0.12, cy - s * 0.12, s * 0.06, s * 0.12);
    return;
  }
  const body = kind === "soldier" ? color : kind === "worker" ? "#5a8f55" : "#c9b27e";
  // Torso.
  ctx.fillStyle = body;
  if (kind === "soldier") {
    ctx.strokeStyle = "#1b1b1b";
    ctx.lineWidth = 0.3;
  }
  ctx.beginPath();
  ctx.moveTo(cx, cy - s * 0.5);
  ctx.lineTo(cx + s * 0.16, cy - s * 0.08);
  ctx.lineTo(cx - s * 0.16, cy - s * 0.08);
  ctx.closePath();
  ctx.fill();
  if (kind === "soldier") ctx.stroke();
  // Head.
  ctx.fillStyle = "#f1d9b5";
  ctx.beginPath();
  ctx.arc(cx, cy - s * 0.56, s * 0.13, 0, Math.PI * 2);
  ctx.fill();
}

/** A small ship: a coloured hull, a mast and a sail that leans with `phase`. */
export function drawShip(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, color: string, phase: number): void {
  groundShadow(ctx, cx, cy, s * 0.7, s * 0.18);
  poly(ctx, [cx - s * 0.8, cy, cx + s * 0.8, cy, cx + s * 0.55, cy + s * 0.3, cx - s * 0.55, cy + s * 0.3], "#7a4f2a", OUTLINE, 0.5);
  ctx.strokeStyle = "#3a2a18";
  ctx.lineWidth = 0.6;
  ctx.beginPath();
  ctx.moveTo(cx, cy);
  ctx.lineTo(cx, cy - s * 0.9);
  ctx.stroke();
  const lean = Math.sin(phase * 0.3) * s * 0.08;
  poly(ctx, [cx, cy - s * 0.85, cx + s * 0.5 + lean, cy - s * 0.5, cx, cy - s * 0.15], shade(color, 0.45), color, 0.4);
}

/* -------------------------------- Buildings ------------------------------- */

/** Optional rendering flags for {@link drawBuilding}. */
export interface BuildingSpriteOpts {
  color: string; // owner colour, used for trim, banners and the roof flag
  selected?: boolean;
  /** Animation phase (the server tick); drives smoke and turning sails. */
  phase?: number;
}

/**
 * Draws a finished building of the given type at tile centre `(cx, cy)`. The
 * shape is selected by the building's {@link BuildingStyle.archetype}; colours
 * come from the same table.
 */
export function drawBuilding(ctx: CanvasRenderingContext2D, name: BuildingName, cx: number, cy: number, s: number, opts: BuildingSpriteOpts): void {
  const style = BUILDING_STYLE[name];
  const phase = opts.phase ?? 0;
  switch (style.archetype) {
    case "hut": basicHouse(ctx, cx, cy, s, style.wall, style.roof, 1.25, 0.8); break;
    case "house": basicHouse(ctx, cx, cy, s, style.wall, style.roof, 1.6, 1.0); break;
    case "farm": farm(ctx, cx, cy, s, style.wall, style.roof); break;
    case "industry": industry(ctx, cx, cy, s, style.wall, style.roof, style.accent ?? "#d8d8d8", phase); break;
    case "mill": mill(ctx, cx, cy, s, style.wall, style.roof, phase); break;
    case "store": store(ctx, cx, cy, s, style.wall, style.roof); break;
    case "keep": headquarters(ctx, cx, cy, s, style.wall, style.roof, style.accent ?? "#e8c34d", opts.color); break;
    case "tower": tower(ctx, cx, cy, s, style.wall, style.roof, opts.color, style.tier ?? 0, phase); break;
    case "well": well(ctx, cx, cy, s, style.wall, style.roof); break;
    case "mine": mine(ctx, cx, cy, s, style.wall); break;
    case "harbor": harbor(ctx, cx, cy, s, style.wall, style.roof); break;
    case "shipyard": shipyard(ctx, cx, cy, s, style.wall, style.roof); break;
    case "catapult": catapult(ctx, cx, cy, s, opts.phase ?? 0); break;
  }
  if (opts.selected) selectionRing(ctx, cx, cy, s);
}

/** A pulsing white ring marking the selected building. */
function selectionRing(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number): void {
  ctx.strokeStyle = "rgba(255,255,255,0.95)";
  ctx.lineWidth = 0.8;
  ctx.beginPath();
  ctx.ellipse(cx, cy + s * 0.1, s * 0.95, s * 0.4, 0, 0, Math.PI * 2);
  ctx.stroke();
}

/**
 * The workhorse house: a contact shadow, a right-hand depth face, a front wall
 * with timber framing and a door, and an overhanging pitched roof. `wMul`/`hMul`
 * scale the footprint and wall height relative to `s`.
 */
function basicHouse(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string, wMul: number, hMul: number): void {
  const w = s * wMul;
  const wallH = s * hMul;
  const roofH = s * (0.55 + hMul * 0.3);
  const top = cy - wallH;
  groundShadow(ctx, cx, cy, w * 0.62, s * 0.2);
  // Right depth face for a hint of 3-D.
  poly(ctx, [cx + w / 2, cy, cx + w / 2 + s * 0.22, cy - s * 0.12, cx + w / 2 + s * 0.22, top - s * 0.1, cx + w / 2, top], shade(wall, -0.22), OUTLINE, 0.4);
  // Front wall.
  poly(ctx, [cx - w / 2, cy, cx + w / 2, cy, cx + w / 2, top, cx - w / 2, top], wall, OUTLINE, 0.5);
  // Timber framing.
  ctx.strokeStyle = "rgba(90, 60, 35, 0.5)";
  ctx.lineWidth = 0.35;
  ctx.beginPath();
  ctx.moveTo(cx - w * 0.5, cy - wallH * 0.5);
  ctx.lineTo(cx + w * 0.5, cy - wallH * 0.5);
  ctx.moveTo(cx, cy);
  ctx.lineTo(cx, top);
  ctx.stroke();
  // Door.
  ctx.fillStyle = shade(roof, -0.25);
  ctx.fillRect(cx - s * 0.16, cy - wallH * 0.6, s * 0.32, wallH * 0.6);
  // Roof: overhanging trapezoid with a lit ridge.
  const over = s * 0.22;
  poly(ctx, [cx - w / 2 - over, top, cx + w / 2 + over, top, cx + w * 0.28, top - roofH, cx - w * 0.28, top - roofH], roof, shade(roof, -0.35), 0.5);
  poly(ctx, [cx - w * 0.28, top - roofH, cx + w * 0.28, top - roofH, cx + w * 0.2, top - roofH + s * 0.12, cx - w * 0.2, top - roofH + s * 0.12], shade(roof, 0.22));
}

/** A house beside a small fenced crop patch. */
function farm(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string): void {
  // Field patch behind/left.
  poly(ctx, [cx - s * 1.5, cy + s * 0.1, cx - s * 0.2, cy + s * 0.1, cx - s * 0.4, cy + s * 0.6, cx - s * 1.7, cy + s * 0.6], "#8a6a2a", "#6b4a1c", 0.4);
  ctx.strokeStyle = "#a9842a";
  ctx.lineWidth = 0.3;
  for (let i = 1; i < 4; i++) {
    ctx.beginPath();
    ctx.moveTo(cx - s * 1.6 + i * s * 0.35, cy + s * 0.12);
    ctx.lineTo(cx - s * 1.7 + i * s * 0.35, cy + s * 0.58);
    ctx.stroke();
  }
  basicHouse(ctx, cx + s * 0.25, cy, s, wall, roof, 1.4, 0.95);
}

/** A workshop with a stone chimney that emits a drifting puff of smoke. */
function industry(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string, accent: string, phase: number): void {
  basicHouse(ctx, cx, cy, s, wall, roof, 1.7, 1.05);
  const chx = cx + s * 0.55;
  const top = cy - s * 1.05;
  ctx.fillStyle = "#6b5a48";
  ctx.fillRect(chx, top - s * 0.5, s * 0.26, s * 0.7);
  // Ember glow at the chimney mouth for forges / the mint.
  if (accent.startsWith("#ff") || accent === "#ffd95a") {
    ctx.fillStyle = accent;
    ctx.beginPath();
    ctx.arc(chx + s * 0.13, top - s * 0.5, s * 0.12, 0, Math.PI * 2);
    ctx.fill();
  }
  // Smoke: three rising puffs fading out, phased by the tick.
  for (let i = 0; i < 3; i++) {
    const t = ((phase * 0.25 + i * 0.33) % 1);
    ctx.fillStyle = `rgba(210, 210, 205, ${0.35 * (1 - t)})`;
    ctx.beginPath();
    ctx.arc(chx + s * 0.13 + Math.sin(t * 6) * s * 0.12, top - s * 0.5 - t * s * 1.1, s * (0.1 + t * 0.18), 0, Math.PI * 2);
    ctx.fill();
  }
}

/** A windmill: a tapered body, a cap and four sails turning with `phase`. */
function mill(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string, phase: number): void {
  groundShadow(ctx, cx, cy, s * 0.7, s * 0.2);
  const bodyH = s * 1.5;
  const top = cy - bodyH;
  poly(ctx, [cx - s * 0.6, cy, cx + s * 0.6, cy, cx + s * 0.4, top, cx - s * 0.4, top], wall, OUTLINE, 0.5);
  // Cap.
  poly(ctx, [cx - s * 0.5, top, cx + s * 0.5, top, cx, top - s * 0.5], roof, shade(roof, -0.3), 0.5);
  // Sails.
  const hub = { x: cx, y: top - s * 0.05 };
  ctx.save();
  ctx.translate(hub.x, hub.y);
  ctx.rotate(phase * 0.18);
  for (let i = 0; i < 4; i++) {
    ctx.rotate(Math.PI / 2);
    ctx.fillStyle = "#efe6cf";
    ctx.strokeStyle = "#5a3d22";
    ctx.lineWidth = 0.4;
    ctx.fillRect(0, -s * 0.06, s * 1.05, s * 0.18);
    ctx.strokeRect(0, -s * 0.06, s * 1.05, s * 0.18);
  }
  ctx.restore();
  ctx.fillStyle = "#3a2a18";
  ctx.beginPath();
  ctx.arc(hub.x, hub.y, s * 0.1, 0, Math.PI * 2);
  ctx.fill();
}

/** A long, low storehouse with a shallow roof and big double doors. */
function store(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string): void {
  const w = s * 2.1;
  const wallH = s * 0.95;
  const top = cy - wallH;
  groundShadow(ctx, cx, cy, w * 0.58, s * 0.24);
  poly(ctx, [cx + w / 2, cy, cx + w / 2 + s * 0.26, cy - s * 0.14, cx + w / 2 + s * 0.26, top - s * 0.1, cx + w / 2, top], shade(wall, -0.22), OUTLINE, 0.4);
  poly(ctx, [cx - w / 2, cy, cx + w / 2, cy, cx + w / 2, top, cx - w / 2, top], wall, OUTLINE, 0.5);
  const over = s * 0.26;
  poly(ctx, [cx - w / 2 - over, top, cx + w / 2 + over, top, cx + w / 2 - s * 0.2, top - s * 0.55, cx - w / 2 + s * 0.2, top - s * 0.55], roof, shade(roof, -0.3), 0.5);
  ctx.fillStyle = shade(roof, -0.2);
  ctx.fillRect(cx - s * 0.4, cy - wallH * 0.75, s * 0.8, wallH * 0.75);
  ctx.strokeStyle = OUTLINE;
  ctx.lineWidth = 0.4;
  ctx.beginPath();
  ctx.moveTo(cx, cy - wallH * 0.75);
  ctx.lineTo(cx, cy);
  ctx.stroke();
}

/** The headquarters: a broad stone keep crowned by the iconic golden dome and a
 *  player-coloured banner. The largest building on the map. */
function headquarters(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string, dome: string, owner: string): void {
  const w = s * 2.4;
  const wallH = s * 1.7;
  const top = cy - wallH;
  groundShadow(ctx, cx, cy, w * 0.6, s * 0.3);
  // Side wings (lower roofs) left and right.
  for (const dir of [-1, 1]) {
    const wx = cx + dir * w * 0.42;
    poly(ctx, [wx - s * 0.5, cy, wx + s * 0.5, cy, wx + s * 0.5, cy - s * 0.95, wx - s * 0.5, cy - s * 0.95], shade(wall, -0.05), OUTLINE, 0.4);
    poly(ctx, [wx - s * 0.66, cy - s * 0.95, wx + s * 0.66, cy - s * 0.95, wx, cy - s * 1.4], roof, shade(roof, -0.3), 0.4);
  }
  // Central tower.
  poly(ctx, [cx - w * 0.32, cy, cx + w * 0.32, cy, cx + w * 0.32, top, cx - w * 0.32, top], wall, OUTLINE, 0.6);
  // Windows.
  ctx.fillStyle = "rgba(40, 55, 80, 0.7)";
  for (let r = 0; r < 2; r++)
    for (let c = -1; c <= 1; c++)
      ctx.fillRect(cx + c * s * 0.5 - s * 0.12, top + s * 0.35 + r * s * 0.55, s * 0.24, s * 0.34);
  // Drum + golden dome.
  const drumY = top;
  poly(ctx, [cx - w * 0.26, drumY, cx + w * 0.26, drumY, cx + w * 0.2, drumY - s * 0.3, cx - w * 0.2, drumY - s * 0.3], shade(wall, 0.1), OUTLINE, 0.4);
  const domeBase = drumY - s * 0.3;
  // Onion dome: the upper half of an ellipse (angles π → 2π in canvas' y-down
  // space bulge *upward*), topped with a small spire.
  ctx.fillStyle = dome;
  ctx.beginPath();
  ctx.ellipse(cx, domeBase, w * 0.24, s * 1.05, 0, Math.PI, Math.PI * 2);
  ctx.closePath();
  ctx.fill();
  ctx.strokeStyle = shade(dome, -0.3);
  ctx.lineWidth = 0.4;
  ctx.stroke();
  // Dome highlight.
  ctx.fillStyle = shade(dome, 0.4);
  ctx.beginPath();
  ctx.ellipse(cx - w * 0.07, domeBase - s * 0.15, w * 0.07, s * 0.5, 0, Math.PI, Math.PI * 2);
  ctx.fill();
  // Finial + banner atop the dome.
  const fy = domeBase - s * 1.05;
  ctx.strokeStyle = "#3a2e22";
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  ctx.moveTo(cx, fy);
  ctx.lineTo(cx, fy - s * 0.5);
  ctx.stroke();
  poly(ctx, [cx, fy - s * 0.5, cx + s * 0.45, fy - s * 0.38, cx, fy - s * 0.26], owner, shade(owner, -0.3), 0.3);
}

/** A crenellated military building; `tier` 0–4 grows the keep from a barracks to
 *  a fortress. Flies the owner's banner. */
function tower(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string, owner: string, tier: number, phase: number): void {
  const w = s * (1.1 + tier * 0.28);
  const h = s * (1.1 + tier * 0.45);
  const top = cy - h;
  groundShadow(ctx, cx, cy, w * 0.62, s * 0.22);
  // Depth face.
  poly(ctx, [cx + w / 2, cy, cx + w / 2 + s * 0.24, cy - s * 0.12, cx + w / 2 + s * 0.24, top + s * 0.1, cx + w / 2, top + s * 0.2], shade(wall, -0.22), OUTLINE, 0.4);
  // Body.
  poly(ctx, [cx - w / 2, cy, cx + w / 2, cy, cx + w / 2, top + s * 0.2, cx - w / 2, top + s * 0.2], wall, OUTLINE, 0.6);
  // Stone courses.
  ctx.strokeStyle = "rgba(80, 74, 64, 0.4)";
  ctx.lineWidth = 0.3;
  for (let i = 1; i < 3 + tier; i++) {
    const yy = cy - (h - s * 0.2) * (i / (3 + tier));
    ctx.beginPath();
    ctx.moveTo(cx - w / 2, yy);
    ctx.lineTo(cx + w / 2, yy);
    ctx.stroke();
  }
  // Crenellations.
  const merlons = 3 + tier;
  const mw = w / (merlons * 2 - 1);
  for (let i = 0; i < merlons; i++) {
    const mx = cx - w / 2 + i * mw * 2;
    poly(ctx, [mx, top + s * 0.2, mx + mw, top + s * 0.2, mx + mw, top, mx, top], shade(wall, -0.08), OUTLINE, 0.4);
  }
  // Door.
  ctx.fillStyle = shade(roof, -0.2);
  ctx.fillRect(cx - s * 0.2, cy - h * 0.45, s * 0.4, h * 0.45);
  // Banner on a corner pole.
  const px = cx + w / 2;
  const py = top + s * 0.2;
  ctx.strokeStyle = "#3a2e22";
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  ctx.moveTo(px, py);
  ctx.lineTo(px, py - s * 0.7);
  ctx.stroke();
  const wave = Math.sin(phase * 0.5 + cx) * s * 0.05;
  poly(ctx, [px, py - s * 0.7, px + s * 0.5, py - s * 0.6 + wave, px, py - s * 0.44], owner, shade(owner, -0.3), 0.3);
}

/** A roofed stone well: a circular kerb, two posts and a little gable roof. */
function well(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string): void {
  groundShadow(ctx, cx, cy, s * 0.5, s * 0.16);
  ctx.fillStyle = wall;
  ctx.strokeStyle = OUTLINE;
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  ctx.ellipse(cx, cy - s * 0.1, s * 0.45, s * 0.2, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.stroke();
  ctx.fillStyle = "#3a4a55";
  ctx.beginPath();
  ctx.ellipse(cx, cy - s * 0.12, s * 0.3, s * 0.13, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.strokeStyle = "#5a3d22";
  ctx.lineWidth = 0.6;
  ctx.beginPath();
  ctx.moveTo(cx - s * 0.4, cy - s * 0.1);
  ctx.lineTo(cx - s * 0.4, cy - s * 0.9);
  ctx.moveTo(cx + s * 0.4, cy - s * 0.1);
  ctx.lineTo(cx + s * 0.4, cy - s * 0.9);
  ctx.stroke();
  poly(ctx, [cx - s * 0.6, cy - s * 0.85, cx + s * 0.6, cy - s * 0.85, cx, cy - s * 1.2], roof, shade(roof, -0.3), 0.5);
}

/** A mine: a timber-framed cave mouth set into a rocky mound (mines sit on
 *  mountains, so the surrounding rock grounds it visually). */
function mine(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string): void {
  groundShadow(ctx, cx, cy, s * 0.7, s * 0.2);
  poly(ctx, [cx - s * 0.8, cy, cx - s * 0.5, cy - s * 0.8, cx + s * 0.1, cy - s * 0.95, cx + s * 0.7, cy - s * 0.6, cx + s * 0.85, cy], shade(wall, -0.05), OUTLINE, 0.5);
  // Entrance.
  ctx.fillStyle = "#241c14";
  ctx.beginPath();
  ctx.moveTo(cx - s * 0.3, cy);
  ctx.lineTo(cx - s * 0.3, cy - s * 0.4);
  ctx.lineTo(cx, cy - s * 0.6);
  ctx.lineTo(cx + s * 0.3, cy - s * 0.4);
  ctx.lineTo(cx + s * 0.3, cy);
  ctx.closePath();
  ctx.fill();
  // Timber frame around the mouth.
  ctx.strokeStyle = "#5a3d22";
  ctx.lineWidth = 0.6;
  ctx.stroke();
  // A support beam over the entrance.
  ctx.fillStyle = "#6b4a2c";
  ctx.fillRect(cx - s * 0.42, cy - s * 0.62, s * 0.84, s * 0.12);
}

/** A coastal storehouse with a timber dock reaching out toward open water. */
function harbor(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string): void {
  // Dock planks (drawn first, on the ground, extending down/south to the sea).
  ctx.fillStyle = "#7a5636";
  ctx.strokeStyle = "#4a3420";
  ctx.lineWidth = 0.3;
  for (let i = 0; i < 4; i++) {
    ctx.fillRect(cx - s * 0.7, cy + s * 0.1 + i * s * 0.22, s * 1.4, s * 0.16);
    ctx.strokeRect(cx - s * 0.7, cy + s * 0.1 + i * s * 0.22, s * 1.4, s * 0.16);
  }
  store(ctx, cx, cy - s * 0.1, s * 0.92, wall, roof);
  // A pair of mooring posts.
  ctx.fillStyle = "#4a3420";
  ctx.fillRect(cx - s * 0.78, cy + s * 0.85, s * 0.12, s * 0.3);
  ctx.fillRect(cx + s * 0.66, cy + s * 0.85, s * 0.12, s * 0.3);
}

/** A shipyard: an open-fronted hall with a half-built hull on a slipway. */
function shipyard(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, wall: string, roof: string): void {
  groundShadow(ctx, cx, cy, s * 0.9, s * 0.24);
  const w = s * 1.9;
  const top = cy - s * 1.1;
  // Back wall + posts (open front).
  poly(ctx, [cx - w / 2, cy, cx + w / 2, cy, cx + w / 2, top, cx - w / 2, top], shade(wall, -0.1), OUTLINE, 0.4);
  ctx.fillStyle = "#6b4a2c";
  ctx.fillRect(cx - w / 2, top, s * 0.16, s * 1.1);
  ctx.fillRect(cx + w / 2 - s * 0.16, top, s * 0.16, s * 1.1);
  // Roof.
  poly(ctx, [cx - w / 2 - s * 0.2, top, cx + w / 2 + s * 0.2, top, cx + w * 0.3, top - s * 0.5, cx - w * 0.3, top - s * 0.5], roof, shade(roof, -0.3), 0.5);
  // Hull ribs on a slipway.
  ctx.strokeStyle = "#8a5a34";
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  ctx.ellipse(cx, cy - s * 0.2, s * 0.6, s * 0.3, 0, Math.PI, 0);
  ctx.stroke();
  for (let i = -2; i <= 2; i++) {
    ctx.beginPath();
    ctx.moveTo(cx + i * s * 0.22, cy - s * 0.2);
    ctx.lineTo(cx + i * s * 0.26, cy - s * 0.5);
    ctx.stroke();
  }
}

/** A siege catapult: a timber frame and a throwing arm cocked back. */
function catapult(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, phase: number): void {
  groundShadow(ctx, cx, cy, s * 0.7, s * 0.18);
  ctx.strokeStyle = "#6b4a2c";
  ctx.lineWidth = 0.8;
  // Base frame.
  poly(ctx, [cx - s * 0.7, cy, cx + s * 0.7, cy, cx + s * 0.5, cy - s * 0.5, cx - s * 0.5, cy - s * 0.5], "#8a5e36", "#4a3420", 0.5);
  // Throwing arm, recoiling slightly with the tick.
  const a = -Math.PI * 0.35 + Math.sin(phase * 0.2) * 0.12;
  const pivx = cx;
  const pivy = cy - s * 0.45;
  ctx.beginPath();
  ctx.moveTo(pivx, pivy);
  ctx.lineTo(pivx + Math.cos(a) * s * 1.1, pivy + Math.sin(a) * s * 1.1);
  ctx.stroke();
  ctx.fillStyle = "#3a2a18";
  ctx.beginPath();
  ctx.arc(pivx + Math.cos(a) * s * 1.1, pivy + Math.sin(a) * s * 1.1, s * 0.16, 0, Math.PI * 2);
  ctx.fill();
}

/**
 * A construction site: a stone groundwork pad with a rising timber skeleton.
 * `progress` ∈ [0, 1] fills the frame from the ground up, matching the engine's
 * groundwork-then-masonry sequence.
 */
export function drawConstructionSite(ctx: CanvasRenderingContext2D, name: BuildingName, cx: number, cy: number, s: number, progress: number, color: string): void {
  const style = BUILDING_STYLE[name];
  const big = style.archetype === "keep" || style.archetype === "tower" || style.archetype === "store";
  const w = s * (big ? 1.9 : 1.4);
  const h = s * (big ? 1.4 : 1.0);
  groundShadow(ctx, cx, cy, w * 0.6, s * 0.2);
  // Groundwork pad.
  poly(ctx, [cx - w / 2, cy, cx + w / 2, cy, cx + w / 2 - s * 0.1, cy - s * 0.2, cx - w / 2 + s * 0.1, cy - s * 0.2], "#b8ac96", "#6b5a40", 0.4);
  // Corner posts and beams, revealed up to `progress`.
  const built = Math.max(0.05, progress) * h;
  ctx.strokeStyle = "#8a5a34";
  ctx.lineWidth = 0.6;
  ctx.beginPath();
  ctx.moveTo(cx - w / 2 + s * 0.1, cy - s * 0.2);
  ctx.lineTo(cx - w / 2 + s * 0.1, cy - s * 0.2 - built);
  ctx.moveTo(cx + w / 2 - s * 0.1, cy - s * 0.2);
  ctx.lineTo(cx + w / 2 - s * 0.1, cy - s * 0.2 - built);
  ctx.stroke();
  // Horizontal courses already laid.
  const courses = Math.floor(progress * 4);
  ctx.fillStyle = "rgba(210, 190, 150, 0.7)";
  for (let i = 0; i < courses; i++) {
    ctx.fillRect(cx - w / 2 + s * 0.1, cy - s * 0.2 - (i + 1) * (h / 4) * progress, w - s * 0.2, (h / 4) * progress * 0.7);
  }
  // A diagonal scaffold brace.
  ctx.strokeStyle = "#a9753f";
  ctx.lineWidth = 0.4;
  ctx.beginPath();
  ctx.moveTo(cx - w / 2 + s * 0.1, cy - s * 0.2);
  ctx.lineTo(cx + w / 2 - s * 0.1, cy - s * 0.2 - built);
  ctx.stroke();
  // Owner's pennant on the corner so sites read as yours at a glance.
  drawFlag(ctx, cx + w / 2, cy, s * 0.7, color, 0, 0);
}
