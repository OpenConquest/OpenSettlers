package fr.opensettlers.engine.state.utils;

/**
 * Hexagonal directions for doubled hex coordinates.
 * See <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">redblobgames</a>.
 */
public enum Direction {
    /** North (0, -2). */
    NORTH,

    /** North-east (1, -1). */
    NORTHEAST,

    /** South-east (1, 1). */
    SOUTHEAST,

    /** South (0, 2). */
    SOUTH,

    /** South-west (-1, 1). */
    SOUTHWEST,

    /** North-west (-1, -1). */
    NORTHWEST,
}
