package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

public abstract class RawExtractor extends ProductionBuilding {

    public RawExtractor(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs) {
        super(id, playerId, position, costs);
    }

    public void extract() {
        // TODO
    }

    public boolean canExtract() {
        // TODO
        return true;
    }
}
