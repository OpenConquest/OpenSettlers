package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import lombok.Getter;

import java.util.UUID;

/** Building that extracts raw resources from a map resource node. */
@Getter
public abstract class RawExtractor extends ProductionBuilding {

    /**
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public RawExtractor(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position);
    }

    /** Extracts resources from the underlying resource node. */
    @Override
    public void produce() {
        // TODO
    }

    /** @return {@code true} if extraction conditions are met. */
    public boolean canExtract() {
        // TODO
        return true;
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
