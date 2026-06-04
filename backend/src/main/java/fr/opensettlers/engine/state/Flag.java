package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A flag that holds resources for transport between buildings. */
@Data
public class Flag {
    /** Unique identifier. */
    private final UUID id;

    /** Owning player ID. */
    private final int playerId;

    /** Position on the game map. */
    private final Coordinates coordinates;

    /** Boolean defining if the building is destroyed. False means it is active. */
    private boolean destroyed = false;

    /** Resources currently held, keyed by type. */
    private final Map<ResourceType, Integer> resources = new HashMap<>();

    /** Maximum total resource capacity. */
    private static final int MAX_CAPACITY = 5;

    /**
     * Adds one unit of the given resource.
     *
     * @param resourceType the resource type to add
     * @throws IllegalStateException if the flag is full
     */
    public void addResource(ResourceType resourceType) {
        if (resources.values().stream().mapToInt(Integer::intValue).sum() >= MAX_CAPACITY) {
            throw new IllegalStateException("Flag is at full capacity");
        }
        resources.put(resourceType, resources.getOrDefault(resourceType, 0) + 1);
    }

    /**
     * Removes one unit of the given resource.
     *
     * @param resourceType the resource type to remove
     * @throws IllegalArgumentException if none available
     */
    public void popResource(ResourceType resourceType) {
        int currentAmount = resources.getOrDefault(resourceType, 0);
        if (currentAmount <= 0) {
            throw new IllegalArgumentException("No resources of this type to pop");
        }
        resources.put(resourceType, currentAmount - 1);
    }

    /** @return {@code true} if the flag is at full capacity */
    public boolean isFull() {
        return resources.values().stream().mapToInt(Integer::intValue).sum() >= MAX_CAPACITY;
    }

    /** @return {@code true} if the flag holds no resources */
    public boolean isEmpty() {
        return resources.values().stream().mapToInt(Integer::intValue).sum() == 0;
    }

    /** Destroys the building, rendering it inactive. */
    public void destroy() {
        this.destroyed = true;
    }

    /**
     * Checks if the building has been destroyed.
     * @return boolean
     */
    public boolean isDestroyed() {
        return this.destroyed;
    }
}
