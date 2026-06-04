package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.Data;

/** A harvestable natural resource node on the map (e.g. tree, stone deposit). */
@Data
public class NaturalResourceNode {

    /** Resource type provided by this node. */
    private final ResourceType type;

    /** Current harvestable quantity. */
    private int quantity;

    /** Maximum capacity (set to the initial quantity at creation). */
    private final int maxCapacity;

    /**
     * @param type            resource type
     * @param initialQuantity initial (and max) quantity
     */
    public NaturalResourceNode(ResourceType type, int initialQuantity) {
        this.type = type;
        this.quantity = initialQuantity;
        this.maxCapacity = initialQuantity;
    }

    /**
     * Harvests one unit if available.
     *
     * @return {@code true} if a unit was harvested
     */
    public boolean harvest() {
        if (this.quantity > 0) {
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

    /** @return {@code true} if no resources remain. */
    public boolean isDepleted() {
        return this.quantity <= 0;
    }
}