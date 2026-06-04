package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.RoadNetwork;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.WorkerState;

import java.util.List;
import java.util.UUID;

/**
 * System managing physical movement and lifecycle transitions of worker units.
 */
public class WorkerSystem implements ISystem {

    /**
     * Processes physical worker units per game tick, handling movement,
     * arrival, building destruction notifications, and return routing.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        RoadNetwork roadNetwork = gameState.getRoadNetwork();
        List<Building> buildings = gameState.getBuildings();

        for (Worker worker : gameState.getWorkers()) {
            if (worker.getTargetBuildingId() != null) {
                Building b = findBuildingById(buildings, worker.getTargetBuildingId());
                if (b == null || b.isDestroyed()) {
                    worker.setType(null);
                    worker.setTargetBuildingId(null);
                    worker.setState(WorkerState.RETURNING);
                    
                    StorageBuilding nearestWarehouse = findNearestWarehouse(gameState, worker.getPosition());
                    if (nearestWarehouse != null && nearestWarehouse.getAttachedFlag() != null) {
                        Flag sourceFlag = findClosestFlag(roadNetwork, worker.getPosition());
                        Flag destFlag = nearestWarehouse.getAttachedFlag();
                        if (sourceFlag != null && destFlag != null) {
                            worker.setPath(roadNetwork.findPath(sourceFlag, destFlag));
                            worker.setCurrentPathIndex(0);
                        }
                    } else {
                        worker.setState(null);
                    }
                }
            }

            if (worker.getState() == WorkerState.SPAWNING) {
                if (worker.getTargetBuildingId() != null) {
                    Building targetBuilding = findBuildingById(buildings, worker.getTargetBuildingId());
                    if (targetBuilding != null && targetBuilding.getAttachedFlag() != null) {
                        Flag sourceFlag = findClosestFlag(roadNetwork, worker.getPosition());
                        Flag destFlag = targetBuilding.getAttachedFlag();
                        if (sourceFlag != null && destFlag != null) {
                            worker.setPath(roadNetwork.findPath(sourceFlag, destFlag));
                            worker.setCurrentPathIndex(0);
                            worker.setState(WorkerState.WALKING_TO_JOB);
                        }
                    }
                }
            }

            if (worker.getState() == WorkerState.WALKING_TO_JOB || worker.getState() == WorkerState.RETURNING) {
                List<Flag> path = worker.getPath();
                if (path != null && !path.isEmpty() && worker.getCurrentPathIndex() < path.size()) {
                    Flag nextFlag = path.get(worker.getCurrentPathIndex());
                    worker.setPosition(nextFlag.getCoordinates());
                    worker.setCurrentPathIndex(worker.getCurrentPathIndex() + 1);
                } else {
                    if (worker.getState() == WorkerState.WALKING_TO_JOB) {
                        worker.setState(WorkerState.WORKING);
                        if (worker.getTargetBuildingId() != null) {
                            Building target = findBuildingById(buildings, worker.getTargetBuildingId());
                            if (target != null) {
                                worker.setPosition(target.getPosition());
                                if (target instanceof ProductionBuilding pb) {
                                    pb.setProductivity(50);
                                    pb.setWaitingTicks(0);
                                }
                            }
                        }
                    } else if (worker.getState() == WorkerState.RETURNING) {
                        StorageBuilding warehouse = findWarehouseAtPosition(buildings, worker.getPosition());
                        if (warehouse != null) {
                            warehouse.setStoredNeutralSettlers(warehouse.getStoredNeutralSettlers() + 1);
                        }
                        worker.setState(null);
                    }
                }
            }
        }
    }

    /**
     * Finds a building on the map by its unique identifier.
     *
     * @param buildings the list of buildings in the game state
     * @param id        the target UUID
     * @return the matching Building instance, or null if not found
     */
    private Building findBuildingById(List<Building> buildings, UUID id) {
        for (Building b : buildings) {
            if (b.getId().equals(id)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Finds a storage building located at a specific coordinate.
     *
     * @param buildings the list of buildings in the game state
     * @param position  the position coordinates to check
     * @return the StorageBuilding at the coordinates, or null if not found
     */
    private StorageBuilding findWarehouseAtPosition(List<Building> buildings, Coordinates position) {
        for (Building b : buildings) {
            if (b instanceof StorageBuilding sb && b.getPosition().equals(position)) {
                return sb;
            }
        }
        return null;
    }

    /**
     * Finds the nearest active StorageBuilding to the given coordinates.
     *
     * @param state the current game state
     * @param pos   the search source position coordinates
     * @return the nearest active StorageBuilding, or null if none exists
     */
    private StorageBuilding findNearestWarehouse(GameState state, Coordinates pos) {
        StorageBuilding nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Building b : state.getBuildings()) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()) {
                double dist = Math.hypot(sb.getPosition().getX() - pos.getX(), sb.getPosition().getY() - pos.getY());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = sb;
                }
            }
        }
        return nearest;
    }

    /**
     * Resolves the closest flag registered on the road network relative to a coordinate.
     *
     * @param network the active road network graph
     * @param coords  the position coordinate to find the closest flag for
     * @return the closest Flag instance
     */
    private Flag findClosestFlag(RoadNetwork network, Coordinates coords) {
        Flag closest = null;
        double minDist = Double.MAX_VALUE;
        for (Flag flag : network.getAllFlags()) {
            double dist = Math.hypot(flag.getCoordinates().getX() - coords.getX(), flag.getCoordinates().getY() - coords.getY());
            if (dist < minDist) {
                minDist = dist;
                closest = flag;
            }
        }
        return closest;
    }
}
