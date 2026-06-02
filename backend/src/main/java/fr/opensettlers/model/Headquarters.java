package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;

import java.util.Map;
import java.util.UUID;

public class Headquarters extends StorageBuilding {
    public Headquarters(int playerId, Coordinates position, Map<ResourceType, Integer> startingResources) {
        super(UUID.randomUUID(), playerId, position, Map.of(), startingResources);
    }
}
