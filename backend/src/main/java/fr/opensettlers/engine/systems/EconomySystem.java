package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.RoadNetwork;
import fr.opensettlers.engine.state.*;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import fr.opensettlers.engine.state.utils.ResourceType;

import java.util.*;

public class EconomySystem implements ISystem {

    @Override
    public void process(GameState gameState) {
        RoadNetwork roadNetwork = gameState.getRoadNetwork();
        List<Building> buildings = gameState.getBuildings();

        // Ingest resources that arrived at StorageBuildings (Warehouses)
        for (Building b : buildings) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()) {
                Flag flag = sb.getAttachedFlag();
                if (flag != null) {
                    for (int i = 0; i < flag.getResourceSlots().size(); i++) {
                        ResourceStack rs = flag.getResourceSlots().get(i);
                        if (rs.getTargetFlagId() != null && rs.getTargetFlagId().equals(flag.getId())) {
                            flag.getResourceSlots().remove(i);
                            sb.storeResource(rs.getType());
                            i--; // Adjust index since we removed an item
                        }
                    }
                }
            }
        }

        // Process matching for each ResourceType
        for (ResourceType type : ResourceType.values()) {
            List<Demand> demands = gatherDemands(gameState, type);
            List<Supply> supplies = gatherSupplies(gameState, type);

            // Sort demands by priority: Military Construction > Regular Construction > Production
            demands.sort((d1, d2) -> {
                int p1 = getDemandPriority(d1, gameState);
                int p2 = getDemandPriority(d2, gameState);
                if (p1 != p2) {
                    return Integer.compare(p1, p2);
                }
                // Tie breaker: use global resource distribution preferences if applicable
                return Integer.compare(getPreferenceIndex(d1, gameState), getPreferenceIndex(d2, gameState));
            });

            // Match demands with closest supplies
            for (Demand demand : demands) {
                while (demand.quantity > 0) {
                    Supply bestSupply = findClosestSupply(roadNetwork, demand.flag, supplies);
                    if (bestSupply == null) {
                        break; // No more supply for this resource type
                    }

                    // Route 1 unit
                    routeResource(gameState, bestSupply, demand.flag, type);

                    // Update counts
                    demand.quantity--;
                    bestSupply.quantity--;
                    if (bestSupply.quantity <= 0) {
                        supplies.remove(bestSupply);
                    }
                }
            }

            // Route any remaining unrouted production outputs or flag resources of this type to the nearest Warehouse
            routeSurplusToWarehouses(gameState, supplies, type);
        }
    }

    private int getDemandPriority(Demand demand, GameState state) {
        if (demand.building instanceof ConstructionSite site) {
            BuildingName target = site.getTargetBuildingType();
            if (target == BuildingName.GUARD_HOUSE || target == BuildingName.WATCH_TOWER || target == BuildingName.CASTLE) {
                return 1; // High priority: Military construction
            }
            return 2; // Medium-high priority: Regular construction
        }
        return 3; // Medium priority: Active production
    }

    private int getPreferenceIndex(Demand demand, GameState state) {
        if (demand.building != null) {
            BuildingName name = null;
            if (demand.building instanceof ConstructionSite site) {
                name = site.getTargetBuildingType();
            } else if (demand.building instanceof ProductionBuilding pb) {
                name = getBuildingNameForClass(pb);
            }

            if (name != null) {
                List<BuildingName> pref = state.getResourceDistributionPriorities().get(demand.type);
                if (pref != null) {
                    int idx = pref.indexOf(name);
                    if (idx != -1) {
                        return idx;
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private BuildingName getBuildingNameForClass(ProductionBuilding pb) {
        if (pb instanceof RawExtractor re) {
            if (re.getExtractedResource() == ResourceType.LOG) return BuildingName.WOODCUTTER;
            if (re.getExtractedResource() == ResourceType.STONE) return BuildingName.QUARRY;
            if (re.getExtractedResource() == ResourceType.IRON) return BuildingName.MINE;
            if (re.getExtractedResource() == ResourceType.FISH) return BuildingName.FISHING_HUT;
            if (re.getExtractedResource() == ResourceType.WHEAT) return BuildingName.FARM;
            if (re.getExtractedResource() == ResourceType.WATER) return BuildingName.WATER_WELL;
            if (re.getExtractedResource() == null) return BuildingName.FORESTER;
        } else if (pb instanceof ProcessingBuilding pcb) {
            ResourceType out = pcb.getRecipe().getOutput();
            if (out == ResourceType.PLANK) return BuildingName.SAWMILL;
            if (out == ResourceType.FLOUR) return BuildingName.MILL;
            if (out == ResourceType.BREAD) return BuildingName.BAKERY;
            if (out == ResourceType.BEER) return BuildingName.BREWERY;
            if (out == ResourceType.STEEL) return BuildingName.FOUNDRY;
            if (out == ResourceType.SWORD) return BuildingName.ARMORY;
        }
        return null;
    }

    private List<Demand> gatherDemands(GameState state, ResourceType type) {
        List<Demand> demands = new ArrayList<>();
        for (Building b : state.getBuildings()) {
            if (b.isDestroyed()) continue;

            Flag flag = b.getAttachedFlag();
            if (flag == null) continue;

            if (b instanceof ConstructionSite site) {
                int req = site.getRequiredMaterials().getOrDefault(type, 0);
                int del = site.getDeliveredMaterials().getOrDefault(type, 0);
                int inTransit = countInTransit(state, flag.getId(), type);
                int netDemand = req - del - inTransit;
                if (netDemand > 0) {
                    demands.add(new Demand(site, flag, type, netDemand));
                }
            } else if (b instanceof ProductionBuilding pb) {
                if (pb.getInputSlots() != null) {
                    for (ResourceSlot slot : pb.getInputSlots()) {
                        if (slot.getType() == type) {
                            int current = slot.getQuantity();
                            int inTransit = countInTransit(state, flag.getId(), type);
                            int netDemand = slot.getMAX_PER_SLOT() - current - inTransit;
                            if (netDemand > 0) {
                                demands.add(new Demand(pb, flag, type, netDemand));
                            }
                        }
                    }
                }
            }
        }
        return demands;
    }

    private List<Supply> gatherSupplies(GameState state, ResourceType type) {
        List<Supply> supplies = new ArrayList<>();

        // 1. From production outputs
        for (Building b : state.getBuildings()) {
            if (b.isDestroyed()) continue;
            Flag flag = b.getAttachedFlag();
            if (flag == null) continue;

            if (b instanceof ProductionBuilding pb) {
                ResourceSlot outSlot = pb.getOutputSlot();
                if (outSlot != null && outSlot.getType() == type) {
                    int available = outSlot.getQuantity() - pb.getOutputDestinations().size();
                    if (available > 0) {
                        supplies.add(new Supply(pb, flag, available, SupplySource.PRODUCTION));
                    }
                }
            }
        }

        // 2. From unrouted resources on flags
        for (Flag flag : state.getRoadNetwork().getAllFlags()) {
            if (flag.isDestroyed()) continue;
            int unroutedCount = 0;
            for (ResourceStack rs : flag.getResourceSlots()) {
                if (rs.getType() == type && rs.getTargetFlagId() == null) {
                    unroutedCount++;
                }
            }
            if (unroutedCount > 0) {
                supplies.add(new Supply(null, flag, unroutedCount, SupplySource.FLAG));
            }
        }

        // 3. From storage buildings (Warehouse)
        for (Building b : state.getBuildings()) {
            if (b.isDestroyed()) continue;
            Flag flag = b.getAttachedFlag();
            if (flag == null) continue;

            if (b instanceof StorageBuilding sb) {
                int stored = sb.getStoredResources().getOrDefault(type, 0);
                if (stored > 0) {
                    supplies.add(new Supply(sb, flag, stored, SupplySource.WAREHOUSE));
                }
            }
        }

        return supplies;
    }

    private Supply findClosestSupply(RoadNetwork network, Flag demandFlag, List<Supply> supplies) {
        Supply bestSupply = null;
        int minDistance = Integer.MAX_VALUE;

        // Prioritize non-warehouse supplies first
        for (Supply s : supplies) {
            if (s.source == SupplySource.WAREHOUSE) continue;
            List<Flag> path = network.findPath(s.flag, demandFlag);
            if (path != null) {
                int dist = path.size();
                if (dist < minDistance) {
                    minDistance = dist;
                    bestSupply = s;
                }
            }
        }

        // Fallback to warehouse supplies if none found
        if (bestSupply == null) {
            for (Supply s : supplies) {
                if (s.source != SupplySource.WAREHOUSE) continue;
                List<Flag> path = network.findPath(s.flag, demandFlag);
                if (path != null) {
                    int dist = path.size();
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestSupply = s;
                    }
                }
            }
        }

        return bestSupply;
    }

    private void routeResource(GameState state, Supply supply, Flag demandFlag, ResourceType type) {
        if (supply.source == SupplySource.PRODUCTION) {
            state.getTransportManager().requestTransportFromBuilding((ProductionBuilding) supply.provider, demandFlag);
        } else if (supply.source == SupplySource.FLAG) {
            state.getTransportManager().requestTransport(supply.flag, type, demandFlag);
        } else if (supply.source == SupplySource.WAREHOUSE) {
            StorageBuilding sb = (StorageBuilding) supply.provider;
            sb.retrieveResource(type);
            sb.getAttachedFlag().addResource(type, demandFlag.getId());
        }
    }

    private void routeSurplusToWarehouses(GameState state, List<Supply> supplies, ResourceType type) {
        for (Supply supply : supplies) {
            if (supply.source == SupplySource.WAREHOUSE) continue; // Already in storage

            StorageBuilding nearestWarehouse = findNearestWarehouse(state, supply.flag.getCoordinates());
            if (nearestWarehouse != null && nearestWarehouse.getAttachedFlag() != null) {
                Flag whFlag = nearestWarehouse.getAttachedFlag();
                if (!whFlag.getId().equals(supply.flag.getId())) {
                    if (supply.source == SupplySource.PRODUCTION) {
                        state.getTransportManager().requestTransportFromBuilding((ProductionBuilding) supply.provider, whFlag);
                    } else if (supply.source == SupplySource.FLAG) {
                        state.getTransportManager().requestTransport(supply.flag, type, whFlag);
                    }
                }
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

    private int countInTransit(GameState state, UUID targetFlagId, ResourceType type) {
        int count = 0;
        for (Road road : state.getRoadNetwork().getAllRoads()) {
            Carrier carrier = road.getCarrier();
            if (carrier != null && carrier.getCarriedResource() != null) {
                ResourceStack rs = carrier.getCarriedResource();
                if (rs.getType() == type && targetFlagId.equals(rs.getTargetFlagId())) {
                    count++;
                }
            }
        }
        for (Flag flag : state.getRoadNetwork().getAllFlags()) {
            for (ResourceStack rs : flag.getResourceSlots()) {
                if (rs.getType() == type && targetFlagId.equals(rs.getTargetFlagId())) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- Helper Classes ---

    private static class Demand {
        Building building;
        Flag flag;
        ResourceType type;
        int quantity;

        Demand(Building building, Flag flag, ResourceType type, int quantity) {
            this.building = building;
            this.flag = flag;
            this.type = type;
            this.quantity = quantity;
        }
    }

    private enum SupplySource { PRODUCTION, FLAG, WAREHOUSE }

    private static class Supply {
        Building provider; // ProductionBuilding or StorageBuilding (Warehouse), null if Flag
        Flag flag;
        int quantity;
        SupplySource source;

        Supply(Building provider, Flag flag, int quantity, SupplySource source) {
            this.provider = provider;
            this.flag = flag;
            this.quantity = quantity;
            this.source = source;
        }
    }
}
