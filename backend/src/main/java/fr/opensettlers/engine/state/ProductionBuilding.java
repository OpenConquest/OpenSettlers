package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Building that produces goods from input resources with a defined production time.
 */
@Getter
public abstract class ProductionBuilding extends Building implements IProducer {
    /**
     * Input resource slots.
     */
    protected List<ResourceSlot> inputSlots;

    /**
     * Output resource slot.
     */
    protected ResourceSlot outputSlot;

    /** 
     * Target destinations assigned to resources currently stored in the output slot.
     * When an item is produced, it goes to outputSlot and a target is eventually assigned here.
     */
    protected List<UUID> outputDestinations = new java.util.ArrayList<>();

    /**
     * Initializes a new ProductionBuilding.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public ProductionBuilding(int playerId, Coordinates position) {
        super(playerId, position);
    }

    /**
     * Produces goods from available input resources.
     */
    @Override
    public abstract void produce();

    /**
     * Checks if the building can produce. Must be called before produce().
     *
     * @return boolean indicating if production is possible
     */
    @Override
    public abstract boolean canProduce();
}
