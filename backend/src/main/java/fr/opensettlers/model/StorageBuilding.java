package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class StorageBuilding extends Building{
    private final Map<ResourceType, Integer> storedResources;

    public StorageBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs, Map<ResourceType, Integer> storedResources) {
        super(id, playerId, position, costs, new Flag(UUID.randomUUID(), playerId, position));
        this.storedResources = storedResources;
        for (ResourceType resourceType : storedResources.keySet()) {
            storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0));
        }
    }

    private void store(ResourceType resourceType) {
        storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0) + 1);
    }

    private void retrieve(ResourceType resourceType) {
        int currentAmount = storedResources.getOrDefault(resourceType, 0);
        if (currentAmount < 1) {
            throw new IllegalArgumentException("Not enough resources in storage");
        }
        storedResources.put(resourceType, currentAmount - 1);
    }
}
