package fr.opensettlers.utils.enums;

/**
 * Enumeration of all building types available in the game.
 */
public enum BuildingName {
    /** Main building; starting point, resource storage hub and last line of defense. */
    HEADQUARTERS,

    /** Storage-only building with no production capability. */
    WAREHOUSE,

    /** Produces wood logs from nearby trees. */
    WOODCUTTER,

    /** Plants new trees to replenish forests. */
    FORESTER,

    /** Produces stone without requiring input resources. */
    QUARRY,

    /** Extracts granite (stone) from mountain deposits; consumes food. */
    GRANITE_MINE,

    /** Extracts coal from mountain deposits; consumes food. */
    COAL_MINE,

    /** Extracts iron ore from mountain deposits; consumes food. */
    IRON_MINE,

    /** Extracts gold from mountain deposits; consumes food. */
    GOLD_MINE,

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

    /** Reveals a wide area around itself without projecting territory. */
    LOOKOUT_TOWER,

    /** Military building that extends territory. */
    GUARD_HOUSE,

    /** Military building that extends territory. */
    WATCH_TOWER,

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
     * Checks whether this building is one of the four mountain mines.
     *
     * @return {@code true} for granite, coal, iron and gold mines
     */
    public boolean isMine() {
        return this == GRANITE_MINE || this == COAL_MINE || this == IRON_MINE || this == GOLD_MINE;
    }

    /**
     * Returns the ore a mine extracts, matching the Settlers II mine types.
     *
     * @return the mined resource, or {@code null} if this building is not a mine
     */
    public ResourceType minedResource() {
        return switch (this) {
            case GRANITE_MINE -> ResourceType.STONE;
            case COAL_MINE -> ResourceType.COAL;
            case IRON_MINE -> ResourceType.IRON;
            case GOLD_MINE -> ResourceType.GOLD;
            default -> null;
        };
    }

    /**
     * Returns the construction site grade this building requires, mirroring the
     * Settlers II hut/house/castle building sizes.
     *
     * @return the required site size
     */
    public SiteSize siteSize() {
        return switch (this) {
            case WOODCUTTER, FORESTER, QUARRY, FISHING_HUT, HUNTERS_HUT, WATER_WELL,
                 BARRACKS, GUARD_HOUSE, LOOKOUT_TOWER -> SiteSize.HUT;
            case SAWMILL, MILL, BAKERY, BREWERY, ARMORY, MINT, METALWORKS, FOUNDRY,
                 SLAUGHTERHOUSE, WATCH_TOWER, SHIPYARD, CATAPULT -> SiteSize.HOUSE;
            case FARM, PIG_FARM, DONKEY_BREEDER, WAREHOUSE, FORTRESS, HARBOR,
                 HEADQUARTERS -> SiteSize.CASTLE;
            case GRANITE_MINE, COAL_MINE, IRON_MINE, GOLD_MINE -> SiteSize.MINE;
        };
    }

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
            case GRANITE_MINE -> "Granite Mine";
            case COAL_MINE -> "Coal Mine";
            case IRON_MINE -> "Iron Mine";
            case GOLD_MINE -> "Gold Mine";
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
            case LOOKOUT_TOWER -> "Lookout Tower";
            case GUARD_HOUSE -> "Guard House";
            case WATCH_TOWER -> "Watch Tower";
            case FORTRESS -> "Fortress";
            case BARRACKS -> "Barracks";
            case CATAPULT -> "Catapult";
            case HARBOR -> "Harbor";
            case SHIPYARD -> "Shipyard";
        };
    }
}
