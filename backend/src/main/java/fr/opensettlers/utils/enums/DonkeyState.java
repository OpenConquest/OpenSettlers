package fr.opensettlers.utils.enums;

/**
 * Lifecycle states of a donkey unit.
 */
public enum DonkeyState {
    /** Walking along the road network toward its assigned road. */
    WALKING_TO_ROAD,

    /** Standing on its road, doubling the carrier's transport speed. */
    ASSISTING,

    /** Without an assignment (e.g. its road was destroyed), awaiting a new road. */
    IDLE
}
