package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class Flag {
    private final UUID id;
    private final int playerId;
    private final Coordinates coordinates;
    private Map<ResourceType, Integer> resources = new HashMap<>();
    private final int maxCapacity = 5;

    public Flag(UUID id, int playerId, Coordinates coordinates) {
        this.id = id;
        this.playerId = playerId;
        this.coordinates = coordinates;
    }

    public void addResource(ResourceType resourceType) {
        if (resources.values().stream().mapToInt(Integer::intValue).sum() >= maxCapacity) {
            throw new IllegalStateException("Flag is at full capacity");
        }
        resources.put(resourceType, resources.getOrDefault(resourceType, 0) + 1);
    }

    public void popResource(ResourceType resourceType) {
        int currentAmount = resources.getOrDefault(resourceType, 0);
        if (currentAmount <= 0) {
            throw new IllegalArgumentException("No resources of this type to pop");
        }
        resources.put(resourceType, currentAmount - 1);
    }
    public boolean isFull() {
        return resources.values().stream().mapToInt(Integer::intValue).sum() >= maxCapacity;
    }

    public boolean isEmpty() {
        return resources.values().stream().mapToInt(Integer::intValue).sum() == 0;
    }

}
