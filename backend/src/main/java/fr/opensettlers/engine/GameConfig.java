package fr.opensettlers.engine;

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
