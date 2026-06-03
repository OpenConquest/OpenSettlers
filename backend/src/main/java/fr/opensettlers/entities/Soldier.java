package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;
import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

@Data
public class Soldier {
    /**
     * Unique identifier for the soldier.
     */
    private final UUID id;

    /**
     * Identifier of the player who owns the soldier.
     */
    private final int playerId;

    /**
     * Health of the soldier.
     */
    private int health = 3;

    /**
     * Position of the soldier.
     */
    private @NonNull Coordinates position;

    /**
     * Attacks another soldier. 50% chance to deal 1 damage to the target if it's in the same position.
     * @param target - The enemy to attack
     */
    public void attack(Soldier target) {
        if (this.isSamePosition(target)) {
            if (Math.random() < 0.5) {
                target.health -= 1;
            }
        }
    }

    /**
     * Changes the position of the unit by 1 unit in the specified direction.
     */
    public void move(Direction direction) {
        this.position.move(direction);
    }

    /**
     * Checks if this unit is alive.
     * @return boolean - True if health > 0.
     */
    public boolean isAlive() {
        return health > 0;
    }

    /**
     * Checks if another Soldier shares the same position. There can be only one soldier per tile, two soldiers sharing
     * the same tile means they've engaged combat.
     * @param other - Another soldier.
     * @return boolean
     */
    public boolean isSamePosition(Soldier other) {
        return this.position.equals(other.position);
    }
}
