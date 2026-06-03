package fr.opensettlers.utils;

public enum ResourceType {
    /**
     * Wood log. Naturally available and harvestable raw resource.
     */
    LOG(null),

    /**
     * Wood planks, produced from logs.
     */
    PLANK(null),

    /**
     * Stone. Naturally available raw resource.
     */
    STONE(OreType.STONE),

    /**
     * Raw iron. Naturally available raw resource.
     */
    IRON(OreType.IRON),

    /**
     * Steel, produced from iron.
     */
    STEEL(null),

    /**
     * Coal. Naturally available raw resource.
     */
    COAL(OreType.COAL),

    /**
     * Wheat. Harvestable raw resource.
     */
    WHEAT(null),

    /**
     * Wheat flour, produced from wheat
     */
    FLOUR(null),

    /**
     * Bread, produced from water and flour.
     */
    BREAD(null),

    /**
     * Fish. Naturally available raw resource. Counts as food.
     */
    FISH(null),

    /**
     * Beer, produced from water and wheat. Used to recruit soldiers.
     */
    BEER(null),

    /**
     * Sword, produced from steel. Used to recruit soldiers.
     */
    SWORD(null),

    /**
     * Water. Naturally available raw resource.
     */
    WATER(null);

    /**
     * The type of ore associated with this resource, if it is an ore. This is null for non-ore resources and is set to the corresponding OreType for ore resources.
     */
    private final OreType oreType;

    /**
     * Constructor for ResourceType enum. This constructor is used to initialize the oreType field for ore resources and set it to null for non-ore resources.
     *
     * @param oreType the type of ore associated with this resource, or null if this resource is not an ore
     */
    ResourceType(OreType oreType) {
        this.oreType = oreType;
    }

    /**
     * Gets the type of ore associated with this resource, if it is an ore. This method returns null for non-ore resources and returns the corresponding OreType for ore resources.
     *
     * @return the type of ore associated with this resource, or null if this resource is not an ore
     */
    public OreType getOreType() {
        return this.oreType;
    }

    /**
     * Checks if this resource is an ore. This method returns true if the oreType field is not null, indicating that this resource is an ore, and returns false if the oreType field is null, indicating that this resource is not an ore.
     *
     * @return true if this resource is an ore, false otherwise
     */
    public boolean isOre() {
        return this.oreType != null;
    }
}
