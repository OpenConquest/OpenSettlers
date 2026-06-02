package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Flag {
    private final UUID id;
    private final int playerId;
    private final Coordinates coordinates;
    private Map<ResourceType, Integer> resources;
    private final int capacity = 5;

    public void addResource(ResourceType resourceType) {
        if (resources.values().stream().mapToInt(Integer::intValue).sum() >= capacity) {
            throw new IllegalStateException("Flag is at full capacity");
        }
        resources.put(resourceType, resources.getOrDefault(resourceType, 0) + 1);
    }

    private void popResource(ResourceType resourceType) {
        int currentAmount = resources.getOrDefault(resourceType, 0);
        if (currentAmount <= 0) {
            throw new IllegalArgumentException("No resources of this type to pop");
        }
        resources.put(resourceType, currentAmount - 1);
    }
}
