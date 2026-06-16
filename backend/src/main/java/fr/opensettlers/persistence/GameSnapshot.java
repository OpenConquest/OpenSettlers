package fr.opensettlers.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A serializable, self-contained snapshot of a {@link fr.opensettlers.state.GameState}.
 *
 * <p>The snapshot captures everything needed to recreate a playable game: the
 * terrain, the buildings and their stock or garrison, the road network (flags
 * and roads), and the bookkeeping of players, turns and victory. Transient
 * units in motion (carriers, walking workers, soldiers on the march, ships,
 * geologist signs) are intentionally not persisted — they are re-derived by the
 * simulation. Buildings still under construction are restored as completed.</p>
 *
 * <p>It is a plain Java bean so Jackson can serialize it to and from the JSON
 * stored in {@link GameSaveEntity}.</p>
 */
public class GameSnapshot {

    /** Current game tick at the time of the save. */
    public long tick;

    /** Total number of players (human and AI). */
    public int playerCount;

    /** Player IDs controlled by the AI. */
    public List<Integer> aiPlayers = new ArrayList<>();

    /** Players eliminated at the time of the save. */
    public List<Integer> eliminatedPlayers = new ArrayList<>();

    /** Winner player ID, or {@code null} if the game was still ongoing. */
    public Integer winnerPlayerId;

    /** Whether the game had already ended. */
    public boolean over;

    /** All map tiles. */
    public List<TileSnap> tiles = new ArrayList<>();

    /** Standalone flags (flags not attached to a building). */
    public List<FlagSnap> flags = new ArrayList<>();

    /** All buildings (construction sites are stored as their completed type). */
    public List<BuildingSnap> buildings = new ArrayList<>();

    /** All roads of the network. */
    public List<RoadSnap> roads = new ArrayList<>();

    /** Per-player distribution priorities (playerId → good name → ordered building names). */
    public Map<Integer, Map<String, List<String>>> distributionPriorities = new HashMap<>();

    /** Per-player target garrison occupation percentage. */
    public Map<Integer, Integer> militaryOccupation = new HashMap<>();

    /** A single map tile. */
    public static class TileSnap {
        /** Horizontal coordinate. */
        public double x;
        /** Vertical coordinate. */
        public double y;
        /** Terrain type name. */
        public String type;
        /** Tile elevation. */
        public int elevation;
        /** Natural resource type name, or {@code null}. */
        public String resourceType;
        /** Remaining resource quantity, or {@code null}. */
        public Integer resourceQuantity;
    }

    /** A standalone flag. */
    public static class FlagSnap {
        /** Unique identifier. */
        public UUID id;
        /** Owning player. */
        public int playerId;
        /** Horizontal coordinate. */
        public double x;
        /** Vertical coordinate. */
        public double y;
    }

    /** A building with its stock and garrison. */
    public static class BuildingSnap {
        /** Building type name. */
        public String type;
        /** Owning player. */
        public int playerId;
        /** Horizontal coordinate. */
        public double x;
        /** Vertical coordinate. */
        public double y;
        /** Identifier of the flag attached to this building. */
        public UUID flagId;
        /** Stored resources (storage buildings only), keyed by resource name. */
        public Map<String, Integer> storedResources;
        /** Garrisoned soldier ranks (military buildings only). */
        public List<String> garrisonRanks;
        /** Stored gold coins (military buildings only). */
        public int storedCoins;
        /** Whether production is paused (production buildings only). */
        public Boolean productionPaused;
        /** Whether coin delivery is enabled (military buildings only). */
        public Boolean coinsAllowed;
    }

    /** A road between two flags. */
    public static class RoadSnap {
        /** Identifier of the flag at one end. */
        public UUID startFlagId;
        /** Identifier of the flag at the other end. */
        public UUID endFlagId;
        /** Intermediate path coordinates as [x, y] pairs. */
        public List<double[]> path = new ArrayList<>();
        /** Road level (1 = track, 2 = main road). */
        public int level;
        /** Number of completed deliveries over this road. */
        public int transportCount;
    }
}
