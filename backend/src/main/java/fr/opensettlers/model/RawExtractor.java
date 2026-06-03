package fr.opensettlers.model;

import fr.opensettlers.utils.Coordinates;
import lombok.Getter;

import java.util.UUID;

/**
 * A RawExtractor is a building that can extract raw resources from the map. It can be built on specific resource nodes and will produce resources over time.
 */
@Getter
public abstract class RawExtractor extends ProductionBuilding {

    /**
     * Creates a new RawExtractor.
     *
     * @param id       the unique identifier of the building
     * @param playerId the ID of the player who owns the building
     * @param position the coordinates of the building on the map
     */
    public RawExtractor(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position);
    }

    /**
     * Extracts the resources from the resource node. This method should be called when the building is ready to extract resources.
     */
    public void extract() {
        // TODO
    }

    /**
     * Checks if the building can extract resources. This should check if the building is built on a valid resource node and if it has the necessary conditions to extract resources.
     *
     * @return true if the building can extract resources, false otherwise
     */
    public boolean canExtract() {
        // TODO
        return true;
    }
}
