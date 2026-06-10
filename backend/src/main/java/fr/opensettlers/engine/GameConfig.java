package fr.opensettlers.engine;

import fr.opensettlers.engine.state.utils.BuildingName;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Contains configuration properties for the game engine, loaded from application.properties.
 */
public final class GameConfig {

    /**
     * Time interval in milliseconds separating each tick.
     * Configured using key "opensettlers.tick-rate" (e.g. 0.1s), defaulting to 100ms.
     */
    public static final int TICK_PERIOD_MS = (int) org.eclipse.microprofile.config.ConfigProvider.getConfig()
            .getOptionalValue("opensettlers.tick-rate", java.time.Duration.class)
            .orElse(java.time.Duration.ofMillis(100))
            .toMillis();

    /**
     * Default cooldown ticks required for a production building to produce a resource.
     * Configured using key "opensettlers.production-time", defaulting to 5 ticks.
     */
    public static final int PRODUCTION_TIME = getIntConfig("opensettlers.production-time", 5);

    // --- Worker max distances ---
    public static final int WOODCUTTER_MAX_DISTANCE = getIntConfig("opensettlers.worker.woodcutter.max-distance", 4);
    public static final int FORESTER_MAX_DISTANCE = getIntConfig("opensettlers.worker.forester.max-distance", 4);
    public static final int QUARRYMAN_MAX_DISTANCE = getIntConfig("opensettlers.worker.quarryman.max-distance", 4);
    public static final int FARMER_MAX_FIELDS = getIntConfig("opensettlers.worker.farmer.max-fields", 3);
    public static final int FISHERMAN_MAX_DISTANCE = getIntConfig("opensettlers.worker.fisherman.max-distance", 3);

    // --- Military: garrison capacity ---
    public static final int BARRACKS_CAPACITY = getIntConfig("opensettlers.military.capacity.barracks", 2);
    public static final int GUARDHOUSE_CAPACITY = getIntConfig("opensettlers.military.capacity.guardhouse", 3);
    public static final int WATCHTOWER_CAPACITY = getIntConfig("opensettlers.military.capacity.watchtower", 6);
    public static final int CASTLE_CAPACITY = getIntConfig("opensettlers.military.capacity.castle", 9);
    public static final int HEADQUARTERS_CAPACITY = getIntConfig("opensettlers.military.capacity.headquarters", 1);

    // --- Military: territory radius (hex distance) ---
    public static final int BARRACKS_TERRITORY_RADIUS = getIntConfig("opensettlers.military.radius.barracks", 4);
    public static final int GUARDHOUSE_TERRITORY_RADIUS = getIntConfig("opensettlers.military.radius.guardhouse", 5);
    public static final int WATCHTOWER_TERRITORY_RADIUS = getIntConfig("opensettlers.military.radius.watchtower", 8);
    public static final int CASTLE_TERRITORY_RADIUS = getIntConfig("opensettlers.military.radius.castle", 11);
    public static final int HEADQUARTERS_TERRITORY_RADIUS = getIntConfig("opensettlers.military.radius.headquarters", 20);

    /**
     * Returns the garrison capacity for a given military building type.
     */
    public static int militaryCapacity(BuildingName name) {
        return switch (name) {
            case BARRACKS -> BARRACKS_CAPACITY;
            case GUARD_HOUSE -> GUARDHOUSE_CAPACITY;
            case WATCH_TOWER -> WATCHTOWER_CAPACITY;
            case CASTLE -> CASTLE_CAPACITY;
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
            case HEADQUARTERS -> HEADQUARTERS_TERRITORY_RADIUS;
            default -> 0;
        };
    }

    private static int getIntConfig(String key, int defaultValue) {
        return ConfigProvider.getConfig()
                .getOptionalValue(key, Integer.class)
                .orElse(defaultValue);
    }

    /**
     * Private constructor to prevent instantiation of utility/configuration class.
     */
    private GameConfig() {}
}
