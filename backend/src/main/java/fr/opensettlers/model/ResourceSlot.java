package fr.opensettlers.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResourceSlot {
    private final ResourceType type;
    private int quantity;
    private int MAX_PER_SLOT = 5;

    public boolean addResource() {
        if (quantity < MAX_PER_SLOT) {
            quantity++;
            return true;
        }
        return false;
    }
}
