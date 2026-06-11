package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.Direction;
import fr.opensettlers.engine.state.utils.SoldierState;
import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

/**
 * A military unit that can move, attack, and be killed.
 */
@Data
public class Soldier {
    /**
     * Unique identifier.
     */
    private final UUID id;

    /**
     * Owning player ID.
     */
    private final int playerId;

    /**
     * Current health points.
     */
    private int health = 3;

    /**
     * Current position on the map.
     */
    private @NonNull Coordinates position;

    /**
     * Current state in the soldier's life cycle.
     */
    private SoldierState state = SoldierState.WALKING_TO_GARRISON;

    /**
     * ID of the building the soldier is heading to (friendly garrison or enemy target).
     */
    private UUID targetBuildingId;

    /**
     * Attacks the target (50% hit chance, 1 damage) if on the same tile.
     *
     * @param target the enemy soldier
     */
    public void attack(Soldier target) {
        if (this.isSamePosition(target)) {
            if (Math.random() < 0.5) {
                target.health -= 1;
            }
        }
    }

    /**
     * Moves one tile in the given direction.
     *
     * @param direction movement direction
     */
    public void move(Direction direction) {
        this.position.move(direction);
    }

    /**
     * Moves one grid step toward the target position (offset grid, diagonal allowed).
     *
     * @param target the destination coordinates
     */
    public void stepToward(Coordinates target) {
        double dx = Math.signum(target.getX() - this.position.getX());
        double dy = Math.signum(target.getY() - this.position.getY());
        this.position.move(dx, dy);
    }

    /**
     * Checks whether the soldier stands on the given position.
     *
     * @param target the position to compare
     * @return {@code true} if positions match
     */
    public boolean isAt(Coordinates target) {
        return this.position.getX() == target.getX() && this.position.getY() == target.getY();
    }

    /**
     * Checks if this unit is dead.
     *
     * @return boolean indicating whether the soldier is dead
     */
    public boolean isDead() {
        return this.health <= 0;
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
