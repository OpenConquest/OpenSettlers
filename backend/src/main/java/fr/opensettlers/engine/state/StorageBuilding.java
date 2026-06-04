package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.WorkerState;
import fr.opensettlers.engine.state.utils.WorkerType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Building that stores resources by type and quantity, and manages the neutral settler pool.
 */
@Getter
public class StorageBuilding extends Building {
    /**
     * Stored resources keyed by type.
     */
    private final Map<ResourceType, Integer> storedResources;

    /**
     * Stored neutral settlers who have no specialized job, capped at 99.
     */
    private int storedNeutralSettlers = 99;

    /**
     * Initializes a new StorageBuilding.
     *
     * @param playerId        owning player ID
     * @param position        map coordinates
     * @param storedResources initial resource stock
     */
    public StorageBuilding(int playerId, Coordinates position, Map<ResourceType, Integer> storedResources) {
        super(playerId, position);
        this.storedResources = new HashMap<>(storedResources);
    }

    /**
     * Stores one unit of the given resource.
     *
     * @param resourceType the resource type to store
     */
    public void storeResource(ResourceType resourceType) {
        storedResources.put(resourceType, storedResources.getOrDefault(resourceType, 0) + 1);
    }

    /**
     * Retrieves one unit of the given resource.
     *
     * @param resourceType the resource type to retrieve
     * @throws IllegalArgumentException if insufficient stock is available
     */
    public void retrieveResource(ResourceType resourceType) {
        int currentAmount = storedResources.getOrDefault(resourceType, 0);
        if (currentAmount < 1) {
            throw new IllegalArgumentException("Not enough resources in storage: " + resourceType);
        }
        storedResources.put(resourceType, currentAmount - 1);
    }

    /**
     * Gets the count of stored neutral settlers.
     *
     * @return the neutral settler count
     */
    public int getStoredNeutralSettlers() {
        return this.storedNeutralSettlers;
    }

    /**
     * Sets the count of stored neutral settlers, capped at 99.
     *
     * @param count the new count
     */
    public void setStoredNeutralSettlers(int count) {
        this.storedNeutralSettlers = Math.min(count, 99);
    }

    /**
     * Regenerates the stored neutral settlers back to 99 if they dropped below.
     */
    public void regenerateNeutralSettlers() {
        if (this.storedNeutralSettlers < 99) {
            this.storedNeutralSettlers = 99;
        }
    }

    /**
     * Spawns a new worker from the storage building's neutral settler pool.
     * Decrements the settler count to 98.
     *
     * @param targetType the role/type of the spawned worker
     * @param spawnPos   the starting coordinates of the worker
     * @return the spawned Worker, or null if no settlers are available
     */
    public Worker spawnWorker(WorkerType targetType, Coordinates spawnPos) {
        if (this.storedNeutralSettlers > 0) {
            this.storedNeutralSettlers--;
            Worker worker = new Worker(this.getPlayerId());
            worker.setType(targetType);
            worker.setPosition(spawnPos);
            worker.setState(WorkerState.SPAWNING);
            return worker;
        }
        return null;
    }
}
