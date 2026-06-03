package fr.opensettlers.entities;

import fr.opensettlers.utils.ResourceType;
import lombok.Value;

import java.util.Map;

/** Defines a resource conversion: required inputs and the produced output. */
@Value
public class Recipe {
    /** All known recipes, keyed by output resource type mapping to required inputs and quantities. */
    public static final Map<ResourceType, Map<ResourceType, Integer>> RECIPES = Map.ofEntries(
            Map.entry(ResourceType.PLANK, Map.of(ResourceType.LOG, 1)),
            Map.entry(ResourceType.FLOUR, Map.of(ResourceType.WHEAT, 1)),
            Map.entry(ResourceType.BREAD, Map.of(ResourceType.FLOUR, 1, ResourceType.WATER, 1)),
            Map.entry(ResourceType.BEER, Map.of(ResourceType.WHEAT, 1, ResourceType.WATER, 1)),
            Map.entry(ResourceType.STEEL, Map.of(ResourceType.IRON, 1, ResourceType.COAL, 1)),
            Map.entry(ResourceType.SWORD, Map.of(ResourceType.STEEL, 1, ResourceType.COAL, 1))
    );
    /** Required input resources and their quantities. */
    Map<ResourceType, Integer> input;

    /** Output resource type produced. */
    ResourceType output;

    /**
     * @return {@code true} if all required inputs are available
     */
    public boolean canProcess() {
        // TODO
        return true;
    }

    /** Consumes the required input resources. */
    public void consume() {
        // TODO
    }
}
