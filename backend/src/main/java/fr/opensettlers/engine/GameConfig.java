package fr.opensettlers.engine;

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
    public static final int PRODUCTION_TIME = org.eclipse.microprofile.config.ConfigProvider.getConfig()
            .getOptionalValue("opensettlers.production-time", Integer.class)
            .orElse(5);

    /**
     * Width and height of the generated square map in tiles.
     * Configured using key "opensettlers.map-width", defaulting to 32.
     */
    public static final int MAP_SIZE = org.eclipse.microprofile.config.ConfigProvider.getConfig()
            .getOptionalValue("opensettlers.map-width", Integer.class)
            .orElse(32);

    /**
     * Number of game ticks between two soldier movement steps.
     * Configured using key "opensettlers.soldier-move-ticks", defaulting to 3.
     */
    public static final int SOLDIER_MOVE_TICKS = org.eclipse.microprofile.config.ConfigProvider.getConfig()
            .getOptionalValue("opensettlers.soldier-move-ticks", Integer.class)
            .orElse(3);

    /**
     * Maximum distance from which military buildings can send attackers.
     * Configured using key "opensettlers.attack-radius", defaulting to 25 tiles.
     */
    public static final int ATTACK_RADIUS = org.eclipse.microprofile.config.ConfigProvider.getConfig()
            .getOptionalValue("opensettlers.attack-radius", Integer.class)
            .orElse(25);

    /**
     * Private constructor to prevent instantiation of utility/configuration class.
     */
    private GameConfig() {}
}
