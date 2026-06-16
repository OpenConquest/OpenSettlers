/**
 * TypeScript mirror of the backend's wire protocol.
 *
 * The server speaks two channels:
 *  - REST (`fr.opensettlers.controller`) to create / list / stop / save / load games.
 *  - WebSocket (`/game/{gameId}`) carrying {@link ServerMessage}s down and
 *    {@link ClientMessage}s up.
 *
 * Coordinates are *doubled-height* hexagonal coordinates (flat-top hexes),
 * see {@link https://www.redblobgames.com/grids/hexagons/#coordinates-doubled}.
 */

/** A point on the doubled-height hex grid. `x + y` is always even for a tile. */
export interface Coordinates {
  x: number;
  y: number;
}

/* -------------------------------------------------------------------------- */
/*  Enumerations (kept as string unions to match the JSON the server emits)   */
/* -------------------------------------------------------------------------- */

export type TileType =
  | "GRASS"
  | "WATER"
  | "MOUNTAIN"
  | "FOREST"
  | "STONE"
  | "DESERT"
  | "HILLS"
  | "FIELD";

export type ResourceType =
  | "LOG"
  | "PLANK"
  | "STONE"
  | "IRON"
  | "STEEL"
  | "COAL"
  | "GOLD"
  | "COIN"
  | "WHEAT"
  | "FLOUR"
  | "BREAD"
  | "FISH"
  | "PIG"
  | "MEAT"
  | "DONKEY"
  | "BEER"
  | "SWORD"
  | "SHIELD"
  | "TOOL"
  | "WATER";

export type BuildingName =
  | "HEADQUARTERS"
  | "WAREHOUSE"
  | "WOODCUTTER"
  | "FORESTER"
  | "QUARRY"
  | "MINE"
  | "FARM"
  | "FISHING_HUT"
  | "HUNTERS_HUT"
  | "WATER_WELL"
  | "SAWMILL"
  | "MILL"
  | "BAKERY"
  | "PIG_FARM"
  | "SLAUGHTERHOUSE"
  | "DONKEY_BREEDER"
  | "FOUNDRY"
  | "ARMORY"
  | "MINT"
  | "METALWORKS"
  | "BREWERY"
  | "GUARD_HOUSE"
  | "WATCH_TOWER"
  | "CASTLE"
  | "FORTRESS"
  | "BARRACKS"
  | "CATAPULT"
  | "HARBOR"
  | "SHIPYARD";

/* -------------------------------------------------------------------------- */
/*  Server → client messages                                                  */
/* -------------------------------------------------------------------------- */

export type ServerMessage = MapMessage | StateMessage | GameOverMessage;

/** Static terrain, sent once when the connection opens. */
export interface MapMessage {
  type: "MAP";
  size: number;
  tiles: TileDto[];
}

export interface TileDto {
  x: number;
  y: number;
  tileType: TileType;
  elevation: number;
  resource: ResourceType | null;
  quantity: number | null;
}

/** Dynamic state, broadcast every tick (filtered by fog of war per player). */
export interface StateMessage {
  type: "STATE";
  tick: number;
  buildings: BuildingDto[];
  flags: FlagDto[];
  roads: RoadDto[];
  workers: WorkerDto[];
  soldiers: SoldierDto[];
  donkeys: DonkeyDto[];
  ships: ShipDto[];
  /** `[x, y, playerId]` triples for owned tiles. */
  territory: [number, number, number][];
  signs: SignDto[];
  /** Live natural-resource nodes on explored tiles (trees, ore, fish, …). */
  resources: ResourceTileDto[];
  /** `[x, y]` pairs explored by the viewer, or `null` for spectators. */
  explored: [number, number][] | null;
  /**
   * The viewer's distribution priority order per contested good (good →
   * ordered consumer building names). `null` for spectators.
   */
  distribution: Partial<Record<ResourceType, BuildingName[]>> | null;
  /** The viewer's target garrison occupation percentage (0–100). `null` for spectators. */
  militaryOccupation: number | null;
}

