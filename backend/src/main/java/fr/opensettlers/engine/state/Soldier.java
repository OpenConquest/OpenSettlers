package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.Direction;
import fr.opensettlers.engine.state.utils.SoldierState;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A military unit that can garrison in a {@link MilitaryBuilding},
 * march toward enemy buildings, and engage in 1v1 duels.
 * <p>
 * Combat: each attack has a random hit chance (50%). Each hit does 1 damage.
 * A soldier has 3 HP. No strength/rank system.
 */
@Data
public class Soldier {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private int playerId;

    /** Current health points (max 3). */
    private int health = 3;

    /** Current position on the map. */
    private @NonNull Coordinates position;

    /** Current state in the soldier's lifecycle. */
    private SoldierState state = SoldierState.WALKING_TO_GARRISON;

    /** The building this soldier is garrisoned in (null if moving). */
    private MilitaryBuilding garrison;

    /** The building this soldier is heading to: enemy target or friendly garrison destination. */
    private Building targetBuilding;

    /** The opponent this soldier is currently dueling (null if not fighting). */
    private Soldier opponent;

    /** Navigation path as a list of coordinates to follow. */
    private List<Coordinates> path = new ArrayList<>();

    /** Current index in the path list. */
    private int pathIndex = 0;

    /**
     * Creates a new soldier owned by the given player at the given position.
     *
     * @param playerId owner player ID
     * @param position spawn position
     */
    public Soldier(int playerId, Coordinates position) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.position = position;
    }

    /**
     * Attempts to hit the target soldier. 50% chance to deal 1 damage.
     *
     * @param target the enemy soldier
     */
    public void attack(Soldier target) {
        if (Math.random() < 0.5) {
            target.health -= 1;
        }
    }

    /**
     * Checks if this soldier is dead (health <= 0).
     *
     * @return {@code true} if the soldier is dead
     */
    public boolean isDead() {
        return this.health <= 0;
    }

    /**
     * Checks if the soldier has reached the end of its path.
     *
     * @return {@code true} if path is complete or empty
     */
    public boolean hasReachedDestination() {
        return path == null || path.isEmpty() || pathIndex >= path.size();
    }

    /**
     * Advances the soldier one step along its path.
     */
    public void advanceOnPath() {
        if (!hasReachedDestination()) {
            this.position = path.get(pathIndex);
            pathIndex++;
        }
    }

    /**
     * Moves one hex step toward the target, picking the neighbor direction that
     * minimizes the remaining hex distance. Used when no precomputed path exists.
     *
     * @param target the destination coordinates
     */
    public void stepToward(Coordinates target) {
        Coordinates best = null;
        int bestDist = this.position.distanceTo(target);
        for (Direction dir : Direction.values()) {
            Coordinates candidate = this.position.neighbor(dir);
            int dist = candidate.distanceTo(target);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        if (best != null) {
            this.position = best;
        }
    }

    /**
     * Checks whether the soldier stands on the given position.
     *
     * @param target the position to compare
     * @return {@code true} if positions match
     */
    public boolean isAt(Coordinates target) {
        return this.position.equals(target);
    }

    /**
     * Checks if another soldier occupies the same tile (implies combat).
     *
     * @param other the other soldier
     * @return {@code true} if positions match
     */
    public boolean isSamePosition(Soldier other) {
        return this.position.equals(other.position);
    }
}
