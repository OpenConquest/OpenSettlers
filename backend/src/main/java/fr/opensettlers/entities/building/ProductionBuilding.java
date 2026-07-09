package fr.opensettlers.entities.building;

import fr.opensettlers.utils.Coordinates;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import fr.opensettlers.entities.unit.Worker;
import fr.opensettlers.entities.resource.ResourceSlot;

/**
 * Building that produces goods from input resources with a defined production time.
 */
@Getter
public abstract class ProductionBuilding extends Building implements IProducer {
    /**
     * Cooldown remaining before the next production cycle can occur.
     */
    @Setter
    protected int productionCooldown = 0;

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
    protected List<UUID> outputDestinations = new ArrayList<>();

    /** Specialist worker occupying this building. */
    @Setter
    private Worker occupant;

    /** Productivity percentage of the building (0 to 100). */
    @Setter
    private int productivity = 0;

    /**
     * Whether the player has paused this building. A paused building keeps its
     * occupant but produces nothing and accepts no new input deliveries, mirroring
     * the "stop production" control of the original game.
     */
    @Setter
    private boolean productionPaused = false;

    /** Ticks spent in waiting state to track the 10-second decay. */
    @Setter
    private int waitingTicks = 0;

    /**
     * Initializes a new ProductionBuilding.
     *
     * @param playerId owning player ID
     * @param position map coordinates
     */
    public ProductionBuilding(int playerId, Coordinates position) {
        super(playerId, position);
    }

    /** Produces goods from available input resources. */
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
