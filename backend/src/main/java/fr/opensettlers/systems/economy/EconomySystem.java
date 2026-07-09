package fr.opensettlers.systems.economy;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.state.RoadNetwork;
import fr.opensettlers.entities.building.*;
import fr.opensettlers.entities.unit.*;
import fr.opensettlers.entities.world.*;
import fr.opensettlers.entities.resource.*;
import fr.opensettlers.utils.enums.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.enums.ResourceType;
import fr.opensettlers.systems.economy.Demand;
import fr.opensettlers.systems.economy.Supply;
import fr.opensettlers.systems.economy.SupplySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import fr.opensettlers.systems.ISystem;

/**
 * System coordinating matching of global supplies and demands.
 * Routes surplus items to Warehouses, and serves demands prioritizing
 * construction materials followed by a FIFO queue for production.
 */
public class EconomySystem implements ISystem {

    /**
     * Executes the matching logic between supplies and demands,
     * routes items to targets, and deposits materials arriving at warehouse flags.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        RoadNetwork roadNetwork = gameState.getRoadNetwork();
        List<Building> buildings = gameState.getBuildings();

        // Ingest resources that arrived at warehouses
        for (Building b : buildings) {
            if (b instanceof StorageBuilding sb && !sb.isDestroyed()) {
                Flag flag = sb.getAttachedFlag();
                if (flag != null) {
                    for (int i = 0; i < flag.getResourceSlots().size(); i++) {
                        ResourceStack rs = flag.getResourceSlots().get(i);
                        if (rs.getTargetFlagId() != null && rs.getTargetFlagId().equals(flag.getId())) {
                            flag.getResourceSlots().remove(i);
                            sb.storeResource(rs.getType());
                            i--;
                        }
                    }
                }
            }
        }

        // Run supply-demand matching for each ResourceType
        for (ResourceType type : ResourceType.values()) {
            // Donkeys are units, not goods: they stay at the breeder until the
            // DonkeySystem sends them walking to a main road.
            if (type == ResourceType.DONKEY) continue;
            List<Demand> demands = gatherDemands(gameState, type);
            List<Supply> supplies = gatherSupplies(gameState, type);

            // Sort: construction demands first, then production demands by the
            // owner's distribution priority for this good (ties keep FIFO order).
            final ResourceType good = type;
            demands.sort((d1, d2) -> {
                boolean isConst1 = d1.building instanceof ConstructionSite;
                boolean isConst2 = d2.building instanceof ConstructionSite;
                if (isConst1 != isConst2) {
                    return isConst1 ? -1 : 1;
                }
                if (isConst1) {
                    return 0; // both construction: stable FIFO placement order
                }
                return Integer.compare(
                        distributionRank(gameState, d1, good),
                        distributionRank(gameState, d2, good));
            });

            // Match demands with closest supplies
            for (Demand demand : demands) {
                while (demand.quantity > 0) {
                    Supply bestSupply = findClosestSupply(roadNetwork, demand.flag, supplies);
                    if (bestSupply == null) {
                        break;
                    }

                    if (!routeResource(gameState, bestSupply, demand.flag, type)) {
                        // Supply is blocked this tick (e.g. its flag is full); skip it
                        supplies.remove(bestSupply);
                        continue;
                    }

                    demand.quantity--;
                    bestSupply.quantity--;
                    if (bestSupply.quantity <= 0) {
                        supplies.remove(bestSupply);
                    }
                }
            }

            // Route leftover production output or flag items of this type to nearest warehouse
            routeSurplusToWarehouses(gameState, supplies, type);
        }
    }

    /**
     * Distribution rank of a demand's consumer building for a good: its index in
     * the owner's priority order (0 = highest priority). Construction sites and
     * buildings with no stated preference rank last, so preferred consumers are
     * served first when a good is scarce.
     *
     * @param state  the game state (for the per-player priority table)
     * @param demand the demand to rank
     * @param type   the contested good
     * @return the zero-based rank, or {@link Integer#MAX_VALUE} if unranked
     */
    static int distributionRank(GameState state, Demand demand, ResourceType type) {
        Building b = demand.building;
        if (b == null) return Integer.MAX_VALUE;
        Map<ResourceType, List<BuildingName>> table = state.getDistributionFor(b.getPlayerId());
        return rankOf(table.get(type), b.getName());
    }

