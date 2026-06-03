package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * The ProductionBuilding is a building that can produce resources or goods. It has input slots for resources and output slots for the produced goods. It also has a production time that determines how long it takes to produce the goods.
 */
@Getter
public class ProductionBuilding extends Building {
    /**
     * The production time in seconds. This is the time it takes for the building to produce the goods after it has received the required input resources.
     */
    private final int productionTime = 5;

    /**
     * The input slots for the resources required to produce the goods. Each slot can hold a certain amount of a specific resource type.
     */
    private List<ResourceSlot> inputSlots;

    /**
     * The output slots for the produced goods. Each slot can hold a certain amount of a specific resource type.
     */
    private ResourceSlot outputSlots;

    /**
     * Creates a new ProductionBuilding.
     *
     * @param id       the unique identifier of the building
     * @param playerId the ID of the player who owns the building
     * @param position the coordinates of the building on the map
     */
    public ProductionBuilding(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position, new Flag(UUID.randomUUID(), playerId, position));
    }

    /**
     * Produces the goods according to the input resources and the production time. This method should be called when the building is ready to produce goods.
     */
    public void produce() {
        // TODO
    }

}
