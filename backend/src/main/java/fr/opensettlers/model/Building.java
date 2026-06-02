package fr.opensettlers.model;

import lombok.Getter;
import fr.opensettlers.utils.Coordinates;

/**
 * Base class for all buildings in the game.
 * Each building has a position (x, y) and belongs to a player (playerId).
 * Specific types of buildings will extend this class and add their own properties and behaviors.
 */
@Getter
public abstract class Building {
    /**
     * The x-coordinate of the building's position on the game map.
     */
    private final int x;

    /**
     * The y-coordinate of the building's position on the game map.
     */
    private final int y;

    /**
     * The ID of the player who owns this building.
     */
    private final int playerId;

    /**
     * Constructs a new Building with the specified coordinates and player ID.
     *
     * @param coordinates The coordinates of the building's position on the game map.
     * @param playerId    The ID of the player who owns this building.
     */
    public Building(Coordinates coordinates, int playerId) {
        this.x = coordinates.getX();
        this.y = coordinates.getY();
        this.playerId = playerId;
    }
}
