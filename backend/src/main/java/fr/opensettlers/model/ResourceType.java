package fr.opensettlers.model;

public enum ResourceType {
    /**
     * Wood log. Naturally available and harvestable raw resource.
     */
    LOG,

    /**
     * Wood planks, produced from logs.
     */
    PLANK,

    /**
     * Stone. Naturally available raw resource..
     */
    STONE,

    /**
     * Raw iron. Naturally available raw resource.
     */
    IRON,

    /**
     * Steel, produced from iron.
     */
    STEEL,

    /**
     * Coal. Naturally available raw resource.
     */
    COAL,

    /**
     * Wheat. Harvestable raw resource.
     */
    WHEAT,

    /**
     * Wheat flour, produced from wheat
     */
    FLOUR,

    /**
     * Bread, produced from water and flour.
     */
    BREAD,

    /**
     * Fish. Naturally available raw resource. Counts as food.
     */
    FISH,

    /**
     * Beer, produced from water and wheat. Used to recruit soldiers.
     */
    BEER,

    /**
     * Sword, produced from steel. Used to recruit soldiers.
     */
    SWORD,

    /**
     * Water. Naturally available raw resource.
     */
    WATER
}