    /**
     * Index of {@code name} in a priority order, or {@link Integer#MAX_VALUE}
     * when the order is {@code null} or omits the building (lowest priority).
     *
     * @param order the ordered consumer list, highest priority first (may be null)
     * @param name  the consumer building type to rank (may be null)
     * @return the zero-based rank, or {@link Integer#MAX_VALUE} if unranked
     */
    static int rankOf(List<BuildingName> order, BuildingName name) {
        if (order == null || name == null) return Integer.MAX_VALUE;
        int idx = order.indexOf(name);
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }

    /**
     * Gathers all unsatisfied demands for a specific resource type.
     *
     * @param state the current game state
     * @param type  the target resource type
     * @return a list of active Demands
     */
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
                // A paused building stops pulling new input goods.
                if (!pb.isProductionPaused() && pb.getInputSlots() != null) {
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
            } else if (b instanceof MilitaryBuilding mb && type == ResourceType.COIN) {
                // Occupied military buildings request gold coins for promotions,
                // unless the player switched coin delivery off for this building.
                if (mb.isTerritoryClaimed() && mb.isCoinsAllowed()) {
                    int capacity = GameConfig.coinCapacity(mb.getBuildingName());
                    int inTransit = countInTransit(state, flag.getId(), type);
                    int netDemand = capacity - mb.getStoredCoins() - inTransit;
                    if (netDemand > 0) {
                        demands.add(new Demand(mb, flag, type, netDemand));
                    }
                }
            }
        }
        return demands;
    }

    /**
     * Gathers all available supplies for a specific resource type.
     *
     * @param state the current game state
     * @param type  the target resource type
     * @return a list of available Supplies
     */
    private List<Supply> gatherSupplies(GameState state, ResourceType type) {
        List<Supply> supplies = new ArrayList<>();

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

    /**
     * Resolves the closest supply flag from the demand flag using Dijkstra path length.
     * Prioritizes active production/flag supplies over warehouses.
     *
     * @param network    the active road network
     * @param demandFlag the destination flag of the demand
     * @param supplies   the list of available supplies
     * @return the closest Supply instance, or null if unreachable
     */
    private Supply findClosestSupply(RoadNetwork network, Flag demandFlag, List<Supply> supplies) {
        Supply bestSupply = null;
        int minDistance = Integer.MAX_VALUE;

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

    /**
     * Requests transport and decreases source stock inventory to route a matched supply.
     *
     * @param state      the active game state
     * @param supply     the selected supply source
     * @param demandFlag the target destination flag
     * @param type       the type of resource to route
     * @return {@code true} if the resource was routed, {@code false} if the supply is blocked
     */
    private boolean routeResource(GameState state, Supply supply, Flag demandFlag, ResourceType type) {
        if (supply.source == SupplySource.PRODUCTION) {
            return state.getTransportManager().requestTransportFromBuilding((ProductionBuilding) supply.provider, demandFlag);
        } else if (supply.source == SupplySource.FLAG) {
            return state.getTransportManager().requestTransport(supply.flag, type, demandFlag);
        } else if (supply.source == SupplySource.WAREHOUSE) {
            StorageBuilding sb = (StorageBuilding) supply.provider;
            Flag whFlag = sb.getAttachedFlag();
            if (whFlag == null || whFlag.isFull()
                    || sb.getStoredResources().getOrDefault(type, 0) < 1) {
                return false;
            }
            sb.retrieveResource(type);
            whFlag.addResource(type, demandFlag.getId());
            return true;
        }
        return false;
    }

    /**
     * Dispatches surplus unrouted active resources to the nearest Warehouse.
     *
     * @param state    the active game state
     * @param supplies the list of unmatched supplies
     * @param type     the target resource type
     */
    private void routeSurplusToWarehouses(GameState state, List<Supply> supplies, ResourceType type) {
        for (Supply supply : supplies) {
            if (supply.source == SupplySource.WAREHOUSE) continue;

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

    /**
     * Locates the nearest StorageBuilding relative to a coordinate.
     *
     * @param state the active game state
     * @param pos   the search coordinate position
     * @return the nearest active StorageBuilding instance, or null if none
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

    /**
     * Counts the total quantity of a resource type currently in transit to a flag.
     *
     * @param state        the current game state
     * @param targetFlagId the target flag UUID
     * @param type         the resource type to count
     * @return the count of items in transit
     */
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
}
