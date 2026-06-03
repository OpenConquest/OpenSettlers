package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;
import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

/** A military unit that can move, attack, and be killed. */
@Data
public class Soldier {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** Current health points. */
    private int health = 3;

    /** Current position on the map. */
    private @NonNull Coordinates position;

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
     * Checks if this unit is dead.
     * @return boolean
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

    /**
     * The function that is triggered every tick by the game loop.
     */
    public void tick() {
        if (!isDead()) {
            return;
        }

        // TODO soldier behaviour
    }
}
