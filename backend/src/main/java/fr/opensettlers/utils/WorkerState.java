package fr.opensettlers.utils;

/**
 * State machine states for physical worker units.
 */
public enum WorkerState {
    /**
     * Worker is exiting the storage building or headquarters.
     */
    SPAWNING,

    /**
     * Worker is traveling along the road network toward their target destination.
     */
    WALKING_TO_JOB,

    /**
     * Worker has arrived on-site and is actively performing their job.
     */
    WORKING,

    /**
     * Worker has arrived on-site but is waiting (inputs missing or output full).
     */
    WAITING,

    /**
     * Worker is returning to the nearest warehouse after their job was dismantled.
     */
    RETURNING
}
