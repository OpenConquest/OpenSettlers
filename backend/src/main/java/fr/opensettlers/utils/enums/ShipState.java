package fr.opensettlers.utils.enums;

/**
 * Lifecycle states of a {@link fr.opensettlers.entities.unit.Ship}.
 */
public enum ShipState {
    /** Sailing across the water toward the landing tile of an expedition. */
    SAILING,

    /** The ship reached its destination; its colonists may now found a harbor. */
    LANDED,

    /** The expedition completed (a harbor was founded); the ship is retired. */
    FINISHED
}
