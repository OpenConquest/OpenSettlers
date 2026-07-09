package fr.opensettlers.entities.unit;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.Direction;
import fr.opensettlers.utils.enums.SoldierRank;
import fr.opensettlers.utils.enums.SoldierState;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import fr.opensettlers.entities.building.Building;

/**
 * A military unit that can garrison in a {@link MilitaryBuilding},
 * march toward enemy buildings, and engage in 1v1 duels.
 * <p>
 * Combat: each attack has a random hit chance (50%). Each hit does 1 damage.
 * Health depends on the soldier's {@link SoldierRank} (3 HP for a Private,
 * up to 7 HP for a General), so higher ranks win more duels.
 */
@Data
public class Soldier {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private int playerId;

    /** Military rank, raised by gold coins delivered to the garrison. */
    private SoldierRank rank = SoldierRank.PRIVATE;

    /** Current health points (max given by the rank). */
    private int health = SoldierRank.PRIVATE.getMaxHealth();

    /** Current position on the map. */
    private @NonNull Coordinates position;

    /** Current state in the soldier's lifecycle. */
    private SoldierState state = SoldierState.WALKING_TO_GARRISON;

    /** The building this soldier is garrisoned in (military or headquarters, null if moving). */
    private Building garrison;

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
     * Promotes the soldier one rank up (no effect on a General).
     * The soldier is healed to the new rank's maximum health.
     */
    public void promote() {
        if (rank.isPromotable()) {
            this.rank = rank.next();
            this.health = rank.getMaxHealth();
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
