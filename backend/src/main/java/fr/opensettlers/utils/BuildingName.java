package fr.opensettlers.utils;


public enum BuildingName {
    /**
     * The Headquarters is the main building of a player. It is where the player starts the game and where they can store their resources.
     */
    HEADQUARTERS,

    /**
     * A Warehouse is a building that can store resources. It has no production capabilities and is used solely for storage.
     */
    WAREHOUSE,

    /**
     * A Woodcutter is a building that produces wood logs. It takes in no resources and produces log as output.
     */
    WOODCUTTER,

    /**
     * A Forester
     */
    FORESTER,

    /**
     * A Quarry is a building that produces stone. It takes in no resources and produces stone as output.
     */
    QUARRY,

    /**
     * A Mine is a building that produces ore. It takes in no resources and produces ore as output.
     */
    MINE,

    /**
     * A Farm is a building that produces food. It takes in no resources and produces wheat as output.
     */
    FARM,

    /**
     * A Fishing Hut is a building that produces fish. It takes in no resources and produces fish as output.
     */
    FISHING_HUT,

    /**
     * A Water Well is a building that produces water. It takes in no resources and produces water as output.
     */
    WATER_WELL,

    /**
     * A Sawmill is a building that produces planks. It takes in logs as input and produces planks as output.
     */
    SAWMILL,

    /**
     * A Mill is a building that produces flour. It takes in wheat as input and produces flour as output.
     */
    MILL,

    /**
     * A Bakery is a building that produces bread. It takes in flour and water as inputs and produces bread as output.
     */
    BAKERY,

    /**
     * A Foundry is a building that produces steel. It takes in iron and coal as input and produces steel as output.
     */
    FOUNDRY,

    /**
     * An Armory is a building that produces swords. It takes in steel and coal as input and produces swords as output.
     */
    ARMORY,

    /**
     * A Brewery is a building that produces beer. It takes in wheat and water as input and produces beer as output.
     */
    BREWERY,

    /**
     * A Guard House is a military building to extend the territory of a player.
     */
    GUARD_HOUSE,

    /**
     * A Watch Tower is a military building to extend the territory of a player.
     */
    WATCH_TOWER,

    /**
     * A Castle is a military building to extend the territory of a player.
     */
    CASTLE,

    /**
     * A Barracks is a military building to extend the territory of a player.
     */
    BARRACKS;

    /**
     * Returns the string representation of the building name. This is used for display purposes and should return a user-friendly name for each building type.
     *
     * @return the string representation of the building name
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
