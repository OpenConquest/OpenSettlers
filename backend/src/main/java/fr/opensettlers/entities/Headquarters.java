package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;

import java.util.Map;
import java.util.UUID;

/** Main building of a player; stores starting resources. */
public class Headquarters extends StorageBuilding {
    /**
     * Constructor of the Headquarters class.
     *
     * @param playerId          owning player ID
     * @param position          map position
     * @param startingResources initial resources to store
     */
    public Headquarters(int playerId, Coordinates position, Map<ResourceType, Integer> startingResources) {
        super(UUID.randomUUID(), playerId, position, startingResources);
    }
}
