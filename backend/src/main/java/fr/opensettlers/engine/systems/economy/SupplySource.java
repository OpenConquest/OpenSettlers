package fr.opensettlers.engine.systems.economy;

/**
 * Enumeration of the different sources of supply in the economy.
 */
public enum SupplySource {
    /**
     * Supply originating from a production building's output slot.
     */
    PRODUCTION,

    /**
     * Supply sitting unrouted on a road network flag.
     */
    FLAG,

    /**
     * Supply stored inside a warehouse/storage building.
     */
    WAREHOUSE
}
