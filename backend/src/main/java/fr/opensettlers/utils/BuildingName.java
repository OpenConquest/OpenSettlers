package fr.opensettlers.utils;


public enum BuildingName {
    HEADQUARTERS,
    WAREHOUSE,
    WOODCUTTER,
    FORESTER,
    QUARRY,
    MINE,
    FARM,
    FISHING_HUT,
    WATER_WELL,
    SAWMILL,
    MILL,
    BAKERY,
    FOUNDRY,
    ARMORY,
    BREWERY,
    GUARD_HOUSE,
    WATCH_TOWER,
    CASTLE,
    BARRACKS;

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
