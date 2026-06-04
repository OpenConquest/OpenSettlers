package fr.opensettlers.engine.state.utils;

/**
 * State machine states for physical worker units.
 */
public enum WorkerState {
    SPAWNING,          // Exiting the HQ/Warehouse
    WALKING_TO_JOB,    // Traveling on the road network to target building
    WORKING,           // On-site performing duties (leveling, building, harvesting)
    RETURNING          // Traveling back to HQ/Warehouse after building destruction
}
