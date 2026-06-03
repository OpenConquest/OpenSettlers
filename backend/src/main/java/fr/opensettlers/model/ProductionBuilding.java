package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductionBuilding extends Building {
    private final int productionTime = 5;
    private List<ResourceSlot> inputSlots;
    private ResourceSlot outputSlots;
    public ProductionBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs) {
        super(id, playerId, position, costs, new Flag(UUID.randomUUID(), playerId, position));
    }
    public void produce() {
        // TODO
    }

}
