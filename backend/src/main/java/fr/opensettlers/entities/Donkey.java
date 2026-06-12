package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.DonkeyState;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * A donkey unit bred by a {@link fr.opensettlers.utils.BuildingName#DONKEY_BREEDER}.
 * It walks on its own to a level-2 main road and assists the carrier there,
 * doubling the road's transport speed.
 */
@Data
public class Donkey {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** Current coordinates on the map. */
    private Coordinates position;

    /** Planned navigation path consisting of sequential flags. */
    private List<Flag> path;

    /** Index of the next flag in the path to move towards. */
    private int currentPathIndex;

    /** The road this donkey is assigned to (walking toward or assisting). */
    private Road assignedRoad;

    /** Current state in the donkey's lifecycle. */
    private DonkeyState state = DonkeyState.IDLE;

    /**
     * Creates a new donkey.
     *
     * @param playerId owner of the unit
     * @param position spawn coordinates
     */
    public Donkey(int playerId, Coordinates position) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.position = position;
    }
}
