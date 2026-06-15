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

    /** Extracts ore from mountain deposits; consumes food. */
    MINE,

    /** Produces wheat without requiring input resources. */
    FARM,

    /** Produces fish without requiring input resources. */
    FISHING_HUT,

    /** Hunts wild game to produce meat. */
    HUNTERS_HUT,

    /** Produces water without requiring input resources. */
    WATER_WELL,

    /** Produces planks from logs. */
    SAWMILL,

    /** Produces flour from wheat. */
    MILL,

    /** Produces bread from flour and water. */
    BAKERY,

    /** Breeds pigs from wheat and water. */
    PIG_FARM,

    /** Produces meat from pigs. */
    SLAUGHTERHOUSE,

    /** Breeds donkeys from wheat and water to assist carriers on main roads. */
    DONKEY_BREEDER,

    /** Produces steel from iron and coal. */
    FOUNDRY,

    /** Produces swords and shields from steel and coal. */
    ARMORY,

    /** Mints gold coins from gold and coal. */
    MINT,

    /** Crafts tools from iron and planks. */
    METALWORKS,

    /** Produces beer from wheat and water. */
    BREWERY,

    /** Military building that extends territory. */
    GUARD_HOUSE,

    /** Military building that extends territory. */
    WATCH_TOWER,

    /** Military building that extends territory. */
    CASTLE,

    /** Largest military building, with the widest territory projection. */
    FORTRESS,

    /** Military building that extends territory. */
    BARRACKS,

    /** Siege building that throws stones at enemy military buildings. */
    CATAPULT,

    /** Coastal warehouse able to launch and receive sea expeditions. */
    HARBOR,

    /** Coastal building that enables a player to send colonization expeditions. */
    SHIPYARD;

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
            case HUNTERS_HUT -> "Hunter's Hut";
            case WATER_WELL -> "Water Well";
            case SAWMILL -> "Sawmill";
            case MILL -> "Mill";
            case BAKERY -> "Bakery";
            case PIG_FARM -> "Pig Farm";
            case SLAUGHTERHOUSE -> "Slaughterhouse";
            case DONKEY_BREEDER -> "Donkey Breeder";
            case FOUNDRY -> "Foundry";
            case ARMORY -> "Armory";
            case MINT -> "Mint";
            case METALWORKS -> "Metalworks";
            case BREWERY -> "Brewery";
            case GUARD_HOUSE -> "Guard House";
            case WATCH_TOWER -> "Watch Tower";
            case CASTLE -> "Castle";
            case FORTRESS -> "Fortress";
            case BARRACKS -> "Barracks";
            case CATAPULT -> "Catapult";
            case HARBOR -> "Harbor";
            case SHIPYARD -> "Shipyard";
        };
    }
}
