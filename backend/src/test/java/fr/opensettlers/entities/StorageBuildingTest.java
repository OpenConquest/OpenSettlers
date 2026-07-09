package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.ResourceType;
import fr.opensettlers.utils.enums.WorkerType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import fr.opensettlers.entities.unit.Worker;
import fr.opensettlers.entities.building.StorageBuilding;

/**
 * Tests that training specialists consumes a tool from the warehouse stock.
 */
class StorageBuildingTest {

    private static final Coordinates POS = new Coordinates(0, 0);

    @Test
    void trainingASpecialistConsumesATool() {
        StorageBuilding warehouse = new StorageBuilding(0, POS,
                new HashMap<>(Map.of(ResourceType.TOOL, 1)));

        Worker worker = warehouse.spawnWorker(WorkerType.MINER, POS);

        assertNotNull(worker);
        assertEquals(WorkerType.MINER, worker.getType());
        assertTrue(worker.isCarriesTool());
        assertEquals(0, warehouse.getStoredResources().get(ResourceType.TOOL));
        assertEquals(98, warehouse.getStoredNeutralSettlers());
    }

    @Test
    void cannotTrainWithoutTools() {
        StorageBuilding warehouse = new StorageBuilding(0, POS, new HashMap<>());

        assertFalse(warehouse.canSpawnWorker());
        assertNull(warehouse.spawnWorker(WorkerType.BUILDER, POS));
        assertEquals(99, warehouse.getStoredNeutralSettlers());
    }

    @Test
    void cannotTrainWithoutSettlers() {
        StorageBuilding warehouse = new StorageBuilding(0, POS,
                new HashMap<>(Map.of(ResourceType.TOOL, 5)));
        warehouse.setStoredNeutralSettlers(0);

        assertFalse(warehouse.canSpawnWorker());
        assertNull(warehouse.spawnWorker(WorkerType.FARMER, POS));
        assertEquals(5, warehouse.getStoredResources().get(ResourceType.TOOL));
    }
}
