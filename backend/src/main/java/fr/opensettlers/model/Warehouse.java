package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

public class Warehouse extends StorageBuilding {
    public Warehouse(int playerId, Coordinates position) {
        super(UUID.randomUUID(), playerId, position, Map.of(), Map.of());
    }
}
