package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.RoadNetwork;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.WorkerState;

import java.util.List;
import java.util.UUID;

public class WorkerSystem implements ISystem {

    @Override
    public void process(GameState gameState) {
        RoadNetwork roadNetwork = gameState.getRoadNetwork();
        List<Building> buildings = gameState.getBuildings();

        for (Worker worker : gameState.getWorkers()) {
            // Check if building is destroyed/missing for walking or working workers
            if (worker.getTargetBuildingId() != null) {
                Building b = findBuildingById(buildings, worker.getTargetBuildingId());
                if (b == null || b.isDestroyed()) {
                    // Specialist role is lost, worker becomes neutral and returns
                    worker.setType(null);
                    worker.setTargetBuildingId(null);
                    worker.setState(WorkerState.RETURNING);
                    
                    // Recalculate path to nearest warehouse
                    StorageBuilding nearestWarehouse = findNearestWarehouse(gameState, worker.getPosition());
                    if (nearestWarehouse != null && nearestWarehouse.getAttachedFlag() != null) {
                        Flag sourceFlag = findClosestFlag(roadNetwork, worker.getPosition());
                        Flag destFlag = nearestWarehouse.getAttachedFlag();
                        if (sourceFlag != null && destFlag != null) {
                            worker.setPath(roadNetwork.findPath(sourceFlag, destFlag));
                            worker.setCurrentPathIndex(0);
                        }
                    } else {
                        // No warehouse found, immediately clean up the worker
                        worker.setState(null);
                    }
                }
            }

            // Move the worker if they are on a path
            if (worker.getState() == WorkerState.SPAWNING) {
                // Initialize path toward the target building
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
                    // Move to the next flag in the path
                    Flag nextFlag = path.get(worker.getCurrentPathIndex());
                    worker.setPosition(nextFlag.getCoordinates());
                    worker.setCurrentPathIndex(worker.getCurrentPathIndex() + 1);
                } else {
                    // Arrived at the end of the path
                    if (worker.getState() == WorkerState.WALKING_TO_JOB) {
                        // Arrived at the building site
                        worker.setState(WorkerState.WORKING);
                        if (worker.getTargetBuildingId() != null) {
                            Building target = findBuildingById(buildings, worker.getTargetBuildingId());
                            if (target != null) {
                                worker.setPosition(target.getPosition());
                            }
                        }
                    } else if (worker.getState() == WorkerState.RETURNING) {
                        // Arrived at the warehouse
                        StorageBuilding warehouse = findWarehouseAtPosition(buildings, worker.getPosition());
                        if (warehouse != null) {
                            warehouse.setStoredNeutralSettlers(warehouse.getStoredNeutralSettlers() + 1);
                        }
                        // Mark worker as inactive/ready to be cleaned up
                        worker.setState(null);
                    }
                }
            }
        }
    }

    private Building findBuildingById(List<Building> buildings, UUID id) {
        for (Building b : buildings) {
            if (b.getId().equals(id)) {
                return b;
            }
        }
        return null;
    }

    private StorageBuilding findWarehouseAtPosition(List<Building> buildings, Coordinates position) {
        for (Building b : buildings) {
            if (b instanceof StorageBuilding sb && b.getPosition().equals(position)) {
                return sb;
            }
        }
        return null;
    }

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
