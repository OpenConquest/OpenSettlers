package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Getter;

import java.util.UUID;

/** Building that extracts raw resources from a map resource node. */
@Getter
public class RawExtractor extends ProductionBuilding {

    /** The type of resource this building extracts. Null for buildings like Forester that do not produce inventory. */
    private final fr.opensettlers.utils.ResourceType extractedResource;

    /**
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param position map coordinates
     * @param extractedResource the resource it produces
     */
    public RawExtractor(UUID id, int playerId, Coordinates position, ResourceType extractedResource) {
        super(id, playerId, position);
        this.extractedResource = extractedResource;
        this.inputSlots = new java.util.ArrayList<>();
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
    public boolean canExtract() {
        // Forester (no output slot) can always "extract" (plant) if map allows
        if (this.outputSlot == null) {
            return true; // TODO: check map if there's space to plant a tree
        }
        return this.outputSlot.getQuantity() < this.outputSlot.getMAX_PER_SLOT();
        // TODO: check map for resource node availability (e.g., fish in water, tree nearby)
    }

    /**
     * Calls the production method according to the production frequency.
     */
    @Override
    public void tick() {
        if (this.productionCooldown <= 0) {
            if (this.canExtract()) {
                this.produce();
                this.productionCooldown = PRODUCTION_TIME;
            }
        } else {
            this.productionCooldown--;
        }
    }
}
