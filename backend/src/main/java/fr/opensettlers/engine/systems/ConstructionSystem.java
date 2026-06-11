package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.BuildingFactory;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.RoadNetwork;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.*;

import java.util.List;

/**
 * System coordinating construction sites, from delivery ingestion, groundwork leveling,
 * masonry construction progress, to final commissioning and specialist occupation.
 */
public class ConstructionSystem implements ISystem {

    /**
     * Iterates and updates all active construction sites and regenerates settlers at warehouses.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        // Copy: completeConstruction swaps entries in the live building list
        List<Building> buildings = new java.util.ArrayList<>(gameState.getBuildings());
        for (Building b : buildings) {
            if (b instanceof ConstructionSite site) {
                processSite(gameState, site);
            }
        }

        for (Building b : buildings) {
            if (b instanceof StorageBuilding sb) {
                sb.regenerateNeutralSettlers();
            }
        }
    }

    /**
     * Drives the step-by-step construction phases for an individual site.
     *
     * @param state the current game state
     * @param site  the construction site to process
     */
    private void processSite(GameState state, ConstructionSite site) {
        Flag flag = site.getAttachedFlag();
        if (flag != null) {
            for (ResourceType type : site.getRequiredMaterials().keySet()) {
                int needed = site.getRequiredMaterials().get(type);
                int current = site.getDeliveredMaterials().getOrDefault(type, 0);

                while (current < needed && flag.getResourceCount(type) > 0) {
                    flag.popResource(type);
                    current++;
                    site.getDeliveredMaterials().put(type, current);
                }
            }
        }

        if (!allMaterialsDelivered(site)) {
            return;
        }

        if (site.getGroundworkProgress() < 100) {
            if (site.getAssignedTerrassier() == null) {
                Worker terrassier = spawnWorkerFromWarehouse(state, site, WorkerType.TERRASSIER);
                if (terrassier != null) {
                    site.setAssignedTerrassier(terrassier);
                }
            } else {
                Worker terrassier = site.getAssignedTerrassier();
                if (terrassier.getState() == WorkerState.WORKING) {
                    site.setGroundworkProgress(site.getGroundworkProgress() + 5);
                }
            }
            return;
        }

        if (site.getBuildingProgress() < 100) {
            if (site.getAssignedTerrassier() != null) {
                dismissWorker(state, site.getAssignedTerrassier());
                site.setAssignedTerrassier(null);
            }

            if (site.getAssignedBuilder() == null) {
                Worker builder = spawnWorkerFromWarehouse(state, site, WorkerType.BUILDER);
                if (builder != null) {
                    site.setAssignedBuilder(builder);
                }
            } else {
                Worker builder = site.getAssignedBuilder();
                if (builder.getState() == WorkerState.WORKING) {
                    site.setBuildingProgress(site.getBuildingProgress() + 5);
                }
            }
            return;
        }

        if (site.getAssignedBuilder() != null) {
            dismissWorker(state, site.getAssignedBuilder());
            site.setAssignedBuilder(null);
        }
        completeConstruction(state, site);
    }

    /**
     * Checks if all required materials for construction have been delivered.
     *
     * @param site the construction site to check
     * @return true if all material quantities are satisfied, false otherwise
     */
    private boolean allMaterialsDelivered(ConstructionSite site) {
        for (ResourceType type : site.getRequiredMaterials().keySet()) {
            int needed = site.getRequiredMaterials().get(type);
            int current = site.getDeliveredMaterials().getOrDefault(type, 0);
            if (current < needed) {
                return false;
            }
        }
        return true;
    }

    /**
     * Spawns a worker of the target role from the nearest warehouse.
     *
     * @param state          the current game state
     * @param targetBuilding the building they are assigned to
     * @param type           the specialized role to spawn them with
     * @return the spawned Worker instance, or null if none available
     */
    private Worker spawnWorkerFromWarehouse(GameState state, Building targetBuilding, WorkerType type) {
        StorageBuilding warehouse = findNearestWarehouse(state, targetBuilding.getPosition());
        if (warehouse != null) {
            Worker worker = warehouse.spawnWorker(type, warehouse.getPosition());
            if (worker != null) {
                worker.setTargetBuildingId(targetBuilding.getId());
                state.getWorkers().add(worker);
                return worker;
            }
        }
        return null;
    }

    /**
     * Dismisses a worker, removing their specialized role and routing them back to the warehouse.
     *
     * @param state  the current game state
     * @param worker the worker unit to dismiss
     */
    private void dismissWorker(GameState state, Worker worker) {
        if (worker == null) return;
        worker.setType(null);
        worker.setTargetBuildingId(null);
        worker.setState(WorkerState.RETURNING);

        StorageBuilding warehouse = findNearestWarehouse(state, worker.getPosition());
        if (warehouse != null && warehouse.getAttachedFlag() != null) {
            Flag sourceFlag = findClosestFlag(state.getRoadNetwork(), worker.getPosition());
            Flag destFlag = warehouse.getAttachedFlag();
            if (sourceFlag != null && destFlag != null) {
                worker.setPath(state.getRoadNetwork().findPath(sourceFlag, destFlag));
                worker.setCurrentPathIndex(0);
            }
        } else {
            worker.setState(null);
        }
    }

    /**
     * Converts a construction site into its completed building representation and spawns a specialist.
     *
     * @param state the current game state
     * @param site  the completed site to swap out
     */
    private void completeConstruction(GameState state, ConstructionSite site) {
        Building newBuilding = BuildingFactory.createBuilding(
                site.getTargetBuildingType(),
                site.getPlayerId(),
                site.getPosition(),
                state.getMap()
        );

        newBuilding.setAttachedFlag(site.getAttachedFlag());
        site.getAttachedFlag().setBuilding(newBuilding);

        state.getBuildings().remove(site);
        state.getBuildings().add(newBuilding);

        WorkerType occupantRole = getWorkerTypeForBuilding(site.getTargetBuildingType());
        if (occupantRole != null && newBuilding instanceof ProductionBuilding pb) {
            Worker specialist = spawnWorkerFromWarehouse(state, pb, occupantRole);
            if (specialist != null) {
                pb.setOccupant(specialist);
            }
        }
    }

    /**
     * Resolves the nearest warehouse building relative to a map position.
     *
     * @param state the current game state
     * @param pos   the coordinates from which to measure distance
     * @return the nearest active StorageBuilding, or null if none
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
     * Finds the closest registered flag in the network relative to coordinates.
     *
     * @param network the active road network
     * @param coords  the coordinates to check
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

    /**
     * Maps a building name to its corresponding specialist worker type.
     *
     * @param name the building name
     * @return the corresponding WorkerType, or null if none
     */
    private WorkerType getWorkerTypeForBuilding(BuildingName name) {
        return switch (name) {
            case WOODCUTTER -> WorkerType.WOODCUTTER;
            case FORESTER -> WorkerType.FORESTER;
            case QUARRY -> WorkerType.QUARRYMAN;
            case MINE -> WorkerType.MINER;
            case FISHING_HUT -> WorkerType.FISHERMAN;
            case FARM -> WorkerType.FARMER;
            case SAWMILL -> WorkerType.CARPENTER;
            case MILL -> WorkerType.MILLER;
            case BAKERY -> WorkerType.BAKER;
            case BREWERY -> WorkerType.BREWER;
            case FOUNDRY -> WorkerType.SMELTER;
            case ARMORY -> WorkerType.SMITH;
            default -> null;
        };
    }
}
