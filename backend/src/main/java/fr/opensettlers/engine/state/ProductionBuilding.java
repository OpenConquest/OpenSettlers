package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/** Building that produces goods from input resources with a defined production time. */
@Getter
public abstract class ProductionBuilding extends Building {
    /** Production time in seconds. */
    protected static final int PRODUCTION_TIME = 5;

    /** The production cooldown. Allows production when it reaches 0. */
    protected int productionCooldown = 0;

    /** Input resource slots. */
    protected List<ResourceSlot> inputSlots;

    /** Output resource slot. */
    protected ResourceSlot outputSlot;

    /**
     * @param id       unique identifier
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public ProductionBuilding(UUID id, int playerId, Coordinates position) {
        super(id, playerId, position, new Flag(UUID.randomUUID(), playerId, position));
    }

    /** Produces goods from available input resources. */
    public abstract void produce();
}
