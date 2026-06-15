package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ShipState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A sea-going vessel used to carry colonization expeditions across the water.
 *
 * <p>A ship is launched from a coastal {@link fr.opensettlers.utils.BuildingName#HARBOR}
 * once the player owns a {@link fr.opensettlers.utils.BuildingName#SHIPYARD} and the
 * harbor has accumulated enough expedition materials. It follows a precomputed
 * path of water tiles to a target landing tile, where its colonists found a new
 * harbor — expanding the player's reach to remote shores and islands, exactly as
 * the sea expeditions of The Settlers II.</p>
 */
@Data
public class Ship {

    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** Current coordinates on the map (always a water tile while sailing). */
    private Coordinates position;

    /** Ordered water tiles from the launch point to the tile next to the shore. */
    private final List<Coordinates> path;

    /** Index of the next tile to advance to along {@link #path}. */
    private int pathIndex = 0;

    /** The land tile where the expedition will found a new harbor. */
    private final Coordinates landingTile;

    /** Ticks remaining before the ship advances one tile. */
    private int moveCooldown = 0;

    /** Current lifecycle state. */
    private ShipState state = ShipState.SAILING;

    /**
     * Creates a new expedition ship.
     *
     * @param playerId    owning player ID
     * @param start       launch coordinates (a water tile next to the home harbor)
     * @param path        water tiles to follow toward the destination shore
     * @param landingTile the land tile on which to found the new harbor
     */
    public Ship(int playerId, Coordinates start, List<Coordinates> path, Coordinates landingTile) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.position = start;
        this.path = new ArrayList<>(path);
        this.landingTile = landingTile;
    }

    /**
     * Indicates whether the ship has completed its expedition and can be
     * removed from the game state.
     *
     * @return {@code true} once the expedition has been carried out
     */
    public boolean isFinished() {
        return state == ShipState.FINISHED;
    }
}
