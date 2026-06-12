package fr.opensettlers.state;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.Donkey;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.systems.TransportManager;
import fr.opensettlers.utils.*;
import lombok.Data;

import java.util.*;

/**
 * Stores all data in a game session and handles the game loop.
 */
@Data
public class GameState {
    /** Unique identifier of the game session. */
    private final UUID gameId;

    /** Unique identifiers of the players in the game. Should not contain duplicates. */
    private final List<UUID> playerIds;

    /** The road network managing flags, roads, and pathfinding. */
    private final RoadNetwork roadNetwork = new RoadNetwork();

    /** The transport manager handling resource logistics. */
    private final TransportManager transportManager = new TransportManager(roadNetwork);

    /** All building instances in the map. */
    private final List<Building> buildings = new ArrayList<>();
    /** All flag instances in the map. */
    private final List<Flag> flags = new ArrayList<>();
    /** All soldier instances in the map. */
    private final List<Soldier> soldiers = new ArrayList<>();
    /** All active worker units on the map. */
    private final List<Worker> workers = new ArrayList<>();

    /** All donkey units on the map (walking to or assisting main roads). */
    private final List<Donkey> donkeys = new ArrayList<>();

    /** Per-player exploration state (fog of war). */
    private final FogOfWarManager fogOfWar = new FogOfWarManager();

    /** The hex tile grid map using double height coordinates. */
    private Map<Coordinates, MapTile> mapTiles = new HashMap<>();

    /** Territory ownership manager, recalculated when military buildings change. */
    private final TerritoryManager territoryManager = new TerritoryManager();

    /** Global resource distribution priorities. */
    private final Map<ResourceType, List<BuildingName>> resourceDistributionPriorities = new HashMap<>() {{
        put(ResourceType.LOG, List.of(BuildingName.SAWMILL, BuildingName.MINE));
        put(ResourceType.COAL, List.of(BuildingName.FOUNDRY, BuildingName.ARMORY));
        put(ResourceType.WHEAT, List.of(BuildingName.MILL, BuildingName.BREWERY));
    }};

    /** Current tick since the start of the game. */
    private long currentTick = 0;

    /**
     * Initializes the map from a generated grid array.
     */
    public void setMapTilesFromGrid(MapTile[][] grid) {
        mapTiles.clear();
        for (MapTile[] row : grid) {
            for (MapTile tile : row) {
                if (tile != null) {
                    mapTiles.put(tile.getCoordinates(), tile);
                }
            }
        }
    }

    /**
     * Handles player quitting. Soldiers and buildings aren't removed and stay autonomous.
     * @param playerId The Unique identifier of the player to remove.
     */
    public void playerQuits(UUID playerId) {
        if (!playerIds.contains(playerId)) {
            throw new RuntimeException(String.format("Player %s does not exist", playerId));
        }

        playerIds.remove(playerId);
    }

    /** Makes the game loop go forward one tick. */
    public void tick() {
        currentTick++;

        // Sync flags to road network (in case new ones were added to the list but not the network)
        for (Flag flag : flags) {
            if (roadNetwork.getFlagById(flag.getId()) == null) {
                roadNetwork.addFlag(flag);
            }
        }

        // Flags (also unregister destroyed ones from the road network graph)
        for (Flag flag : flags) {
            if (flag.isDestroyed()) {
                roadNetwork.removeFlag(flag.getId());
            }
        }
        flags.removeIf(Flag::isDestroyed);

        // Buildings
        buildings.removeIf(Building::isDestroyed);

        // Soldiers
        soldiers.removeIf(Soldier::isDead);

        // Workers (clean up workers that have been dismissed/removed)
        workers.removeIf(w -> w.getState() == null);
    }

    /**
     * Returns the MapTile at the given double-height coordinates, or null if out of bounds.
     */
    public MapTile getTile(Coordinates coord) {
        return mapTiles.get(coord);
    }

