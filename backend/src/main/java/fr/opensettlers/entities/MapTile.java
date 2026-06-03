package fr.opensettlers.entities;

import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.TileType;
import lombok.Getter;
import lombok.Setter;

/** A single tile on the game map with a type, elevation, and optional resource node. */
@Getter
@Setter
public class MapTile {
    /** Tile coordinates on the map. */
    private final Coordinates coordinates;

    /** Terrain type (may change during the game, e.g. forest → grass). */
    private TileType type;

    /** Tile elevation; affects road connectivity and movement. */
    private int elevation;

    /** Natural resource on this tile, or {@code null} if none. */
    private NaturalResourceNode naturalResource;

    /** Max allowed elevation difference for road connections. */
    public static final int MAX_ROAD_ELEVATION_DELTA = 2;

    /**
     * @param coordinates tile position
     * @param initialType terrain type
     * @param elevation   tile elevation
     */
    public MapTile(Coordinates coordinates, TileType initialType, int elevation) {
        this.coordinates = coordinates;
        this.type = initialType;
        this.elevation = elevation;
        this.naturalResource = null;
    }

    /**
     * Checks whether a road can connect to the given neighbor based on elevation delta.
     *
     * @param neighbor the adjacent tile
     * @return {@code true} if the elevation difference is within the allowed limit
     */
    public boolean canConnectRoadTo(MapTile neighbor) {
        if (neighbor == null) return false;

        int delta = Math.abs(this.elevation - neighbor.getElevation());
        return delta <= MAX_ROAD_ELEVATION_DELTA;
    }

    /**
     * Harvests one unit from the resource node; depleted forest tiles become grass.
     *
     * @return {@code true} if a resource was harvested
     */
    public boolean harvestResource() {
        if (this.naturalResource == null) {
            return false;
        }

        boolean success = this.naturalResource.harvest();

        if (success && this.naturalResource.isDepleted() && this.type == TileType.FOREST) {
            this.type = TileType.GRASS;
            this.naturalResource = null;
        }

        return success;
    }

    /**
     * Plants a new tree on grass, or replenishes an existing forest resource.
     *
     * @param newTree the resource node for the new tree
     * @return {@code true} if the tree was planted or replenished
     */
    public boolean replantTree(NaturalResourceNode newTree) {
        if (this.type == TileType.GRASS && this.naturalResource == null) {
            this.type = TileType.FOREST;
            this.naturalResource = newTree;
            return true;
        }
        else if (this.type == TileType.FOREST && this.naturalResource != null) {
            return this.naturalResource.replenish();
        }

        return false;
    }

    /**
     * @return {@code true} if the tile supports building construction (grass or field)
     */
    public boolean isBuildable() {
        return this.type == TileType.GRASS;
    }

    /** @return {@code true} if units can traverse this tile (not water or mountain) */
    public boolean isWalkable() {
        return this.type != TileType.WATER && this.type != TileType.MOUNTAIN;
    }
}