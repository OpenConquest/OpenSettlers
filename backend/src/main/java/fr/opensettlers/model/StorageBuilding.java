package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class StorageBuilding extends Building{
    private final Map<ResourceType, Integer> storedResources;

    public StorageBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs, Map<ResourceType, Integer> storedResources) {
        super(id, playerId, position, costs);
        this.storedResources = storedResources;
        for (ResourceType resourceType : storedResources.keySet()) {
            storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0));
        }
    }

    private void store(ResourceType resourceType, int amount) {
        storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0) + amount);
    }

    private void retrieve(ResourceType resourceType, int amount) {
        int currentAmount = storedResources.getOrDefault(resourceType, 0);
        if (currentAmount < amount) {
            throw new IllegalArgumentException("Not enough resources in storage");
        }
        storedResources.put(resourceType, currentAmount - amount);
    }
}
