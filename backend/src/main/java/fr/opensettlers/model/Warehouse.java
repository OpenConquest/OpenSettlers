package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A Warehouse is a building that can store resources. It has no production capabilities and is used solely for storage.
 */
public class Warehouse extends StorageBuilding {
    /**
     * Creates a new Warehouse for the given player at the given position. The Warehouse starts with no resources.
     *
     * @param playerId the ID of the player who owns this Warehouse
     * @param position the position of the Warehouse on the map
     */
    public Warehouse(int playerId, Coordinates position) {
        Map<ResourceType, Integer> buildingCost = new HashMap<ResourceType, Integer>();
        buildingCost.put(ResourceType.PLANK, 4);
        buildingCost.put(ResourceType.STONE, 3);
        super(UUID.randomUUID(), playerId, position, buildingCost, Map.of());
    }
}
