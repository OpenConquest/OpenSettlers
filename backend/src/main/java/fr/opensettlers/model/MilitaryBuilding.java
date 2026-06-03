package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MilitaryBuilding extends Building{
    private List<Soldier> soldiers;
    private int maxCapacity;


    public MilitaryBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs) {
        super(id, playerId, position, costs, new Flag(UUID.randomUUID(), playerId, position));
    }

    public void recruit() {
        // TODO
    }
    public void expandTerritory() {
        // TODO
    }
}