export interface BuildingDto {
  id: string;
  name: BuildingName | null;
  playerId: number;
  x: number;
  y: number;
  underConstruction: boolean;
  groundworkProgress: number | null;
  buildingProgress: number | null;
  productivity: number | null;
  outputQuantity: number | null;
  storedResources: Record<string, number> | null;
  garrison: number | null;
  maxGarrison: number | null;
  coins: number | null;
  /** Whether production is paused (production buildings only). */
  productionPaused: boolean | null;
  /** Whether gold-coin delivery is enabled (military buildings only). */
  coinsAllowed: boolean | null;
}

export interface FlagDto {
  id: string;
  playerId: number;
  x: number;
  y: number;
  resources: ResourceType[];
}

export interface CarrierDto {
  id: string;
  progress: number;
  state: string;
  carriedType: ResourceType | null;
}

export interface RoadDto {
  id: string;
  startFlagId: string;
  endFlagId: string;
  /** Intermediate path as `[x, y]` pairs (flag endpoints excluded). */
  path: [number, number][];
  carrier: CarrierDto | null;
  level: number;
  hasDonkey: boolean;
}

export interface WorkerDto {
  id: string;
  playerId: number;
  workerType: string | null;
  x: number;
  y: number;
  state: string | null;
}

export interface SoldierDto {
  id: string;
  playerId: number;
  x: number;
  y: number;
  health: number;
  state: string | null;
  rank: string;
}

export interface DonkeyDto {
  id: string;
  playerId: number;
  x: number;
  y: number;
  state: string | null;
}

export interface ShipDto {
  id: string;
  playerId: number;
  x: number;
  y: number;
  state: string | null;
}

export interface SignDto {
  x: number;
  y: number;
  resource: ResourceType | null;
}

/** A live natural-resource node on an explored tile, with remaining quantity. */
export interface ResourceTileDto {
  x: number;
  y: number;
  resource: ResourceType;
  quantity: number;
}

export interface GameOverMessage {
  type: "GAME_OVER";
  tick: number;
  winner: number | null;
  eliminated: number[];
}

/* -------------------------------------------------------------------------- */
/*  Client → server messages                                                  */
/* -------------------------------------------------------------------------- */

export type MessageType =
  | "BUILD_BUILDING"
  | "DESTROY_BUILDING"
  | "PLACE_FLAG"
  | "LINK_FLAGS"
  | "ATTACK_BUILDING"
  | "SEND_GEOLOGIST"
  | "SET_PRODUCTION"
  | "SET_COIN_DELIVERY"
  | "SET_DISTRIBUTION"
  | "SET_MILITARY";

/** Mirror of `fr.opensettlers.controller.GameMessage`. Unused fields stay omitted. */
export interface ClientMessage {
  type: MessageType;
  playerId: number;
  buildingName?: BuildingName;
  position?: Coordinates;
  targetId?: string;
  flagIdA?: string;
  flagIdB?: string;
  path?: Coordinates[];
  /** Toggle value for SET_PRODUCTION / SET_COIN_DELIVERY. */
  enabled?: boolean;
  /** Contested good for SET_DISTRIBUTION. */
  resourceType?: ResourceType;
  /** Ordered consumer building types for SET_DISTRIBUTION (highest first). */
  priorities?: BuildingName[];
  /** Target garrison occupation percentage for SET_MILITARY (0–100). */
  militaryOccupation?: number;
}

/* -------------------------------------------------------------------------- */
/*  REST payloads                                                             */
/* -------------------------------------------------------------------------- */

export interface CreateGameRequest {
  playerCount: number;
  aiPlayers: number;
}

export interface GameCreatedResponse {
  gameId: string;
  playerCount: number;
  websocketPath: string;
}

export interface SaveSummary {
  id: number;
  name: string;
  tick?: number;
  createdAt?: string;
}

export interface SaveResponse {
  saveId: number;
}

export interface LoadResponse {
  gameId: string;
  websocketPath: string;
}
