package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * A ProcessingBuilding is a building that can process resources. It takes in resources as input and produces a different resource as output.
 */
@Getter
public abstract class ProcessingBuilding extends ProductionBuilding {
    /**
     * The recipe that defines the input and output resources for this building.
     */
    private final Recipe recipe;

    /**
     * Creates a new ProcessingBuilding.
     *
     * @param id       the unique identifier of the building
     * @param playerId the ID of the player who owns the building
     * @param position the coordinates of the building on the map
     * @param costs    the resources required to build this building
     * @param recipe   the recipe that defines the input and output resources for this building
     */
    public ProcessingBuilding(UUID id, int playerId, Coordinates position, Map<ResourceType, Integer> costs, Recipe recipe) {
        super(id, playerId, position, costs);
        this.recipe = recipe;
    }

    /**
     * Processes the input resources according to the recipe and produces the output resource. This method should be called when the building is ready to process resources.
     */
    public void process() {
        // TODO
    }
}
