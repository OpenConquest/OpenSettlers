package fr.opensettlers.utils;

/**
 * Hexagonal directions for double heighted double widthed coordinates. Refer to
 * <a href="https://www.redblobgames.com/grids/hexagons/#neighbors-doubled">this article</a> for more information.
 */
public enum Direction {
    /**
     * North direction. (0, -2) unit vector.
     */
    NORTH,

    /**
     * North-east direction. (1, -1) unit vector.
     */
    NORTHEAST,

    /**
     * South-east direction. (1, 1) unit vector.
     */
    SOUTHEAST,

    /**
     * South direction. (0, 2) unit vector.
     */
    SOUTH,

    /**
     * South-west direction. (-1, 1) unit vector.
     */
    SOUTHWEST,

    /**
     * North-west direction. (-1, -1) unit vector.
     */
    NORTHWEST,
}
