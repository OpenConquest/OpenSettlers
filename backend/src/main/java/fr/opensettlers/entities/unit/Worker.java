package fr.opensettlers.entities.unit;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.WorkerState;
import fr.opensettlers.utils.enums.WorkerType;
import lombok.Data;

import java.util.List;
import java.util.UUID;
import fr.opensettlers.entities.world.Flag;

/**
 * Entity representing a physical settler/worker moving on the map.
 */
@Data
public class Worker {
    /** Unique identifier of the worker unit. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** Current type/role of the worker. Null indicates a neutral settler. */
    private WorkerType type;

    /** Current coordinates on the map. */
    private Coordinates position;

    /** Planned navigation path consisting of sequential flags. */
    private List<Flag> path;

    /** Index of the next flag in the path to move towards. */
    private int currentPathIndex;

    /** ID of the building the worker is assigned to. */
    private UUID targetBuildingId;

    /** Current state in the worker's life cycle. */
    private WorkerState state;

    /** Whether the worker carries the tool consumed at training time
     *  (returned to stock when the worker is dismissed). */
    private boolean carriesTool;

    /** For geologists: ID of the flag they were sent to survey around. */
    private UUID targetFlagId;

    /** For geologists: number of tiles left to survey before heading home. */
    private int surveysLeft;

    /** For geologists: ticks remaining on the current tile survey. */
    private int surveyCooldown;

    /**
     * Instantiates a new neutral worker.
     *
     * @param playerId owner of the unit
     */
    public Worker(int playerId) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.type = null;
        this.state = WorkerState.SPAWNING;
    }
}
