package fr.opensettlers.engine.state;

import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;
import fr.opensettlers.engine.state.utils.TileType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The game world: a square grid of {@link MapTile} with per-tile territory ownership.
 *
 * <p>Tiles are stored in offset hexagonal coordinates (odd rows are shifted),
 * matching the layout produced by the map generator.</p>
 */
@Getter
public class GameMap {
    /** Width and height of the square tile grid. */
    private final int size;

    /** All tiles, indexed by [x][y]. */
    private final MapTile[][] tiles;

    /** Territory owner per tile (player ID), or {@value NO_OWNER} if unclaimed. */
    private final int[][] owners;

    /** Owner value of an unclaimed tile. */
    public static final int NO_OWNER = -1;

    /**
     * Wraps a generated tile grid into a game map.
     *
     * @param tiles the square tile grid
     */
    public GameMap(MapTile[][] tiles) {
        this.size = tiles.length;
        this.tiles = tiles;
        this.owners = new int[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                owners[x][y] = NO_OWNER;
            }
        }
    }

    /**
     * Returns the tile at the given grid position.
     *
     * @param x horizontal index
     * @param y vertical index
     * @return the tile, or {@code null} if out of bounds
     */
    public MapTile getTile(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) return null;
        return tiles[x][y];
    }

    /**
     * Returns the tile at the given coordinates.
     *
     * @param coords the grid coordinates
     * @return the tile, or {@code null} if out of bounds
     */
    public MapTile getTile(Coordinates coords) {
        return getTile((int) coords.getX(), (int) coords.getY());
    }

    /**
     * Returns the six hexagonal neighbors of a tile (odd-row offset layout).
     *
     * @param x horizontal index
     * @param y vertical index
     * @return the in-bounds neighboring tiles
     */
    public List<MapTile> getNeighbors(int x, int y) {
        int[][] offsets = (y % 2 == 0)
                ? new int[][]{{-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {-1, 1}, {0, 1}}
                : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {1, -1}, {0, 1}, {1, 1}};

        List<MapTile> neighbors = new ArrayList<>();
        for (int[] offset : offsets) {
            MapTile tile = getTile(x + offset[0], y + offset[1]);
            if (tile != null) {
                neighbors.add(tile);
            }
        }
        return neighbors;
    }

    /**
     * Returns all tiles within a euclidean radius of a center position.
     *
     * @param center the center coordinates
     * @param radius the search radius in tiles
     * @return the tiles within the radius, center tile included
     */
    public List<MapTile> getTilesWithinRadius(Coordinates center, int radius) {
        List<MapTile> result = new ArrayList<>();
        int cx = (int) center.getX();
        int cy = (int) center.getY();
        for (int x = Math.max(0, cx - radius); x <= Math.min(size - 1, cx + radius); x++) {
            for (int y = Math.max(0, cy - radius); y <= Math.min(size - 1, cy + radius); y++) {
                if (Math.hypot(x - cx, y - cy) <= radius) {
                    result.add(tiles[x][y]);
                }
            }
        }
        return result;
    }

    /**
     * Finds the closest tile to a center matching a predicate within a radius.
     *
     * @param center    the search center
     * @param radius    the maximum search radius
     * @param predicate the condition the tile must satisfy
     * @return the closest matching tile, or {@code null} if none found
     */
    public MapTile findClosestTile(Coordinates center, int radius, Predicate<MapTile> predicate) {
        MapTile best = null;
        double bestDist = Double.MAX_VALUE;
        for (MapTile tile : getTilesWithinRadius(center, radius)) {
            if (!predicate.test(tile)) continue;
            double dist = Math.hypot(
                    tile.getCoordinates().getX() - center.getX(),
                    tile.getCoordinates().getY() - center.getY());
            if (dist < bestDist) {
                bestDist = dist;
                best = tile;
            }
        }
        return best;
    }

    /**
     * Finds the closest tile holding a non-depleted natural resource of the given type.
     *
     * @param center the search center
     * @param radius the maximum search radius
     * @param type   the resource type to look for
     * @return the closest matching tile, or {@code null} if none found
     */
    public MapTile findClosestResourceTile(Coordinates center, int radius, ResourceType type) {
        return findClosestTile(center, radius, tile ->
                tile.getNaturalResource() != null
                        && tile.getNaturalResource().getType() == type
                        && !tile.getNaturalResource().isDepleted());
    }

    /**
     * Returns the territory owner of a tile.
     *
     * @param coords the tile coordinates
     * @return the owning player ID, or {@value NO_OWNER} if unclaimed or out of bounds
     */
    public int getOwner(Coordinates coords) {
        int x = (int) coords.getX();
        int y = (int) coords.getY();
        if (x < 0 || x >= size || y < 0 || y >= size) return NO_OWNER;
        return owners[x][y];
    }

    /**
     * Claims all tiles within a radius for a player (e.g. around a military building).
     * Already-claimed enemy tiles closer to their own military buildings are overwritten,
     * matching the simple "latest claim wins" model.
     *
     * @param center   the center of the claimed zone
     * @param radius   the territory radius in tiles
     * @param playerId the claiming player ID
     */
    public void claimTerritory(Coordinates center, int radius, int playerId) {
        for (MapTile tile : getTilesWithinRadius(center, radius)) {
            owners[(int) tile.getCoordinates().getX()][(int) tile.getCoordinates().getY()] = playerId;
        }
    }

    /**
     * Checks whether a tile is buildable by a player: correct terrain and owned territory.
     *
     * @param coords   the tile coordinates
     * @param playerId the player attempting to build
     * @return {@code true} if the player can build on this tile
     */
    public boolean canBuild(Coordinates coords, int playerId) {
        MapTile tile = getTile(coords);
        return tile != null && tile.isBuildable() && getOwner(coords) == playerId;
    }
}
