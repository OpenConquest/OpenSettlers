package fr.opensettlers.engine.state.utils;

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
    MINER,        // Miner (Iron, Coal, Stone)
    FISHERMAN,    // Fisherman
    CARPENTER,    // Carpenter
    FARMER,       // Farmer
    MILLER,       // Miller
    BAKER,        // Baker
    BREWER,       // Brewer
    SMELTER,      // Smelter
    SMITH;        // Weapons/Tools smith
}
