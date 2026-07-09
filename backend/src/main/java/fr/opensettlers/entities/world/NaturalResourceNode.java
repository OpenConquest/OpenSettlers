package fr.opensettlers.entities.world;

import fr.opensettlers.utils.enums.ResourceType;
import lombok.Data;

/**
 * A harvestable natural resource node on the map (e.g. tree, stone deposit).
 * Planted nodes (saplings, sown wheat) start with a growth delay and only
 * become harvestable once mature, as in The Settlers II.
 */
@Data
public class NaturalResourceNode {

    /**
     * Resource type provided by this node.
     */
    private final ResourceType type;

    /**
     * Current harvestable quantity.
     */
    private int quantity;

    /**
     * Maximum capacity (set to the initial quantity at creation).
     */
    private final int maxCapacity;

    /**
     * Ticks remaining before the node is mature enough to harvest.
     * Zero for naturally spawned resources (ore veins, wild trees…).
     */
    private int growthTicks;

    /**
     * Initializes a mature NaturalResourceNode (no growth delay).
     *
     * @param type            resource type
     * @param initialQuantity initial (and max) quantity
     */
    public NaturalResourceNode(ResourceType type, int initialQuantity) {
        this(type, initialQuantity, 0);
    }

    /**
     * Initializes a NaturalResourceNode that must grow before being harvestable.
     *
     * @param type            resource type
     * @param initialQuantity initial (and max) quantity
     * @param growthTicks     ticks before the node matures (0 = already mature)
     */
    public NaturalResourceNode(ResourceType type, int initialQuantity, int growthTicks) {
        this.type = type;
        this.quantity = initialQuantity;
        this.maxCapacity = initialQuantity;
        this.growthTicks = Math.max(0, growthTicks);
    }

    /**
     * Advances the node's growth by one tick.
     */
    public void grow() {
        if (growthTicks > 0) {
            growthTicks--;
        }
    }

    /**
     * Checks whether the node finished growing.
     *
     * @return {@code true} if the node is mature
     */
    public boolean isMature() {
        return growthTicks <= 0;
    }

    /**
     * Checks whether the node can be harvested right now.
     *
     * @return {@code true} if the node is mature and not depleted
     */
    public boolean isHarvestable() {
        return isMature() && !isDepleted();
    }

    /**
     * Harvests one unit if available and mature.
     *
     * @return {@code true} if a unit was harvested
     */
    public boolean harvest() {
        if (isHarvestable()) {
            this.quantity--;
            return true;
        }
        return false;
    }

    /**
     * Replenishes one unit if below max capacity.
     *
     * @return {@code true} if a unit was replenished
     */
    public boolean replenish() {
        if (this.quantity < this.maxCapacity) {
            this.quantity++;
            return true;
        }
        return false;
    }

    /**
     * Checks if the resource node is depleted.
     *
     * @return {@code true} if no resources remain.
     */
    public boolean isDepleted() {
        return this.quantity <= 0;
    }
}
