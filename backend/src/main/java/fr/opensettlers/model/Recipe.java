package fr.opensettlers.model;

import lombok.Value;

import java.util.Map;

/**
 * The Recipe class represents a recipe for processing resources. It contains the input resources required to produce an output resource.
 */
@Value
public class Recipe {
    /**
     * The input resources required to produce the output resource. This is a map where the key is the type of resource and the value is the amount of that resource required.
     */
    Map<ResourceType, Integer> input;

    /**
     * The output resource produced by this recipe. This is the type of resource that will be produced when the input resources are processed.
     */
    ResourceType output;

    /**
     * Checks if the recipe can be processed with the current input resources. This method should check if the required input resources are available in the building's input slots.
     *
     * @return true if the recipe can be processed, false otherwise
     */
    public boolean canProcess() {
        // TODO
        return true;
    }

    /**
     * Consumes the input resources according to the recipe. This method should be called when the building is ready to process resources and should update the quantities of the input resources in the building's input slots.
     */
    public void consume() {
        // TODO
    }
}
