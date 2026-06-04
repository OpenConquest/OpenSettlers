package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.ArrayList;
import java.util.UUID;

/** Building that extracts raw resources from a map resource node. */
@Getter
public class RawExtractor extends ProductionBuilding {

    /** The type of resource this building extracts. Null for buildings like Forester that do not produce inventory. */
    private final ResourceType extractedResource;

    /**
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param position map coordinates
     * @param extractedResource the resource it produces
     */
    public RawExtractor(UUID id, int playerId, Coordinates position, ResourceType extractedResource) {
        super(id, playerId, position);
        this.extractedResource = extractedResource;
        this.inputSlots = new ArrayList<>();
        if (extractedResource != null) {
            this.outputSlot = new ResourceSlot(extractedResource);
        }
    }

    /** Extracts resources from the underlying resource node. */
    @Override
    public void produce() {
        if (this.outputSlot != null) {
            this.outputSlot.addResource();
        }
    }

    /** @return {@code true} if extraction conditions are met. */
    @Override
    public boolean canProduce() {
        // Forester (no output slot) can always "extract" (plant) if map allows
        if (this.outputSlot == null) {
            return true; // TODO: check map if there's space to plant a tree
        }
        return this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
        // TODO: check map for resource node availability (e.g., fish in water, tree nearby)
    }
}
