package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * Represents a flag in the game, which can hold resources and is associated with a player.
 */
@Data
public class Flag {
    /**
     * Unique identifier for the flag.
     */
    private final UUID id;

    /**
     * Identifier of the player who owns the flag.
     */
    private final int playerId;

    /**
     * Coordinates of the flag on the game map. The coordinates are represented as a pair of integers (x, y).
     */
    private final Coordinates coordinates;

    /**
     * Map to store the resources currently held by the flag. The key is the resource type and the value is the amount of that resource.
     */
    private final Map<ResourceType, Integer> resources = new HashMap<>();

    /**
     * Maximum capacity of the flag, which limits the total amount of resources it can hold.
     */
    private final int maxCapacity = 5;

    /**
     * Adds a resource to the flag. If the flag is already at full capacity, an exception is thrown.
     *
     * @param resourceType The type of resource to add to the flag.
     * @throws IllegalStateException if the flag is at full capacity.
     */
    public void addResource(ResourceType resourceType) {
        if (resources.values().stream().mapToInt(Integer::intValue).sum() >= maxCapacity) {
            throw new IllegalStateException("Flag is at full capacity");
        }
        resources.put(resourceType, resources.getOrDefault(resourceType, 0) + 1);
    }

    /**
     * Removes a resource from the flag. If there are no resources of the specified type to remove, an exception is thrown.
     *
     * @param resourceType The type of resource to remove from the flag.
     * @throws IllegalArgumentException if there are no resources of the specified type to remove.
     */
    public void popResource(ResourceType resourceType) {
        int currentAmount = resources.getOrDefault(resourceType, 0);
        if (currentAmount <= 0) {
            throw new IllegalArgumentException("No resources of this type to pop");
        }
        resources.put(resourceType, currentAmount - 1);
    }

    /**
     * Checks if the flag is at full capacity by summing the total amount of resources it currently holds and comparing it to the maximum capacity.
     *
     * @return true if the flag is at full capacity, false otherwise.
     */
    public boolean isFull() {
        return resources.values().stream().mapToInt(Integer::intValue).sum() >= maxCapacity;
    }

    /**
     * Checks if the flag is empty by summing the total amount of resources it currently holds and checking if it is zero.
     *
     * @return true if the flag is empty, false otherwise.
     */
    public boolean isEmpty() {
        return resources.values().stream().mapToInt(Integer::intValue).sum() == 0;
    }

}
