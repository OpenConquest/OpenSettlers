package fr.opensettlers.entities.resource;

import fr.opensettlers.utils.enums.ResourceType;
import lombok.Data;

/**
 * A slot holding a specific resource type with a capped quantity.
 */
@Data
public class ResourceSlot {
    /**
     * Resource type held by this slot.
     */
    private final ResourceType type;

    /**
     * Current quantity stored.
     */
    private int quantity;

    /**
     * Maximum quantity per slot.
     */
    private final int MAX_PER_SLOT = 5;

    /**
     * Adds one unit if the slot is not full.
     *
     * @return {@code true} if the resource was added
     */
    public boolean addResource() {
        if (quantity < MAX_PER_SLOT) {
            quantity++;
            return true;
        }
        return false;
    }

    /**
     * Removes one unit if the slot is not empty.
     *
     * @return {@code true} if the resource was removed
     */
    public boolean removeResource() {
        if (quantity > 0) {
            quantity--;
            return true;
        }
        return false;
    }
}
