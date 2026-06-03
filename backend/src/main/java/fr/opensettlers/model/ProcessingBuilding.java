package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public abstract class ProcessingBuilding extends ProductionBuilding {
    private final Recipe recipe;

    public ProcessingBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs, Recipe recipe) {
        super(id, playerId, position, costs);
        this.recipe = recipe;
    }

    public void process() {
        // TODO
    }
}
