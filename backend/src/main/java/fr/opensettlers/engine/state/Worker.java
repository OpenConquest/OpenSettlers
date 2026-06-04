package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.WorkerState;
import fr.opensettlers.engine.state.utils.WorkerType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

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
