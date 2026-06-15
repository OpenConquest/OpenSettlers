package fr.opensettlers.service;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.ConstructionSite;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.entities.Soldier;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.state.GameState;
import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.SoldierState;
import fr.opensettlers.utils.TileType;
import fr.opensettlers.utils.WorkerType;

import java.util.List;
import java.util.UUID;

/**
 * Validated, side-effecting game operations shared by every actor able to play:
 * the human players (through {@link GameEngineService} and the WebSocket
 * protocol) and the built-in computer opponents (through
 * {@link fr.opensettlers.systems.AiSystem}).
 *
 * <p>All methods must be called on the game-loop thread, which guarantees
 * single-threaded access to the {@link GameState}. They validate terrain,
 * ownership and territory rules and return whether the action was carried out,
 * so callers can decide how to report success or failure.</p>
 */
public final class GameActions {

    private GameActions() {}

    /**
     * Places a construction site for a new building, after validating the
     * placement.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the construction
     * @param name     the building type to construct
     * @param position the desired position
     * @return {@code true} if a construction site was created
     */
    public static boolean placeBuilding(GameState state, int playerId, BuildingName name, Coordinates position) {
        if (!isPlacementValid(state, name, position, playerId)) {
            return false;
        }
        ConstructionSite site = new ConstructionSite(playerId, position, name);
        state.getBuildings().add(site);
        state.getRoadNetwork().addFlag(site.getAttachedFlag());
        return true;
    }

    /**
     * Checks terrain and ownership rules for a building placement.
     * <ul>
     *   <li>Mines must stand on owned mountain tiles.</li>
     *   <li>Harbors and shipyards must stand on owned coastal grass (adjacent to water).</li>
     *   <li>Every other building needs owned, empty grass.</li>
     * </ul>
     *
     * @param state    the game state
     * @param name     the building type
     * @param position the desired position
     * @param playerId the building owner
     * @return {@code true} if the placement is allowed
     */
    public static boolean isPlacementValid(GameState state, BuildingName name, Coordinates position, int playerId) {
        MapTile tile = state.getTile(position);
        if (tile == null || !state.getTerritoryManager().canBuild(playerId, position)) {
            return false;
        }
        if (isOccupied(state, position)) {
            return false;
        }
        if (name == BuildingName.MINE) {
            return tile.getType() == TileType.MOUNTAIN;
        }
        if (name == BuildingName.HARBOR || name == BuildingName.SHIPYARD) {
            return tile.isBuildable() && state.hasWaterInRange(position, 1);
        }
        return tile.isBuildable();
    }

