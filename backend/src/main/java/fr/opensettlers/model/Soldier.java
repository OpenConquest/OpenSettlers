package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.UUID;

public class Soldier {
    private UUID id;
    private int playerId;
    private int health = 3;
    private Coordinates position;

    public Soldier(UUID id, int playerId, Coordinates position) {
        this.id = id;
        this.playerId = playerId;
        this.position = position;
    }

    public void attack(Soldier target) {
        if (this.isSamePosition(target)) {
            if (Math.random() < 0.5) {
                target.health -= 1;
            }
        }
    }

    public void move(Coordinates newPosition) {
        this.position = newPosition;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isSamePosition(Soldier other) {
        return this.position.equals(other.position);
    }
}
