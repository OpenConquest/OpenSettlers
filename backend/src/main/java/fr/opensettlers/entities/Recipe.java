package fr.opensettlers.entities;

import fr.opensettlers.utils.ResourceType;
import lombok.Value;

import java.util.Map;

/**
 * Defines a resource conversion: required inputs and the produced output.
 */
@Value
public class Recipe {
    /**
     * All known recipes, keyed by output resource type mapping to required inputs and quantities.
     */
    public static final Map<ResourceType, Map<ResourceType, Integer>> RECIPES = Map.ofEntries(
            Map.entry(ResourceType.PLANK, Map.of(ResourceType.LOG, 1)),
            Map.entry(ResourceType.FLOUR, Map.of(ResourceType.WHEAT, 1)),
            Map.entry(ResourceType.BREAD, Map.of(ResourceType.FLOUR, 1, ResourceType.WATER, 1)),
            Map.entry(ResourceType.BEER, Map.of(ResourceType.WHEAT, 1, ResourceType.WATER, 1)),
            Map.entry(ResourceType.PIG, Map.of(ResourceType.WHEAT, 1, ResourceType.WATER, 1)),
            Map.entry(ResourceType.DONKEY, Map.of(ResourceType.WHEAT, 1, ResourceType.WATER, 1)),
            Map.entry(ResourceType.MEAT, Map.of(ResourceType.PIG, 1)),
            Map.entry(ResourceType.STEEL, Map.of(ResourceType.IRON, 1, ResourceType.COAL, 1)),
            Map.entry(ResourceType.SWORD, Map.of(ResourceType.STEEL, 1, ResourceType.COAL, 1)),
            Map.entry(ResourceType.SHIELD, Map.of(ResourceType.STEEL, 1, ResourceType.COAL, 1)),
            Map.entry(ResourceType.COIN, Map.of(ResourceType.GOLD, 1, ResourceType.COAL, 1)),
            Map.entry(ResourceType.TOOL, Map.of(ResourceType.IRON, 1, ResourceType.PLANK, 1))
    );
    /**
     * Required input resources and their quantities.
     */
    Map<ResourceType, Integer> input;

    /**
     * Output resource type produced.
     */
    ResourceType output;

    /** 
     * Checks if the recipe can be processed with the available slots.
     *
     * @param availableSlots the slots to check
     * @return {@code true} if all required inputs are available. 
     */
    public boolean canProcess(java.util.List<ResourceSlot> availableSlots) {
        if (input == null) return true; // No inputs required
        for (Map.Entry<ResourceType, Integer> requirement : input.entrySet()) {
            ResourceType requiredType = requirement.getKey();
            int requiredQuantity = requirement.getValue();
            
            ResourceSlot slot = availableSlots.stream()
                .filter(s -> s.getType() == requiredType)
                .findFirst()
                .orElse(null);
                
            if (slot == null || slot.getQuantity() < requiredQuantity) {
                return false;
            }
        }
        return true;
    }

    /** 
     * Consumes the required input resources from the provided slots.
     *
     * @param availableSlots the slots to consume from 
     */
    public void consume(java.util.List<ResourceSlot> availableSlots) {
        if (input == null) return;
        for (Map.Entry<ResourceType, Integer> requirement : input.entrySet()) {
            ResourceType requiredType = requirement.getKey();
            int requiredQuantity = requirement.getValue();
            
            ResourceSlot slot = availableSlots.stream()
                .filter(s -> s.getType() == requiredType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing required resource slot for " + requiredType));
                
            slot.setQuantity(slot.getQuantity() - requiredQuantity);
        }
    }
}
