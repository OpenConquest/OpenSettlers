package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

/**
 * The Headquarters is the main building of a player. It is where the player starts the game and where they can store their resources.
 */
public class Headquarters extends StorageBuilding {
    /**
     * Creates a new Headquarters for the given player with the given starting resources. The position of the Headquarters is determined by the game and is not set in the constructor.
     *
     * @param playerId the ID of the player who owns this Headquarters
     * @param position the position of the Headquarters on the map
     * @param startingResources the resources that the player starts with, which will be stored in the Headquarters
     */
    public Headquarters(int playerId, Coordinates position, Map<ResourceType, Integer> startingResources) {
        super(UUID.randomUUID(), playerId, position, startingResources);
    }
}
