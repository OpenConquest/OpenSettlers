package fr.opensettlers.utils.enums;

/**
 * Enumeration of all resource types, both raw and processed.
 */
public enum ResourceType {
    /** Raw wood log. */
    LOG(null, false),

    /** Planks, produced from logs. */
    PLANK(null, false),

    /** Stone, mined as ore. */
    STONE(OreType.STONE, false),

    /** Raw iron, mined as ore. */
    IRON(OreType.IRON, false),

    /** Steel, smelted from iron. */
    STEEL(null, false),

    /** Coal, mined as ore. */
    COAL(OreType.COAL, false),

    /** Gold, mined as ore. */
    GOLD(OreType.GOLD, false),

    /** Gold coin, minted from gold and coal; promotes soldiers. */
    COIN(null, false),

    /** Wheat, harvested from farms. */
    WHEAT(null, false),

    /** Flour, milled from wheat. */
    FLOUR(null, false),

    /** Bread, baked from flour and water. Feeds miners. */
    BREAD(null, true),

    /** Fish, a raw food resource. Feeds miners. */
    FISH(null, true),

    /** Pig, bred from wheat and water in a pig farm. */
    PIG(null, false),

    /** Meat, from hunted game or slaughtered pigs. Feeds miners. */
    MEAT(null, true),

    /** Donkey, bred from wheat and water; walks to level-2 roads to assist carriers. */
    DONKEY(null, false),

    /** Beer, brewed from wheat and water. Required to recruit soldiers. */
    BEER(null, false),

    /** Sword, forged from steel. Required to recruit soldiers. */
    SWORD(null, false),

    /** Shield, forged from steel. Required to recruit soldiers. */
    SHIELD(null, false),

    /** Tool, crafted from iron and planks. Required to train specialists. */
    TOOL(null, false),

    /** Water, a raw resource. */
    WATER(null, false);

    /** Associated ore type, or {@code null} if not an ore. */
    private final OreType oreType;

    /** Whether this resource can feed miners. */
    private final boolean food;

    /**
     * @param oreType associated ore type, or {@code null}
     * @param food    whether this resource is edible food
     */
    ResourceType(OreType oreType, boolean food) {
        this.oreType = oreType;
        this.food = food;
    }

    /**
     * Checks whether this resource is an ore.
     *
     * @return {@code true} if this resource has an associated ore type
     */
    public boolean isOre() {
        return this.oreType != null;
    }

    /**
     * Checks whether this resource is edible food (used by mines).
     *
     * @return {@code true} if this resource can feed workers
     */
    public boolean isFood() {
        return this.food;
    }
}
