package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * StorageBuilding is a building that can store resources. It has a map of stored resources and their quantities.
 * It provides methods to store and retrieve resources.
 */
@Getter
public class StorageBuilding extends Building{
    /**
     * A map of the resources stored in this building and their quantities. The key is the type of resource and the value is the quantity of that resource.
     */
    private final Map<ResourceType, Integer> storedResources;

    /**
     * Creates a new StorageBuilding with the given parameters. The storedResources map is initialized with the provided storedResources parameter, and any resource types that are not included in the parameter are initialized to 0.
     * @param id the unique identifier for this building
     * @param playerId the ID of the player who owns this building
     * @param position the coordinates of this building on the game map
     * @param storedResources the initial resources stored in this building, represented as a map of resource types and their quantities. Any resource types not included in this map will be initialized to 0.
     */
    public StorageBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> storedResources) {
        super(id, playerId, position, new Flag(UUID.randomUUID(), playerId, position));
        this.storedResources = storedResources;
        for (ResourceType resourceType : storedResources.keySet()) {
            storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0));
        }
    }

    /**
     * Stores the given resource type in this building. If the resource type is already stored, its quantity is increased by 1. If it is not already stored, it is added to the storedResources map with a quantity of 1.
     *
     * @param resourceType the type of resource to store in this building
     */
    private void store(ResourceType resourceType) {
        storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0) + 1);
    }

    /**
     * Retrieves one unit of the given resource type from this building. If the resource type is not stored or its quantity is less than 1, an IllegalArgumentException is thrown. Otherwise, the quantity of the resource type in the storedResources map is decreased by 1.
     *
     * @param resourceType the type of resource to retrieve from this building
     * @throws IllegalArgumentException if the resource type is not stored or its quantity is less than 1
     */
    private void retrieve(ResourceType resourceType) {
        int currentAmount = storedResources.getOrDefault(resourceType, 0);
        if (currentAmount < 1) {
            throw new IllegalArgumentException("Not enough resources in storage");
        }
        storedResources.put(resourceType, currentAmount - 1);
    }
}
