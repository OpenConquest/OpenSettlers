package fr.opensettlers.utils;

import fr.opensettlers.utils.BuildingName;

/**
 * Central balance and timing configuration of the game engine.
 * All values are plain constants tuned to mirror The Settlers II (10th Anniversary).
 */
public final class GameConfig {

    // --- Game loop ---

    /** Time interval in milliseconds separating each tick. */
    public static final int TICK_PERIOD_MS = 100;

    /** Default cooldown ticks required for a production building to produce a resource. */
    public static final int PRODUCTION_TIME = 5;

    /** Width and height of the generated square map in tiles. */
    public static final int MAP_SIZE = 64;

    /** Number of game ticks between two soldier movement steps. */
    public static final int SOLDIER_MOVE_TICKS = 3;

    /** Maximum hex distance from which military buildings can send attackers. */
    public static final int ATTACK_RADIUS = 50;

    // --- Worker max distances ---

    public static final int WOODCUTTER_MAX_DISTANCE = 4;
    public static final int FORESTER_MAX_DISTANCE = 4;
    public static final int QUARRYMAN_MAX_DISTANCE = 4;
    public static final int FARMER_MAX_FIELDS = 3;
    public static final int FISHERMAN_MAX_DISTANCE = 3;

    /** Hex distance around a mine in which the miner digs out ore deposits. */
    public static final int MINER_MAX_DISTANCE = 2;

    /** Hex distance in which a hunter tracks wild game. */
    public static final int HUNTER_MAX_DISTANCE = 5;

    // --- Military: garrison capacity ---

    public static final int BARRACKS_CAPACITY = 2;
    public static final int GUARDHOUSE_CAPACITY = 3;
    public static final int WATCHTOWER_CAPACITY = 6;
    public static final int CASTLE_CAPACITY = 9;
    public static final int FORTRESS_CAPACITY = 12;
    public static final int HEADQUARTERS_CAPACITY = 1;

    // --- Military: territory radius (hex distance) ---

    public static final int BARRACKS_TERRITORY_RADIUS = 4;
    public static final int GUARDHOUSE_TERRITORY_RADIUS = 5;
    public static final int WATCHTOWER_TERRITORY_RADIUS = 8;
    public static final int CASTLE_TERRITORY_RADIUS = 11;
    public static final int FORTRESS_TERRITORY_RADIUS = 13;
    public static final int HEADQUARTERS_TERRITORY_RADIUS = 20;

    // --- Military: gold coins & promotion ---

    /** Maximum gold coins each military building type can hold for promotions. */
    public static final int BARRACKS_COIN_CAPACITY = 2;
    public static final int GUARDHOUSE_COIN_CAPACITY = 2;
    public static final int WATCHTOWER_COIN_CAPACITY = 4;
    public static final int CASTLE_COIN_CAPACITY = 6;
    public static final int FORTRESS_COIN_CAPACITY = 8;

    /** Ticks between two soldier promotions inside the same building. */
    public static final int PROMOTION_TICKS = 50;

    // --- Catapult ---

    /** Maximum hex distance at which a catapult can shoot enemy military buildings. */
    public static final int CATAPULT_RANGE = 8;

    /** Ticks between two catapult shots. */
    public static final int CATAPULT_COOLDOWN_TICKS = 100;

    /** Probability that a catapult projectile hits its target. */
    public static final double CATAPULT_HIT_CHANCE = 0.5;

    // --- Roads & donkeys ---

    /** Number of completed deliveries after which a road becomes a level-2 main road. */
    public static final int ROAD_UPGRADE_DELIVERIES = 30;

    /** Carrier steps per tick on a donkey-assisted level-2 road. */
    public static final int DONKEY_ROAD_SPEED = 2;

    // --- Geologist ---

    /** Hex distance around the target flag in which a geologist surveys mountain tiles. */
    public static final int GEOLOGIST_RANGE = 4;

    /** Number of tiles a geologist surveys before heading home. */
    public static final int GEOLOGIST_SURVEYS = 8;

    /** Ticks a geologist spends surveying each tile. */
    public static final int GEOLOGIST_SURVEY_TICKS = 10;

    // --- Fog of war ---

    /** Vision radius of non-military buildings. */
    public static final int VISION_BUILDING_RADIUS = 5;

    /** Vision radius of units (workers, soldiers, donkeys). */
    public static final int VISION_UNIT_RADIUS = 3;

    /** Vision radius of flags (covers the roads between buildings). */
    public static final int VISION_FLAG_RADIUS = 2;

    /** Extra vision beyond the territory radius of military buildings and headquarters. */
    public static final int VISION_TERRITORY_MARGIN = 2;

    /**
     * Returns the garrison capacity for a given military building type.
     */
    public static int militaryCapacity(BuildingName name) {
        return switch (name) {
            case BARRACKS -> BARRACKS_CAPACITY;
            case GUARD_HOUSE -> GUARDHOUSE_CAPACITY;
            case WATCH_TOWER -> WATCHTOWER_CAPACITY;
            case CASTLE -> CASTLE_CAPACITY;
            case FORTRESS -> FORTRESS_CAPACITY;
            case HEADQUARTERS -> HEADQUARTERS_CAPACITY;
            default -> 0;
        };
    }

    /**
     * Returns the territory radius for a given military building type.
     */
    public static int militaryRadius(BuildingName name) {
        return switch (name) {
            case BARRACKS -> BARRACKS_TERRITORY_RADIUS;
            case GUARD_HOUSE -> GUARDHOUSE_TERRITORY_RADIUS;
            case WATCH_TOWER -> WATCHTOWER_TERRITORY_RADIUS;
            case CASTLE -> CASTLE_TERRITORY_RADIUS;
            case FORTRESS -> FORTRESS_TERRITORY_RADIUS;
            case HEADQUARTERS -> HEADQUARTERS_TERRITORY_RADIUS;
            default -> 0;
        };
    }

    /**
     * Returns the gold coin storage capacity for a given military building type.
     */
    public static int coinCapacity(BuildingName name) {
        return switch (name) {
            case BARRACKS -> BARRACKS_COIN_CAPACITY;
            case GUARD_HOUSE -> GUARDHOUSE_COIN_CAPACITY;
            case WATCH_TOWER -> WATCHTOWER_COIN_CAPACITY;
            case CASTLE -> CASTLE_COIN_CAPACITY;
            case FORTRESS -> FORTRESS_COIN_CAPACITY;
            default -> 0;
        };
    }

    /**
     * Private constructor to prevent instantiation of utility/configuration class.
     */
    private GameConfig() {}
}
