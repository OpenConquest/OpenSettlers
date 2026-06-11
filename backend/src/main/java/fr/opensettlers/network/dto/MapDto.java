package fr.opensettlers.network.dto;

import java.util.List;

/**
 * Static map description sent once to each client when it connects.
 *
 * @param type  message discriminator, always {@code "MAP"}
 * @param size  width/height of the square tile grid
 * @param tiles all map tiles
 */
public record MapDto(String type, int size, List<TileDto> tiles) {
    /**
     * A single map tile.
     *
     * @param x         horizontal index
     * @param y         vertical index
     * @param tileType  terrain type
     * @param elevation tile elevation
     * @param resource  natural resource type, or {@code null} if none
     * @param quantity  remaining resource quantity, or {@code null} if none
     */
    public record TileDto(int x, int y, String tileType, int elevation, String resource, Integer quantity) {}
}
