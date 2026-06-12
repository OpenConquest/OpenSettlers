package fr.opensettlers.utils;

/**
 * Roles that a settler can take when assigned to a building or construction site.
 */
public enum WorkerType {
    TERRASSIER,   // Ground worker to flatten the construction site
    BUILDER,      // Builder to construct the walls of the site
    CARRIER,      // Carrier transporting goods on roads

    // Specialists for production buildings
    WOODCUTTER,   // Lumberjack
    FORESTER,     // Forester/planter
    QUARRYMAN,    // Stone cutter
    MINER,        // Miner (Iron, Coal, Gold, Stone)
    FISHERMAN,    // Fisherman
    HUNTER,       // Hunter tracking wild game
    CARPENTER,    // Carpenter
    FARMER,       // Farmer
    MILLER,       // Miller
    BAKER,        // Baker
    PIG_BREEDER,  // Pig farmer
    BUTCHER,      // Slaughterhouse worker
    DONKEY_BREEDER, // Donkey farm worker
    GEOLOGIST,    // Surveys mountains for ore deposits
    BREWER,       // Brewer
    SMELTER,      // Smelter
    SMITH,        // Weapons smith (swords and shields)
    MINTER,       // Gold coin minter
    METALWORKER,  // Tool maker
    HELPER;       // Generic helper (catapult operator)
}
