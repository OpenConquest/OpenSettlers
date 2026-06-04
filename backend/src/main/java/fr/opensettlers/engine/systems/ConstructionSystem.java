package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.BuildingFactory;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.RoadNetwork;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.*;

import java.util.List;

public class ConstructionSystem implements ISystem {

    @Override
    public void process(GameState gameState) {
        // Collect construction sites to process
        List<Building> buildings = gameState.getBuildings();
        for (int i = 0; i < buildings.size(); i++) {
            Building b = buildings.get(i);
            if (b instanceof ConstructionSite site) {
                processSite(gameState, site);
            }
        }

        // Regenerate storage buildings settlers
        for (Building b : buildings) {
            if (b instanceof StorageBuilding sb) {
                sb.regenerateNeutralSettlers();
            }
        }
    }

    private void processSite(GameState state, ConstructionSite site) {
        // 1. Ingest materials that arrived at the site's attached flag
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

        // 2. Lock phase until all materials are delivered
        if (!allMaterialsDelivered(site)) {
            return;
        }

        // 3. Groundwork Phase
        if (site.getGroundworkProgress() < 100) {
            if (site.getAssignedTerrassier() == null) {
                // Request/spawn a TERRASSIER
                Worker terrassier = spawnWorkerFromWarehouse(state, site, WorkerType.TERRASSIER);
                if (terrassier != null) {
                    site.setAssignedTerrassier(terrassier);
                }
            } else {
                Worker terrassier = site.getAssignedTerrassier();
                if (terrassier.getState() == WorkerState.WORKING) {
                    site.setGroundworkProgress(site.getGroundworkProgress() + 5); // 5% progress per tick
                }
            }
            return;
        }

        // 4. Building Phase
        if (site.getBuildingProgress() < 100) {
            // Dismiss terrassier when groundwork is completed
            if (site.getAssignedTerrassier() != null) {
                dismissWorker(state, site.getAssignedTerrassier());
                site.setAssignedTerrassier(null);
            }

            if (site.getAssignedBuilder() == null) {
                // Request/spawn a BUILDER
                Worker builder = spawnWorkerFromWarehouse(state, site, WorkerType.BUILDER);
                if (builder != null) {
                    site.setAssignedBuilder(builder);
                }
            } else {
                Worker builder = site.getAssignedBuilder();
                if (builder.getState() == WorkerState.WORKING) {
                    site.setBuildingProgress(site.getBuildingProgress() + 5); // 5% progress per tick
                }
            }
            return;
        }

        // 5. Completion Phase
        if (site.getAssignedBuilder() != null) {
            dismissWorker(state, site.getAssignedBuilder());
            site.setAssignedBuilder(null);
        }
        completeConstruction(state, site);
    }

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

    private void dismissWorker(GameState state, Worker worker) {
        if (worker == null) return;
        worker.setType(null); // Loses role
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
            worker.setState(null); // Clean up immediately if no warehouse
        }
    }

    private void completeConstruction(GameState state, ConstructionSite site) {
        // Instantiate the final building
        Building newBuilding = BuildingFactory.createBuilding(
                site.getTargetBuildingType(),
                site.getPlayerId(),
                site.getPosition()
        );

        // Reuse the existing flag and connect it to the new building
        newBuilding.setAttachedFlag(site.getAttachedFlag());
        site.getAttachedFlag().setBuilding(newBuilding);

        // Swap the buildings in the game state
        state.getBuildings().remove(site);
        state.getBuildings().add(newBuilding);

        // Spawn occupant worker if this is a production building
        WorkerType occupantRole = getWorkerTypeForBuilding(site.getTargetBuildingType());
        if (occupantRole != null && newBuilding instanceof ProductionBuilding pb) {
            Worker specialist = spawnWorkerFromWarehouse(state, pb, occupantRole);
            if (specialist != null) {
                pb.setOccupant(specialist);
            }
        }
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
