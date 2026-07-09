package fr.opensettlers.utils.enums;

/**
 * Roles that a settler can take when assigned to a building or construction site.
 */
public enum WorkerType {
    /** Ground worker who flattens the construction site. */
    TERRASSIER,

    /** Builder who constructs the walls of the site. */
    BUILDER,

    /** Carrier transporting goods along roads. */
    CARRIER,

    // Specialists for production buildings

    /** Lumberjack who fells trees. */
    WOODCUTTER,

    /** Forester who plants saplings. */
    FORESTER,

    /** Stone cutter working a quarry. */
    QUARRYMAN,

    /** Miner extracting iron, coal, gold or stone. */
    MINER,

    /** Fisherman fishing water tiles. */
    FISHERMAN,

    /** Hunter tracking wild game. */
    HUNTER,

    /** Carpenter sawing logs into planks. */
    CARPENTER,

    /** Farmer growing and harvesting grain. */
    FARMER,

    /** Miller grinding wheat into flour. */
    MILLER,

    /** Baker baking bread. */
    BAKER,

    /** Pig farmer raising pigs. */
    PIG_BREEDER,

    /** Slaughterhouse worker producing meat. */
    BUTCHER,

    /** Donkey farm worker breeding donkeys. */
    DONKEY_BREEDER,

    /** Geologist surveying mountains for ore deposits. */
    GEOLOGIST,

    /** Scout exploring the fog of war around a flag. */
    SCOUT,

    /** Brewer brewing beer. */
    BREWER,

    /** Smelter smelting iron into steel. */
    SMELTER,

    /** Weapons smith forging swords and shields. */
    SMITH,

    /** Minter striking gold coins. */
    MINTER,

    /** Tool maker producing tools. */
    METALWORKER,

    /** Generic helper (e.g. catapult operator). */
    HELPER;
}
