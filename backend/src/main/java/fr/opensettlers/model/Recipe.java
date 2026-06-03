package fr.opensettlers.model;

import java.util.Map;

public class Recipe {
    private Map<ResourceType, Integer> input;
    private ResourceType output;

    public boolean canProcess() {
        // TODO
        return true;
    }

    public void consume() {
        // TODO
    }
}
