package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/** Building that stores resources by type and quantity. */
@Getter
public class StorageBuilding extends Building{
    /** Stored resources keyed by type. */
    private final Map<ResourceType, Integer> storedResources;

    /**
     * @param id              unique identifier
     * @param playerId        owning player ID
     * @param position        map coordinates
     * @param storedResources initial resource stock
     */
    public StorageBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> storedResources) {
        super(id, playerId, position);
        this.storedResources = storedResources;
        for (ResourceType resourceType : storedResources.keySet()) {
            storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0));
        }
    }

    /**
     * Stores one unit of the given resource.
     *
     * @param resourceType the resource type to store
     */
    private void store(ResourceType resourceType) {
        storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0) + 1);
    }

    /**
     * Retrieves one unit of the given resource.
     *
     * @param resourceType the resource type to retrieve
     * @throws IllegalArgumentException if insufficient stock
     */
    private void retrieve(ResourceType resourceType) {
        int currentAmount = storedResources.getOrDefault(resourceType, 0);
        if (currentAmount < 1) {
            throw new IllegalArgumentException("Not enough resources in storage");
        }
        storedResources.put(resourceType, currentAmount - 1);
    }
}
