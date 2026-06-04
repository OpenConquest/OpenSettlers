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
     * Private constructor to prevent instantiation of utility/configuration class.
     */
    private GameConfig() {}
}
