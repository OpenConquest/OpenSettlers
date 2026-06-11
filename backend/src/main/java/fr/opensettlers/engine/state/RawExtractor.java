package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.ArrayList;

/**
 * Building that extracts raw resources from a map resource node.
 */
@Getter
public class RawExtractor extends ProductionBuilding {

    /**
     * The type of resource this building extracts. Null for buildings like Forester that do not produce inventory.
     */
    private final ResourceType extractedResource;

    /**
     * Initializes a new RawExtractor.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     * @param extractedResource the resource it produces
     */
    public RawExtractor(int playerId, Coordinates position, ResourceType extractedResource) {
        super(playerId, position);
        this.extractedResource = extractedResource;
        this.inputSlots = new ArrayList<>();
        if (extractedResource != null) {
            this.outputSlot = new ResourceSlot(extractedResource);
        }
    }

    /**
     * Extracts resources from the underlying resource node.
     */
    @Override
    public void produce() {
        if (this.outputSlot != null) {
            this.outputSlot.addResource();
        }
    }

    /**
     * Checks if extraction conditions are met.
     *
     * @return {@code true} if extraction conditions are met.
     */
    @Override
    public boolean canProduce() {
        // Map resource availability (trees, deposits, fish, plantable grass)
        // is checked by the ProductionSystem, which has access to the GameMap.
        if (this.outputSlot == null) {
            return true; // Forester has no output inventory
        }
        return this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
    }
}
