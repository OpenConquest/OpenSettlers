package fr.opensettlers.systems;

import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.state.GameState;
import fr.opensettlers.state.RoadNetwork;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.Direction;
import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.utils.WorkerState;
import fr.opensettlers.utils.WorkerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * System driving scouts, the exploration counterpart of geologists. A scout is
 * sent by the player to a flag at the edge of the known world; he walks there
 * along the road network, then wanders {@link GameConfig#SCOUT_STEPS} random
 * steps within {@link GameConfig#SCOUT_RANGE} of the flag, revealing the fog of
 * war with his wide {@link GameConfig#SCOUT_VISION}. Once done he walks back to
 * the nearest warehouse.
 */
public class ScoutSystem implements ISystem {

    /** Random source used to pick the scout's wandering steps. */
    private final Random random = new Random();

    /**
     * Processes all scouts for the current tick: pathing for the freshly
     * spawned ones, wandering for those on site (walking is handled by the
     * generic WorkerSystem).
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        for (Worker worker : gameState.getWorkers()) {
            if (worker.getType() != WorkerType.SCOUT) {
                continue;
            }
            if (worker.getState() == WorkerState.SPAWNING) {
                routeToTargetFlag(gameState, worker);
            } else if (worker.getState() == WorkerState.WORKING
                    || worker.getState() == WorkerState.WAITING) {
                explore(gameState, worker);
            }
        }
    }

    /**
     * Computes the walking path from the scout's position to his target flag.
     * The WorkerSystem then moves him along it like any other worker.
     *
     * @param state  the current game state
     * @param worker the freshly spawned scout
     */
    private void routeToTargetFlag(GameState state, Worker worker) {
        Flag target = worker.getTargetFlagId() != null
                ? state.getRoadNetwork().getFlagById(worker.getTargetFlagId())
                : null;
        if (target == null || target.isDestroyed()) {
            dismiss(state, worker);
            return;
        }

        Flag source = findClosestFlag(state.getRoadNetwork(), worker.getPosition());
        List<Flag> path = source != null ? state.getRoadNetwork().findPath(source, target) : null;
        if (path == null) {
            dismiss(state, worker);
            return;
        }
        worker.setPath(path);
        worker.setCurrentPathIndex(0);
        worker.setState(WorkerState.WALKING_TO_JOB);
    }

    /**
     * Takes one exploration step every {@link GameConfig#SCOUT_STEP_TICKS}
     * ticks: the scout walks to a random walkable neighbor tile, staying within
     * {@link GameConfig#SCOUT_RANGE} of his target flag. When his step quota is
     * exhausted, he heads home.
     *
     * @param state  the current game state
     * @param worker the scout on site
     */
    private void explore(GameState state, Worker worker) {
        if (worker.getSurveyCooldown() > 0) {
            worker.setSurveyCooldown(worker.getSurveyCooldown() - 1);
            return;
        }
        if (worker.getSurveysLeft() <= 0) {
            dismiss(state, worker);
            return;
        }

        Flag anchor = worker.getTargetFlagId() != null
                ? state.getRoadNetwork().getFlagById(worker.getTargetFlagId())
                : null;
        Coordinates center = anchor != null ? anchor.getCoordinates() : worker.getPosition();

        List<Coordinates> candidates = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            Coordinates next = worker.getPosition().neighbor(dir);
            MapTile tile = state.getTile(next);
            if (tile != null && tile.isWalkable()
                    && next.distanceTo(center) <= GameConfig.SCOUT_RANGE) {
                candidates.add(next);
            }
        }
        if (!candidates.isEmpty()) {
            worker.setPosition(candidates.get(random.nextInt(candidates.size())));
        }
        worker.setSurveysLeft(worker.getSurveysLeft() - 1);
        worker.setSurveyCooldown(GameConfig.SCOUT_STEP_TICKS);
        worker.setState(WorkerState.WORKING);
    }

    /**
     * Sends the scout back to the nearest warehouse (the tool he carries is
     * returned to stock on arrival by the WorkerSystem).
     *
     * @param state  the current game state
     * @param worker the scout to dismiss
     */
    private void dismiss(GameState state, Worker worker) {
        worker.setType(null);
        worker.setTargetFlagId(null);
        worker.setState(WorkerState.RETURNING);

        StorageBuilding warehouse = findNearestWarehouse(state, worker.getPosition());
        if (warehouse != null && warehouse.getAttachedFlag() != null) {
            Flag sourceFlag = findClosestFlag(state.getRoadNetwork(), worker.getPosition());
            List<Flag> path = sourceFlag != null
                    ? state.getRoadNetwork().findPath(sourceFlag, warehouse.getAttachedFlag())
                    : null;
            if (path != null) {
                worker.setPath(path);
                worker.setCurrentPathIndex(0);
                return;
            }
        }
        worker.setState(null); // No way home: the unit is removed
    }

    /**
     * Resolves the closest flag registered on the road network.
     *
     * @param network the road network
     * @param coords  the position to search from
     * @return the closest flag, or {@code null}
     */
    private Flag findClosestFlag(RoadNetwork network, Coordinates coords) {
        Flag closest = null;
        double minDist = Double.MAX_VALUE;
        for (Flag flag : network.getAllFlags()) {
            double dist = flag.getCoordinates().distanceTo(coords);
            if (dist < minDist) {
                minDist = dist;
                closest = flag;
            }
        }
        return closest;
    }

    /**
     * Locates the nearest active storage building.
     *
     * @param state the current game state
     * @param pos   the position to search from
     * @return the nearest storage, or {@code null}
     */
    private StorageBuilding findNearestWarehouse(GameState state, Coordinates pos) {
        StorageBuilding nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()) {
                double dist = sb.getPosition().distanceTo(pos);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = sb;
                }
            }
        }
        return nearest;
    }
}
