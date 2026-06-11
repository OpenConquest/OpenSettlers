package fr.opensettlers.engine;

import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.MilitaryBuilding;
import fr.opensettlers.engine.state.StorageBuilding;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.Direction;

import java.util.*;

/**
 * Manages territorial control based on military buildings.
 * <p>
 * Each active (non-destroyed) military building and headquarters projects
 * a circular territory around itself (hex distance <= radius). The territory
 * of a player is the union of all their military building territories.
 * <p>
 * Players can only place buildings, flags, and roads within their territory.
 */
public class TerritoryManager {

    /**
     * Cached territory per player: playerId → set of controlled coordinates.
     */
    private final Map<Integer, Set<Coordinates>> playerTerritories = new HashMap<>();

    /**
     * Cached owner per tile: coordinate → playerId (-1 if no owner).
     */
    private final Map<Coordinates, Integer> tileOwners = new HashMap<>();

    /**
     * Recalculates all territories from scratch based on current buildings.
     * Should be called after any military building is built, captured, or destroyed.
     *
     * @param state the current game state
     */
    public void recalculate(GameState state) {
        playerTerritories.clear();
        tileOwners.clear();

        // Collect all territory-projecting buildings (military + headquarters)
        List<TerritorySource> sources = new ArrayList<>();

        for (Building b : state.getBuildings()) {
            if (b.isDestroyed()) continue;

            if (b instanceof MilitaryBuilding mb) {
                sources.add(new TerritorySource(mb.getPlayerId(), mb.getPosition(), mb.getTerritoryRadius()));
            } else if (b instanceof StorageBuilding sb) {
                // Headquarters projects territory too
                sources.add(new TerritorySource(sb.getPlayerId(), sb.getPosition(),
                        GameConfig.HEADQUARTERS_TERRITORY_RADIUS));
            }
        }

        // For each source, expand territory via BFS up to its radius
        for (TerritorySource src : sources) {
            Set<Coordinates> tiles = expandTerritory(state, src.position, src.radius);

            playerTerritories.computeIfAbsent(src.playerId, k -> new HashSet<>()).addAll(tiles);

            for (Coordinates c : tiles) {
                // If contested (multiple players), the closest building wins.
                // For simplicity, last-write-wins here — can be refined later.
                // In practice, Settlers 2 uses closest-building-wins.
                Integer currentOwner = tileOwners.get(c);
                if (currentOwner == null) {
                    tileOwners.put(c, src.playerId);
                } else if (currentOwner != src.playerId) {
                    // Resolve: closest military building to this tile wins
                    int existingDist = closestMilitaryDistance(state, c, currentOwner);
                    int newDist = src.position.distanceTo(c);
                    if (newDist < existingDist) {
                        tileOwners.put(c, src.playerId);
                    }
                }
            }
        }
    }

    /**
     * Returns the owner of a given tile, or -1 if unclaimed.
     *
     * @param coord the tile coordinate
     * @return the player ID of the owner, or -1
     */
    public int getOwnerAt(Coordinates coord) {
        return tileOwners.getOrDefault(coord, -1);
    }

    /**
     * Checks if a player can build at the given coordinates.
     * A player can build only within their own territory.
     *
     * @param playerId the player ID
     * @param coord    the target coordinate
     * @return {@code true} if the player controls this tile
     */
    public boolean canBuild(int playerId, Coordinates coord) {
        Set<Coordinates> territory = playerTerritories.get(playerId);
        return territory != null && territory.contains(coord);
    }

    /**
     * Returns the full set of tiles controlled by a player.
     *
     * @param playerId the player ID
     * @return set of controlled coordinates (empty if none)
     */
    public Set<Coordinates> getTerritory(int playerId) {
        return playerTerritories.getOrDefault(playerId, Set.of());
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Expands territory from a center using BFS up to maxRadius hex distance.
     * Only includes tiles that actually exist on the map.
     */
    private Set<Coordinates> expandTerritory(GameState state, Coordinates center, int maxRadius) {
        Set<Coordinates> result = new HashSet<>();
        Queue<Object[]> queue = new LinkedList<>();
        Set<Coordinates> visited = new HashSet<>();

        queue.add(new Object[]{center, 0});
        visited.add(center);
        result.add(center);

        while (!queue.isEmpty()) {
            Object[] curr = queue.poll();
            Coordinates coord = (Coordinates) curr[0];
            int dist = (Integer) curr[1];

            if (dist < maxRadius) {
                for (Direction dir : Direction.values()) {
                    Coordinates neighbor = coord.neighbor(dir);
                    if (!visited.contains(neighbor) && state.getMapTiles().containsKey(neighbor)) {
                        visited.add(neighbor);
                        result.add(neighbor);
                        queue.add(new Object[]{neighbor, dist + 1});
                    }
                }
            }
        }
        return result;
    }

    /**
     * Finds the closest military building distance from a coordinate for a given player.
     */
    private int closestMilitaryDistance(GameState state, Coordinates coord, int playerId) {
        int minDist = Integer.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (b.isDestroyed() || b.getPlayerId() != playerId) continue;
            if (b instanceof MilitaryBuilding || b instanceof StorageBuilding) {
                int dist = b.getPosition().distanceTo(coord);
                if (dist < minDist) minDist = dist;
            }
        }
        return minDist;
    }

    /** Internal record for territory calculation. */
    private record TerritorySource(int playerId, Coordinates position, int radius) {}
}