    /**
     * Finds all tiles matching the given TileType within a hex BFS distance from a center position.
     * Results are in BFS order (closest first).
     */
    public List<MapTile> findTilesInRange(Coordinates center, int maxDistance, TileType type) {
        List<MapTile> result = new ArrayList<>();
        if (!mapTiles.containsKey(center)) return result;

        Set<Coordinates> visited = new HashSet<>();
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[]{center, 0});
        visited.add(center);

        while (!queue.isEmpty()) {
            Object[] curr = queue.poll();
            Coordinates coord = (Coordinates) curr[0];
            int dist = (Integer) curr[1];

            MapTile tile = mapTiles.get(coord);
            if (tile != null && tile.getType() == type && dist > 0) {
                result.add(tile);
            }

            if (dist < maxDistance) {
                for (Direction dir : Direction.values()) {
                    Coordinates neighborCoord = coord.neighbor(dir);
                    if (!visited.contains(neighborCoord) && mapTiles.containsKey(neighborCoord)) {
                        visited.add(neighborCoord);
                        queue.add(new Object[]{neighborCoord, dist + 1});
                    }
                }
            }
        }
        return result;
    }

    /**
     * Finds all tiles within range that have a NaturalResourceNode of the given type and are not depleted.
     * Results are in BFS order (closest first).
     */
    public List<MapTile> findResourceTilesInRange(Coordinates center, int maxDistance, ResourceType resourceType) {
        List<MapTile> result = new ArrayList<>();
        if (!mapTiles.containsKey(center)) return result;

        Set<Coordinates> visited = new HashSet<>();
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[]{center, 0});
        visited.add(center);

        while (!queue.isEmpty()) {
            Object[] curr = queue.poll();
            Coordinates coord = (Coordinates) curr[0];
            int dist = (Integer) curr[1];

            MapTile tile = mapTiles.get(coord);
            if (tile != null && dist > 0 && tile.getNaturalResource() != null
                    && tile.getNaturalResource().getType() == resourceType
                    && !tile.getNaturalResource().isDepleted()) {
                result.add(tile);
            }

            if (dist < maxDistance) {
                for (Direction dir : Direction.values()) {
                    Coordinates neighborCoord = coord.neighbor(dir);
                    if (!visited.contains(neighborCoord) && mapTiles.containsKey(neighborCoord)) {
                        visited.add(neighborCoord);
                        queue.add(new Object[]{neighborCoord, dist + 1});
                    }
                }
            }
        }
        return result;
    }

    /**
     * Finds all buildable (GRASS) tiles within range that have no natural resource and no building.
     * Results are in BFS order (closest first).
     */
    public List<MapTile> findEmptyGrassTilesInRange(Coordinates center, int maxDistance) {
        List<MapTile> result = new ArrayList<>();
        if (!mapTiles.containsKey(center)) return result;

        Set<Coordinates> visited = new HashSet<>();
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[]{center, 0});
        visited.add(center);

        while (!queue.isEmpty()) {
            Object[] curr = queue.poll();
            Coordinates coord = (Coordinates) curr[0];
            int dist = (Integer) curr[1];

            MapTile tile = mapTiles.get(coord);
            if (tile != null && dist > 0 && tile.getType() == TileType.GRASS
                    && tile.getNaturalResource() == null
                    && !hasBuildingAt(coord)) {
                result.add(tile);
            }

            if (dist < maxDistance) {
                for (Direction dir : Direction.values()) {
                    Coordinates neighborCoord = coord.neighbor(dir);
                    if (!visited.contains(neighborCoord) && mapTiles.containsKey(neighborCoord)) {
                        visited.add(neighborCoord);
                        queue.add(new Object[]{neighborCoord, dist + 1});
                    }
                }
            }
        }
        return result;
    }

    /**
     * Checks if at least one WATER tile exists within range.
     */
    public boolean hasWaterInRange(Coordinates center, int maxDistance) {
        return !findTilesInRange(center, maxDistance, TileType.WATER).isEmpty();
    }

    /**
     * Checks if there is a building at the given coordinate.
     */
    private boolean hasBuildingAt(Coordinates coord) {
        for (Building b : buildings) {
            if (b.getPosition().equals(coord)) {
                return true;
            }
        }
        return false;
    }
}
