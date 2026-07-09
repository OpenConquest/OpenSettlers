package fr.opensettlers.utils.enums;

/**
 * Possible states for a resource carrier working on a road.
 */
public enum CarrierState {
    /** Waiting at or near a flag for a resource to transport. */
    IDLE,

    /** Walking toward a flag to pick up a resource. */
    WALKING_TO_PICKUP,

    /** Carrying a resource toward the destination flag. */
    WALKING_TO_DELIVER,

    /** Blocked because the destination flag is full. */
    WAITING
}
