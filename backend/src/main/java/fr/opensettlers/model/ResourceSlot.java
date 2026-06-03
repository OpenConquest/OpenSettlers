package fr.opensettlers.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The ResourceSlot class represents a slot for storing a specific type of resource. It contains the type of resource, the quantity of that resource currently stored in the slot, and the maximum quantity that can be stored in the slot.
 */
@AllArgsConstructor
@Getter
public class ResourceSlot {
    /**
     * The type of resource stored in this slot. This is an enum that represents the different types of resources available in the game (e.g., wood, stone, food).
     */
    private final ResourceType type;

    /**
     * The quantity of the resource currently stored in this slot. This is an integer that represents how much of the resource is currently in the slot.
     */
    private int quantity;

    /**
     * The maximum quantity of the resource that can be stored in this slot. This is an integer that represents the maximum capacity of the slot for the specific resource type.
     */
    private int MAX_PER_SLOT = 5;

    /**
     * Adds one unit of the resource to the slot. This method should check if adding the resource would exceed the maximum capacity of the slot and only add the resource if it does not exceed the capacity.
     *
     * @return true if the resource was added successfully, false if adding the resource would exceed the maximum capacity of the slot
     */
    public boolean addResource() {
        if (quantity < MAX_PER_SLOT) {
            quantity++;
            return true;
        }
        return false;
    }
}
