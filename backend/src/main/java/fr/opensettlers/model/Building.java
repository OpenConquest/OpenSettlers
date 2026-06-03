package fr.opensettlers.model;

import lombok.*;
import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

/**
 * Base class for all buildings in the game.
 * Each building has a position (x, y) and belongs to a player (playerId).
 * Specific types of buildings will extend this class and add their own properties and behaviors.
 */
@Data
public abstract class Building {
    /**
     * Unique identifier for the building.
     */
    private final UUID id;

    /**
     * Identifier of the player who owns the building.
     */
    private final int playerId;
    /**
     * Coordinates of the building on the game map. The coordinates are represented as a pair of integers (x, y).
     */
    private final Coordinates position;

    /**
     * Flag currently attached to the building, if any. A building can have at most one flag attached at a time.
     */
    private final Flag attachedFlag;
}
