package fr.opensettlers.systems;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.state.RoadNetwork;
import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MapTile;
import fr.opensettlers.entities.StorageBuilding;
import fr.opensettlers.entities.Worker;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.TileType;
import fr.opensettlers.utils.WorkerState;
import fr.opensettlers.utils.WorkerType;

import java.util.List;

/**
 * System driving geologists. A geologist is sent by the player to a flag near
 * the mountains; he walks there along the road network, then surveys up to
 * {@link GameConfig#GEOLOGIST_SURVEYS} mountain tiles within
 * {@link GameConfig#GEOLOGIST_RANGE}, planting a sign on each one (the ore
 * found, or an empty sign). Once done he walks back to the nearest warehouse.
 */
public class GeologistSystem implements ISystem {

    /**
     * Processes all geologists for the current tick: pathing for the freshly
     * spawned ones, surveying for those on site (movement itself is handled
     * by the generic WorkerSystem).
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        for (Worker worker : gameState.getWorkers()) {
            if (worker.getType() != WorkerType.GEOLOGIST) {
                continue;
            }
            if (worker.getState() == WorkerState.SPAWNING) {
                routeToTargetFlag(gameState, worker);
            } else if (worker.getState() == WorkerState.WORKING
                    || worker.getState() == WorkerState.WAITING) {
                survey(gameState, worker);
            }
        }
    }

    /**
     * Computes the walking path from the geologist's position to his target
     * flag. The WorkerSystem then moves him along it like any other worker.
     *
     * @param state  the current game state
     * @param worker the freshly spawned geologist
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
     * Surveys one mountain tile every {@link GameConfig#GEOLOGIST_SURVEY_TICKS}
     * ticks, marking it with a sign. When the quota is reached or no tile is
     * left, the geologist heads home.
     *
     * @param state  the current game state
     * @param worker the geologist on site
     */
    private void survey(GameState state, Worker worker) {
        if (worker.getSurveyCooldown() > 0) {
            worker.setSurveyCooldown(worker.getSurveyCooldown() - 1);
            return;
        }
        if (worker.getSurveysLeft() <= 0) {
            dismiss(state, worker);
            return;
        }

        MapTile tile = findUnsurveyedMountain(state, worker.getPosition());
        if (tile == null) {
            dismiss(state, worker);
            return;
        }

        tile.setSurveyed(true);
        if (tile.getNaturalResource() != null
                && tile.getNaturalResource().getType().isOre()
                && !tile.getNaturalResource().isDepleted()) {
            tile.setGeologistSign(tile.getNaturalResource().getType());
        } else {
            tile.setGeologistSign(null); // Empty sign: nothing buried here
        }
        worker.setSurveysLeft(worker.getSurveysLeft() - 1);
        worker.setSurveyCooldown(GameConfig.GEOLOGIST_SURVEY_TICKS);
        worker.setState(WorkerState.WORKING);
    }

    /**
     * Finds the closest mountain tile around the geologist that was not
     * surveyed yet.
     *
     * @param state  the current game state
     * @param center the geologist's position
     * @return the tile to survey, or {@code null} if none remains in range
     */
    private MapTile findUnsurveyedMountain(GameState state, Coordinates center) {
        for (MapTile tile : state.findTilesInRange(center, GameConfig.GEOLOGIST_RANGE, TileType.MOUNTAIN)) {
            if (!tile.isSurveyed()) {
                return tile;
            }
        }
        return null;
    }

    /**
     * Sends the geologist back to the nearest warehouse (the tool he carries
     * is returned to stock on arrival by the WorkerSystem).
     *
     * @param state  the current game state
     * @param worker the geologist to dismiss
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
