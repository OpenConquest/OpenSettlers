package fr.opensettlers.systems;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.service.GameActions;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.TileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * System driving the built-in computer opponents.
 *
 * <p>Each computer player (see {@link GameState#getAiPlayers()}) takes one
 * decision every {@link GameConfig#AI_DECISION_INTERVAL} ticks. The behaviour is
 * deliberately simple but produces a self-sufficient settlement: the AI grows a
 * wood and stone economy, automatically connects every new building to its road
 * network, expands its borders with military buildings, and orders attacks on
 * nearby enemy buildings once it has a standing army.</p>
 *
 * <p>All actions go through {@link GameActions}, the same validated entry point
 * used by human players, so the AI obeys exactly the same rules.</p>
 */
public class AiSystem implements ISystem {

    /**
     * The order in which an AI player tries to erect buildings. Repeated entries
     * request several copies; the AI builds the first entry whose realized count
     * is still below the number of times it has appeared so far.
     */
    private static final List<BuildingName> BUILD_ORDER = List.of(
            BuildingName.WOODCUTTER,
            BuildingName.FORESTER,
            BuildingName.SAWMILL,
            BuildingName.QUARRY,
            BuildingName.GUARD_HOUSE,
            BuildingName.WOODCUTTER,
            BuildingName.SAWMILL,
            BuildingName.WATER_WELL,
            BuildingName.FARM,
            BuildingName.MILL,
            BuildingName.BAKERY,
            BuildingName.BARRACKS);

    /**
     * Lets each computer player act when its decision interval elapses.
     *
     * @param state the active game session state
     */
    @Override
    public void process(GameState state) {
        if (state.getAiPlayers().isEmpty() || state.isOver()) {
            return;
        }
        if (state.getCurrentTick() % GameConfig.AI_DECISION_INTERVAL != 0) {
            return;
        }
        for (int playerId : state.getAiPlayers()) {
            if (!state.getEliminatedPlayers().contains(playerId)) {
                act(state, playerId);
            }
        }
    }

    /**
     * Performs a single AI decision: order an attack if a target is in reach,
     * otherwise extend the economy by one building.
     *
     * @param state    the current game state
     * @param playerId the computer player taking the decision
     */
    private void act(GameState state, int playerId) {
        if (tryAttack(state, playerId)) {
            return;
        }
        tryBuildNext(state, playerId);
    }

    /**
     * Builds the next missing building in {@link #BUILD_ORDER} and connects it to
     * the road network.
     *
     * @param state    the current game state
     * @param playerId the computer player
     * @return {@code true} if a building was placed
     */
    private boolean tryBuildNext(GameState state, int playerId) {
        Map<BuildingName, Integer> counts = countBuildings(state, playerId);
        Map<BuildingName, Integer> seen = new HashMap<>();

        for (BuildingName name : BUILD_ORDER) {
            int target = seen.merge(name, 1, Integer::sum);
            if (counts.getOrDefault(name, 0) < target) {
                if (buildAndConnect(state, playerId, name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds a valid spot for the given building, places it, and links its flag to
     * the nearest flag already in the network.
     *
     * @param state    the current game state
     * @param playerId the computer player
     * @param name     the building type to erect
     * @return {@code true} if the building was placed
     */
    private boolean buildAndConnect(GameState state, int playerId, BuildingName name) {
        Coordinates spot = findBuildSpot(state, playerId, name);
        if (spot == null || !GameActions.placeBuilding(state, playerId, name, spot)) {
            return false;
        }
        connectToNetwork(state, playerId, spot);
        return true;
    }

    /**
     * Locates a buildable tile for a building type within the player's territory,
     * close to one of their storage buildings.
     *
     * @param state    the current game state
     * @param playerId the computer player
     * @param name     the building type to place
     * @return a valid position, or {@code null} if none was found
     */
    private Coordinates findBuildSpot(GameState state, int playerId, BuildingName name) {
        for (Building b : state.getBuildings()) {
            if (b.isDestroyed() || b.getPlayerId() != playerId || !(b instanceof StorageBuilding)) {
                continue;
            }
            Coordinates origin = b.getPosition();

            if (name.isMine()) {
                for (MapTile tile : state.findTilesInRange(origin, GameConfig.AI_BUILD_SEARCH_RADIUS, TileType.MOUNTAIN)) {
                    Coordinates c = tile.getCoordinates();
                    if (GameActions.isPlacementValid(state, name, c, playerId)) {
                        return c;
                    }
                }
                continue;
            }

            List<MapTile> candidates =
                    state.findEmptyGrassTilesInRange(origin, GameConfig.AI_BUILD_SEARCH_RADIUS);
            for (MapTile tile : candidates) {
                Coordinates c = tile.getCoordinates();
                if (GameActions.isPlacementValid(state, name, c, playerId)) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Connects a freshly built building's flag to the closest existing flag of
     * the same player via a short hex path.
     *
     * @param state     the current game state
     * @param playerId  the computer player
     * @param buildingPos the position of the new building (its flag sits there)
     */
    private void connectToNetwork(GameState state, int playerId, Coordinates buildingPos) {
        Flag newFlag = null;
        Flag nearest = null;
        int minDist = Integer.MAX_VALUE;

        for (Flag flag : state.getRoadNetwork().getAllFlags()) {
            if (flag.isDestroyed() || flag.getPlayerId() != playerId) {
                continue;
            }
            if (flag.getCoordinates().equals(buildingPos)) {
                newFlag = flag;
                continue;
            }
            int dist = flag.getCoordinates().distanceTo(buildingPos);
            if (dist < minDist) {
                minDist = dist;
                nearest = flag;
            }
        }

        if (newFlag == null || nearest == null) {
            return;
        }
        List<Coordinates> path = hexPath(state, playerId, buildingPos, nearest.getCoordinates());
        if (path != null) {
            GameActions.linkFlags(state, newFlag.getId(), nearest.getId(), path);
        }
    }

    /**
     * Greedily walks the hex grid from one tile to another, staying on walkable,
     * owned tiles, and returns the intermediate coordinates (endpoints excluded).
     *
     * @param state    the current game state
     * @param playerId the player whose territory the road must stay within
     * @param from     start tile (a flag position)
     * @param to       destination tile (a flag position)
     * @return the intermediate path, or {@code null} if no short owned path exists
     */
    private List<Coordinates> hexPath(GameState state, int playerId, Coordinates from, Coordinates to) {
        List<Coordinates> path = new ArrayList<>();
        Coordinates current = from;

        for (int step = 0; step < GameConfig.AI_MAX_ROAD_LENGTH; step++) {
            if (current.distanceTo(to) <= 1) {
                return path;
            }
            Coordinates best = null;
            int bestDist = Integer.MAX_VALUE;
            for (Direction dir : Direction.values()) {
                Coordinates next = current.neighbor(dir);
                if (next.equals(from) || path.contains(next)) {
                    continue;
                }
                MapTile tile = state.getTile(next);
                if (tile == null || !tile.isWalkable()) {
                    continue;
                }
                if (state.getTerritoryManager().getOwnerAt(next) != playerId) {
                    continue;
                }
                int dist = next.distanceTo(to);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = next;
                }
            }
            if (best == null) {
                return null;
            }
            path.add(best);
            current = best;
        }
        return null;
    }

    /**
     * Orders an attack when an enemy military building or warehouse sits close to
     * one of the player's military buildings with spare attackers.
     *
     * @param state    the current game state
     * @param playerId the computer player
     * @return {@code true} if an attack was ordered
     */
    private boolean tryAttack(GameState state, int playerId) {
        boolean hasArmy = state.getBuildings().stream()
                .anyMatch(b -> b instanceof MilitaryBuilding mb && !mb.isDestroyed()
                        && mb.getPlayerId() == playerId && mb.getSoldiers().size() > 1);
        if (!hasArmy) {
            return false;
        }

        for (Building enemy : state.getBuildings()) {
            if (enemy.isDestroyed() || enemy.getPlayerId() == playerId) {
                continue;
            }
            if (!(enemy instanceof MilitaryBuilding) && !(enemy instanceof StorageBuilding)) {
                continue;
            }
            boolean inReach = state.getBuildings().stream()
                    .anyMatch(b -> b instanceof MilitaryBuilding mb && !mb.isDestroyed()
                            && mb.getPlayerId() == playerId
                            && mb.getPosition().distanceTo(enemy.getPosition()) <= GameConfig.AI_ATTACK_RANGE);
            if (inReach && GameActions.attack(state, playerId, enemy.getId()) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the player's buildings (including construction sites) per type.
     *
     * @param state    the current game state
     * @param playerId the computer player
     * @return a map from building type to count
     */
    private Map<BuildingName, Integer> countBuildings(GameState state, int playerId) {
        Map<BuildingName, Integer> counts = new HashMap<>();
        for (Building b : state.getBuildings()) {
            if (!b.isDestroyed() && b.getPlayerId() == playerId && b.getName() != null) {
                counts.merge(b.getName(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
