package fr.opensettlers.state;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.Donkey;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Tracks the fog of war: the set of tiles each player has explored.
 * <p>
 * Exploration is permanent (Settlers II style — terrain once seen stays
 * known). Vision is projected each tick by the player's buildings, flags and
 * units. Serialization filters what each player receives based on this state.
 */
public class FogOfWarManager {

    /** Explored tiles per player. Exploration only ever grows. */
    private final Map<Integer, Set<Coordinates>> exploredTiles = new HashMap<>();

    /**
     * Reveals the surroundings of every vision source of every player.
     *
     * @param state the current game state
     */
    public void update(GameState state) {
        for (Building b : state.getBuildings()) {
            if (b.isDestroyed()) continue;
            reveal(state, b.getPlayerId(), b.getPosition(), visionRadius(b));
        }
        for (Flag f : state.getRoadNetwork().getAllFlags()) {
            if (f.isDestroyed()) continue;
            reveal(state, f.getPlayerId(), f.getCoordinates(), GameConfig.VISION_FLAG_RADIUS);
        }
        for (Soldier s : state.getSoldiers()) {
            if (s.isDead()) continue;
            reveal(state, s.getPlayerId(), s.getPosition(), GameConfig.VISION_UNIT_RADIUS);
        }
        for (Worker w : state.getWorkers()) {
            if (w.getPosition() == null) continue;
            reveal(state, w.getPlayerId(), w.getPosition(), GameConfig.VISION_UNIT_RADIUS);
        }
        for (Donkey d : state.getDonkeys()) {
            if (d.getPosition() == null) continue;
            reveal(state, d.getPlayerId(), d.getPosition(), GameConfig.VISION_UNIT_RADIUS);
        }
    }

    /**
     * Returns the set of tiles explored by a player.
     *
     * @param playerId the player
     * @return the explored coordinates (empty set if the player saw nothing yet)
     */
    public Set<Coordinates> getExplored(int playerId) {
        return exploredTiles.getOrDefault(playerId, Set.of());
    }

    /**
     * Checks whether a player has explored the given tile.
     *
     * @param playerId the player
     * @param coord    the tile coordinate
     * @return {@code true} if the tile was explored
     */
    public boolean isExplored(int playerId, Coordinates coord) {
        return getExplored(playerId).contains(coord);
    }

    /**
     * Returns the vision radius of a building: territory radius plus a margin
     * for military buildings and headquarters, a fixed radius otherwise.
     *
     * @param building the vision source
     * @return the vision radius in hex distance
     */
    private int visionRadius(Building building) {
        if (building instanceof MilitaryBuilding mb) {
            return mb.getTerritoryRadius() + GameConfig.VISION_TERRITORY_MARGIN;
        }
        if (building instanceof StorageBuilding && building.getName() == BuildingName.HEADQUARTERS) {
            return GameConfig.HEADQUARTERS_TERRITORY_RADIUS + GameConfig.VISION_TERRITORY_MARGIN;
        }
        return GameConfig.VISION_BUILDING_RADIUS;
    }

    /**
     * Adds all existing map tiles within the radius of a center to the
     * player's explored set (BFS over the hex grid).
     *
     * @param state    the current game state (for map bounds)
     * @param playerId the player gaining vision
     * @param center   the vision source position
     * @param radius   the vision radius
     */
    private void reveal(GameState state, int playerId, Coordinates center, int radius) {
        Set<Coordinates> explored = exploredTiles.computeIfAbsent(playerId, k -> new HashSet<>());

        Set<Coordinates> visited = new HashSet<>();
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[]{center, 0});
        visited.add(center);
        if (state.getMapTiles().containsKey(center)) {
            explored.add(center);
        }

        while (!queue.isEmpty()) {
            Object[] curr = queue.poll();
            Coordinates coord = (Coordinates) curr[0];
            int dist = (Integer) curr[1];

            if (dist >= radius) continue;
            for (Direction dir : Direction.values()) {
                Coordinates neighbor = coord.neighbor(dir);
                if (visited.add(neighbor) && state.getMapTiles().containsKey(neighbor)) {
                    explored.add(neighbor);
                    queue.add(new Object[]{neighbor, dist + 1});
                }
            }
        }
    }
}
