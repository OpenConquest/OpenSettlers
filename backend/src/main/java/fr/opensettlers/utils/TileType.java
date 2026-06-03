package fr.opensettlers.utils;

/**
 * Enumeration of map tile terrain types.
 */
public enum TileType {
    /** Grassland terrain, suitable for most buildings. */
    GRASS,

    /** Water terrain, impassable for land units. */
    WATER,

    /** Mountain terrain, source of ore deposits. */
    MOUNTAIN,

    /** Forest terrain, source of wood. */
    FOREST,

    /** Stony terrain, source of stone. */
    STONE,

    /** Cultivated field terrain, used for farming. */
    FIELD,

    /** Desert terrain, dry and unbuildable. */
    DESERT,

    /** Hilly terrain, uneven. */
    HILLS
}