    /**
     * Indicates whether a non-destroyed building already stands on a tile.
     *
     * @param state    the game state
     * @param position the tile to test
     * @return {@code true} if the tile is already built on
     */
    public static boolean isOccupied(GameState state, Coordinates position) {
        for (Building b : state.getBuildings()) {
            if (!b.isDestroyed() && b.getPosition().equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places a standalone flag inside the player's territory.
     *
     * @param state    the game state to mutate
     * @param playerId the player placing the flag
     * @param position the desired position
     * @return the new flag, or {@code null} if the placement was rejected
     */
    public static Flag placeFlag(GameState state, int playerId, Coordinates position) {
        if (!state.getTerritoryManager().canBuild(playerId, position)) {
            return null;
        }
        Flag flag = new Flag(UUID.randomUUID(), playerId, position);
        state.getFlags().add(flag);
        state.getRoadNetwork().addFlag(flag);
        return flag;
    }

    /**
     * Links two existing flags with a road and its carrier.
     *
     * @param state    the game state to mutate
     * @param flagIdA  one endpoint flag ID
     * @param flagIdB  the other endpoint flag ID
     * @param path     intermediate path coordinates (excluding the endpoints)
     * @return {@code true} if a road was created
     */
    public static boolean linkFlags(GameState state, UUID flagIdA, UUID flagIdB, List<Coordinates> path) {
        Flag flagA = state.getRoadNetwork().getFlagById(flagIdA);
        Flag flagB = state.getRoadNetwork().getFlagById(flagIdB);
        if (flagA == null || flagB == null) {
            return false;
        }
        state.getRoadNetwork().addRoad(flagA, flagB, path);
        return true;
    }

    /**
     * Destroys one of the player's own buildings, recalculating territory when
     * a military building or warehouse is razed.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the demolition
     * @param targetId the building to destroy
     * @return {@code true} if a building was destroyed
     */
    public static boolean destroyBuilding(GameState state, int playerId, UUID targetId) {
        Building b = findBuilding(state, targetId);
        if (b == null || b.getPlayerId() != playerId || b.isDestroyed()) {
            return false;
        }
        b.destroy();
        if (b instanceof MilitaryBuilding || b instanceof StorageBuilding) {
            state.getTerritoryManager().recalculate(state);
        }
        return true;
    }

    /**
     * Sends soldiers from the player's nearby military buildings toward an enemy
     * military building or warehouse. Each garrison keeps one defender behind.
     *
     * @param state    the game state to mutate
     * @param playerId the attacking player
     * @param targetId the target building
     * @return the number of soldiers dispatched, or {@code -1} for an invalid order
     */
    public static int attack(GameState state, int playerId, UUID targetId) {
        Building target = findBuilding(state, targetId);
        if (target == null || target.isDestroyed() || target.getPlayerId() == playerId) {
            return -1;
        }
        if (!(target instanceof MilitaryBuilding) && !(target instanceof StorageBuilding)) {
            return -1;
        }

        int dispatched = 0;
        for (Building b : state.getBuildings()) {
            if (!(b instanceof MilitaryBuilding mb) || mb.isDestroyed() || mb.getPlayerId() != playerId) {
                continue;
            }
            if (mb.getPosition().distanceTo(target.getPosition()) > GameConfig.ATTACK_RADIUS) {
                continue;
            }
            while (mb.getSoldiers().size() > 1) {
                Soldier soldier = mb.removeFirstSoldier();
                soldier.setPosition(new Coordinates(mb.getPosition().getX(), mb.getPosition().getY()));
                soldier.setState(SoldierState.MARCHING_TO_ATTACK);
                soldier.setTargetBuilding(target);
                soldier.setGarrison(null);
                state.getSoldiers().add(soldier);
                dispatched++;
            }
        }
        return dispatched;
    }

    /**
     * Trains a geologist at the nearest able warehouse and sends them to survey
     * the mountains around one of the player's flags.
     *
     * @param state    the game state to mutate
     * @param playerId the player ordering the survey
     * @param flagId   the destination flag to survey around
     * @return {@code true} if a geologist was dispatched
     */
    public static boolean sendGeologist(GameState state, int playerId, UUID flagId) {
        Flag target = state.getRoadNetwork().getFlagById(flagId);
        if (target == null || target.isDestroyed() || target.getPlayerId() != playerId) {
            return false;
        }

        StorageBuilding source = null;
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()
                    && sb.getPlayerId() == playerId && sb.canSpawnWorker()) {
                double dist = sb.getPosition().distanceTo(target.getCoordinates());
                if (dist < minDist) {
                    minDist = dist;
                    source = sb;
                }
            }
        }
        if (source == null) {
            return false;
        }

        Worker geologist = source.spawnWorker(WorkerType.GEOLOGIST, source.getPosition());
        geologist.setTargetFlagId(target.getId());
        geologist.setSurveysLeft(GameConfig.GEOLOGIST_SURVEYS);
        state.getWorkers().add(geologist);
        return true;
    }

    /**
     * Finds a building by its ID.
     *
     * @param state    the game state
     * @param targetId the building ID
     * @return the building, or {@code null} if not found
     */
    private static Building findBuilding(GameState state, UUID targetId) {
        for (Building b : state.getBuildings()) {
            if (b.getId().equals(targetId)) {
                return b;
            }
        }
        return null;
    }
}
