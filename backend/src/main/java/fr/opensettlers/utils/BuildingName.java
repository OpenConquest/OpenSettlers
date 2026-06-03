package fr.opensettlers.utils;

/**
 * Enumeration of all building types available in the game.
 */
public enum BuildingName {
    /** Main building; starting point and resource storage hub. */
    HEADQUARTERS,

    /** Storage-only building with no production capability. */
    WAREHOUSE,

    /** Produces wood logs from nearby trees. */
    WOODCUTTER,

    /** Plants new trees to replenish forests. */
    FORESTER,

    /** Produces stone without requiring input resources. */
    QUARRY,

    /** Produces ore without requiring input resources. */
    MINE,

    /** Produces wheat without requiring input resources. */
    FARM,

    /** Produces fish without requiring input resources. */
    FISHING_HUT,

    /** Produces water without requiring input resources. */
    WATER_WELL,

    /** Produces planks from logs. */
    SAWMILL,

    /** Produces flour from wheat. */
    MILL,

    /** Produces bread from flour and water. */
    BAKERY,

    /** Produces steel from iron and coal. */
    FOUNDRY,

    /** Produces swords from steel and coal. */
    ARMORY,

    /** Produces beer from wheat and water. */
    BREWERY,

    /** Military building that extends territory. */
    GUARD_HOUSE,

    /** Military building that extends territory. */
    WATCH_TOWER,

    /** Military building that extends territory. */
    CASTLE,

    /** Military building that extends territory. */
    BARRACKS;

    /**
     * Returns a user-friendly display name for this building.
     *
     * @return the display name
     */
    @Override
    public String toString() {
        return switch (this) {
            case HEADQUARTERS -> "Headquarters";
            case WAREHOUSE -> "Warehouse";
            case WOODCUTTER -> "Woodcutter";
            case FORESTER -> "Forester";
            case QUARRY -> "Quarry";
            case MINE -> "Mine";
            case FARM -> "Farm";
            case FISHING_HUT -> "Fishing Hut";
            case WATER_WELL -> "Water Well";
            case SAWMILL -> "Sawmill";
            case MILL -> "Mill";
            case BAKERY -> "Bakery";
            case FOUNDRY -> "Foundry";
            case ARMORY -> "Armory";
            case BREWERY -> "Brewery";
            case GUARD_HOUSE -> "Guard House";
            case WATCH_TOWER -> "Watch Tower";
            case CASTLE -> "Castle";
            case BARRACKS -> "Barracks";
        };
    }
}
