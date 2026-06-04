package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

/**
 * Storage-only building with no production capabilities.
 */
public class Warehouse extends StorageBuilding {
    /**
     * Initializes a new Warehouse.
     *
     * @param playerId owning player ID
     * @param position map position
     */
    public Warehouse(int playerId, Coordinates position) {
        super(UUID.randomUUID(), playerId, position, Map.of());
    }
}
