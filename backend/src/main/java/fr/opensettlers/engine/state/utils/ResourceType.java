package fr.opensettlers.engine.state.utils;

/**
 * Enumeration of all resource types, both raw and processed.
 */
public enum ResourceType {
    /** Raw wood log. */
    LOG(null),

    /** Planks, produced from logs. */
    PLANK(null),

    /** Stone, mined as ore. */
    STONE(OreType.STONE),

    /** Raw iron, mined as ore. */
    IRON(OreType.IRON),

    /** Steel, smelted from iron. */
    STEEL(null),

    /** Coal, mined as ore. */
    COAL(OreType.COAL),

    /** Wheat, harvested from farms. */
    WHEAT(null),

    /** Flour, milled from wheat. */
    FLOUR(null),

    /** Bread, baked from flour and water. */
    BREAD(null),

    /** Fish, a raw food resource. */
    FISH(null),

    /** Beer, brewed from wheat and water. */
    BEER(null),

    /** Sword, forged from steel. */
    SWORD(null),

    /** Water, a raw resource. */
    WATER(null);

    /** Associated ore type, or {@code null} if not an ore. */
    private final OreType oreType;

    /** @param oreType associated ore type, or {@code null} */
    ResourceType(OreType oreType) {
        this.oreType = oreType;
    }

    /**
     * Checks whether this resource is an ore.
     *
     * @return {@code true} if this resource has an associated ore type
     */
    public boolean isOre() {
        return this.oreType != null;
    }
}
