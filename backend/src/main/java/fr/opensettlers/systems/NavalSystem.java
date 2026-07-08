package fr.opensettlers.systems;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.BuildingFactory;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.Ship;
import fr.opensettlers.entities.ShipyardBuilding;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.service.GameActions;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.ResourceType;
import fr.opensettlers.utils.ShipState;
import fr.opensettlers.utils.TileType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * System running the sea routes of The Settlers II: colonization expeditions.
 *
 * <p>A player who owns at least one operational {@link ShipyardBuilding} may
 * launch an expedition from any coastal {@link BuildingName#HARBOR} that has
 * stockpiled enough building materials. A {@link Ship} is created, sails across
 * the water along a precomputed path, and — once it reaches an unclaimed shore —
 * founds a brand-new harbor there, extending the player's domain to distant
 * coasts and islands.</p>
 *
 * <p>To keep the simulation readable, each player runs at most one expedition at
 * a time.</p>
 */
public class NavalSystem implements ISystem {

    /** Initial stock granted to a harbor founded by an expedition. */
    private static final Map<ResourceType, Integer> COLONY_STOCK = Map.ofEntries(
            Map.entry(ResourceType.PLANK, 10),
            Map.entry(ResourceType.STONE, 6),
            Map.entry(ResourceType.TOOL, 10),
            Map.entry(ResourceType.FISH, 6),
            Map.entry(ResourceType.SWORD, 2),
            Map.entry(ResourceType.SHIELD, 2),
            Map.entry(ResourceType.BEER, 2));

    /**
     * Advances every ship at sea and launches new expeditions where possible.
     *
     * @param state the active game session state
     */
    @Override
    public void process(GameState state) {
        advanceShips(state);
        launchExpeditions(state);
    }

    /**
     * Moves each sailing ship one tile along its path when its cooldown elapses,
     * landing the expedition when it reaches the destination shore.
     *
     * @param state the current game state
     */
    private void advanceShips(GameState state) {
        for (Ship ship : state.getShips()) {
            if (ship.getState() != ShipState.SAILING) {
                if (ship.getState() == ShipState.LANDED) {
                    foundColony(state, ship);
                }
                continue;
            }
            if (ship.getMoveCooldown() > 0) {
                ship.setMoveCooldown(ship.getMoveCooldown() - 1);
                continue;
            }
            ship.setMoveCooldown(GameConfig.SHIP_MOVE_TICKS);

            if (ship.getPathIndex() < ship.getPath().size() - 1) {
                ship.setPathIndex(ship.getPathIndex() + 1);
                ship.setPosition(ship.getPath().get(ship.getPathIndex()));
            } else {
                ship.setState(ShipState.LANDED);
            }
        }
    }

    /**
     * Founds a new harbor at the ship's landing tile, if it is still free, and
     * retires the ship.
     *
     * @param state the current game state
     * @param ship  the landed expedition ship
     */
    private void foundColony(GameState state, Ship ship) {
        Coordinates landing = ship.getLandingTile();
        if (isColonizable(state, landing)) {
            StorageBuilding harbor = (StorageBuilding) BuildingFactory.createBuilding(
                    BuildingName.HARBOR, ship.getPlayerId(), landing, state);
            COLONY_STOCK.forEach((type, qty) -> harbor.getStoredResources().merge(type, qty, Integer::sum));
            state.getBuildings().add(harbor);
            state.getFlags().add(harbor.getAttachedFlag());
            state.getRoadNetwork().addFlag(harbor.getAttachedFlag());
            state.getTerritoryManager().recalculate(state);
        }
        ship.setState(ShipState.FINISHED);
    }

    /**
     * Launches one expedition per eligible player whose harbor has gathered the
     * required materials.
     *
     * @param state the current game state
     */
    private void launchExpeditions(GameState state) {
        Set<Integer> shipyardOwners = new HashSet<>();
        Set<Integer> sailingPlayers = new HashSet<>();
        for (Building b : state.getBuildings()) {
            if (b instanceof ShipyardBuilding && !b.isDestroyed()) {
                shipyardOwners.add(b.getPlayerId());
            }
        }
        for (Ship ship : state.getShips()) {
            sailingPlayers.add(ship.getPlayerId());
        }

        for (Building b : state.getBuildings()) {
            if (!(b instanceof StorageBuilding harbor) || b.isDestroyed()
                    || harbor.getName() != BuildingName.HARBOR) {
                continue;
            }
            int playerId = harbor.getPlayerId();
            if (!shipyardOwners.contains(playerId) || sailingPlayers.contains(playerId)) {
                continue;
            }
            if (harbor.getStoredResources().getOrDefault(ResourceType.PLANK, 0) < GameConfig.EXPEDITION_PLANKS
                    || harbor.getStoredResources().getOrDefault(ResourceType.STONE, 0) < GameConfig.EXPEDITION_STONES) {
                continue;
            }

            Expedition expedition = planExpedition(state, harbor.getPosition());
            if (expedition == null) {
                continue;
            }

            for (int i = 0; i < GameConfig.EXPEDITION_PLANKS; i++) {
                harbor.retrieveResource(ResourceType.PLANK);
            }
            for (int i = 0; i < GameConfig.EXPEDITION_STONES; i++) {
                harbor.retrieveResource(ResourceType.STONE);
            }
            state.getShips().add(new Ship(playerId, expedition.path.get(0),
                    expedition.path, expedition.landing));
            sailingPlayers.add(playerId);
        }
    }

    /**
     * Plans a sea route from a coastal harbor to the nearest unclaimed shore,
     * by breadth-first search over the water tiles.
     *
     * @param state     the current game state
     * @param harborPos the position of the launching harbor
     * @return the planned expedition, or {@code null} if no reachable shore exists
     */
    private Expedition planExpedition(GameState state, Coordinates harborPos) {
        Queue<Coordinates> queue = new ArrayDeque<>();
        Map<Coordinates, Coordinates> parent = new HashMap<>();
        Map<Coordinates, Integer> distance = new HashMap<>();

        for (Direction dir : Direction.values()) {
            Coordinates water = harborPos.neighbor(dir);
            MapTile tile = state.getTile(water);
            if (tile != null && tile.getType() == TileType.WATER && !distance.containsKey(water)) {
                distance.put(water, 1);
                parent.put(water, null);
                queue.add(water);
            }
        }

        int explored = 0;
        while (!queue.isEmpty() && explored < GameConfig.EXPEDITION_MAX_SEARCH) {
            Coordinates water = queue.poll();
            explored++;
            int dist = distance.get(water);

            if (dist >= GameConfig.EXPEDITION_MIN_DISTANCE) {
                Coordinates landing = adjacentColonizableLand(state, water);
                if (landing != null) {
                    return new Expedition(reconstructPath(parent, water), landing);
                }
            }

            for (Direction dir : Direction.values()) {
                Coordinates next = water.neighbor(dir);
                if (distance.containsKey(next)) {
                    continue;
                }
                MapTile tile = state.getTile(next);
                if (tile != null && tile.getType() == TileType.WATER) {
                    distance.put(next, dist + 1);
                    parent.put(next, water);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /**
     * Returns an unclaimed, buildable land tile adjacent to a water tile, or
     * {@code null} if none qualifies.
     *
     * @param state the current game state
     * @param water a water tile
     * @return a colonizable land tile next to {@code water}, or {@code null}
     */
    private Coordinates adjacentColonizableLand(GameState state, Coordinates water) {
        for (Direction dir : Direction.values()) {
            Coordinates land = water.neighbor(dir);
            if (isColonizable(state, land)) {
                return land;
            }
        }
        return null;
    }

    /**
     * Tells whether a tile can host a newly founded harbor: it must be empty,
     * buildable grass owned by nobody.
     *
     * @param state the current game state
     * @param coord the candidate tile
     * @return {@code true} if a harbor may be founded there
     */
    private boolean isColonizable(GameState state, Coordinates coord) {
        MapTile tile = state.getTile(coord);
        return tile != null
                && tile.isBuildable()
                && state.getTerritoryManager().getOwnerAt(coord) == -1
                && !GameActions.isOccupied(state, coord);
    }

    /**
     * Rebuilds the ordered water path from the BFS parent links.
     *
     * @param parent BFS predecessor map
     * @param end    the last water tile reached
     * @return the path from the launch tile to {@code end}, inclusive
     */
    private List<Coordinates> reconstructPath(Map<Coordinates, Coordinates> parent, Coordinates end) {
        List<Coordinates> path = new ArrayList<>();
        Coordinates current = end;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    /** A planned expedition: the water route and the shore to colonize. */
    private record Expedition(List<Coordinates> path, Coordinates landing) {}
}
