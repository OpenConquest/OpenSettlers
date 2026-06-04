package fr.opensettlers.engine;

import fr.opensettlers.engine.state.ProductionBuilding;
import fr.opensettlers.engine.state.Carrier;
import fr.opensettlers.engine.state.Flag;
import fr.opensettlers.engine.state.ResourceStack;
import fr.opensettlers.engine.state.Road;
import fr.opensettlers.engine.state.utils.CarrierState;
import fr.opensettlers.engine.state.utils.ResourceType;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Manages resource transport across the road network.
 *
 * <p>Each game tick, the TransportManager:
 * <ol>
 *   <li>Moves all carriers along their roads</li>
 *   <li>Handles resource pickup when carriers arrive at source flags</li>
 *   <li>Handles resource delivery when carriers arrive at destination flags</li>
 *   <li>Assigns idle carriers to pick up waiting resources</li>
 * </ol>
 *
 * <p>This class is the central orchestrator for the Settlers 2-style transport system,
 * where each road has exactly one carrier that shuttles resources one at a time between
 * the road's two endpoint flags.</p>
 */
@Getter
public class TransportManager {
    /** The road network this manager operates on. */
    private final RoadNetwork roadNetwork;

    /**
     * Creates a new TransportManager for the given road network.
     *
     * @param roadNetwork the road network to manage transport for
     */
    public TransportManager(RoadNetwork roadNetwork) {
        this.roadNetwork = roadNetwork;
    }

    /**
     * Called every game tick. Manages the full transport cycle for all carriers.
     */
    public void tick() {
        Collection<Road> allRoads = roadNetwork.getAllRoads();

        for (Road road : allRoads) {
            Carrier carrier = road.getCarrier();
            if (carrier == null) continue;

            processCarrier(carrier, road);
        }
    }

    /**
     * Processes a single carrier for one tick: movement, pickup, delivery, and idle assignment.
     */
    private void processCarrier(Carrier carrier, Road road) {
        switch (carrier.getState()) {
            case IDLE -> handleIdleCarrier(carrier, road);

            case WALKING_TO_PICKUP -> {
                carrier.moveTowardTarget();
                if (carrier.isAtTargetFlag()) {
                    handlePickup(carrier, road);
                }
            }

            case WALKING_TO_DELIVER -> {
                carrier.moveTowardTarget();
                if (carrier.isAtTargetFlag()) {
                    handleDelivery(carrier, road);
                }
            }

            case WAITING -> {
                if (carrier.getTargetFlag() != null && !carrier.getTargetFlag().isFull()) {
                    handleDelivery(carrier, road);
                }
            }
        }
    }

    /**
     * Handles an idle carrier: looks for resources on either endpoint flag
     * that need to be transported through this road.
     */
    private void handleIdleCarrier(Carrier carrier, Road road) {
        Flag startFlag = road.getStartFlag();
        Flag endFlag = road.getEndFlag();

        ResourceStack fromStart = findResourceForDirection(startFlag, endFlag);
        ResourceStack fromEnd = findResourceForDirection(endFlag, startFlag);

        if (fromStart != null && fromEnd != null) {
            int distToStart = carrier.getProgress();
            int distToEnd = road.getLength() - carrier.getProgress();
            if (distToStart <= distToEnd) {
                carrier.assignPickup(startFlag);
            } else {
                carrier.assignPickup(endFlag);
            }
        } else if (fromStart != null) {
            carrier.assignPickup(startFlag);
        } else if (fromEnd != null) {
            carrier.assignPickup(endFlag);
        }
    }

    /**
     * Handles a carrier arriving at a flag to pick up a resource.
     * Picks the best resource and starts delivery to the other flag.
     */
    private void handlePickup(Carrier carrier, Road road) {
        Flag pickupFlag = carrier.getCurrentFlag();
        if (pickupFlag == null) return;

        Flag deliveryFlag = road.getOtherFlag(pickupFlag);

        ResourceStack resource = findResourceForDirection(pickupFlag, deliveryFlag);

        if (resource != null) {
            if (pickupFlag.getResourceSlots().contains(resource)) {
                pickupFlag.popResource(resource);
            } else if (pickupFlag.getBuilding() instanceof ProductionBuilding pb) {
                pb.getOutputSlot().removeResource();
                pb.getOutputDestinations().remove(resource.getTargetFlagId());
            }
            carrier.pickupAndDeliver(resource, deliveryFlag);
        } else {
            carrier.setState(CarrierState.IDLE);
            carrier.setTargetFlag(null);
        }
    }

    /**
     * Handles a carrier arriving at a flag to deliver a resource.
     * If the flag is full, the carrier enters WAITING state.
     */
    private void handleDelivery(Carrier carrier, Road road) {
        Flag deliveryFlag = carrier.getTargetFlag();
        if (deliveryFlag == null) return;

        if (deliveryFlag.isFull()) {
            carrier.setState(CarrierState.WAITING);
            return;
        }

        ResourceStack resource = carrier.deliver();
        if (resource != null) {
            deliveryFlag.addResource(resource.getType(), resource.getTargetFlagId());
        }
    }

    /**
     * Finds a resource on the source flag that should be transported toward the destination flag.
     *
     * <p>A resource qualifies if its target destination is reachable through the destination flag,
     * i.e., the shortest path from the source to the resource's target passes through destFlag.</p>
     *
     * @param sourceFlag the flag to search for resources
     * @param destFlag   the adjacent flag (other end of the road)
     * @return the best resource to transport, or {@code null} if none qualifies
     */
    private ResourceStack findResourceForDirection(Flag sourceFlag, Flag destFlag) {
        ResourceStack bestResource = null;
        int bestDistance = Integer.MAX_VALUE;

        for (ResourceStack rs : sourceFlag.getResourceSlots()) {
            if (rs.getTargetFlagId() == null) continue;

            Flag targetFlag = roadNetwork.getFlagById(rs.getTargetFlagId());
            if (targetFlag == null) continue;

            if (rs.getTargetFlagId().equals(destFlag.getId())) {
                return rs;
            }

            List<Flag> pathFromSource = roadNetwork.findPath(sourceFlag, targetFlag);
            if (pathFromSource != null && pathFromSource.size() >= 2
                    && pathFromSource.get(1).getId().equals(destFlag.getId())) {
                int dist = pathFromSource.size();
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestResource = rs;
                }
            }
        }

        if (bestResource == null && sourceFlag.getBuilding() instanceof ProductionBuilding pb) {
            if (pb.getOutputSlot() != null) {
                for (UUID targetId : pb.getOutputDestinations()) {
                    Flag targetFlag = roadNetwork.getFlagById(targetId);
                    if (targetFlag == null) continue;

                    if (targetId.equals(destFlag.getId())) {
                        return new ResourceStack(pb.getOutputSlot().getType(), targetId);
                    }

                    List<Flag> path = roadNetwork.findPath(sourceFlag, targetFlag);
                    if (path != null && path.size() >= 2 && path.get(1).getId().equals(destFlag.getId())) {
                        int dist = path.size();
                        if (dist < bestDistance) {
                            bestDistance = dist;
                            bestResource = new ResourceStack(pb.getOutputSlot().getType(), targetId);
                        }
                    }
                }
            }
        }

        return bestResource;
    }

    /**
     * Requests transport of a resource from a source flag to a destination flag.
     *
     * <p>Finds an unrouted resource of the given type on the source flag and assigns
     * the destination. The carriers will then automatically route it hop by hop.</p>
     *
     * @param source      the flag where the resource is waiting
     * @param type        the type of resource to route
     * @param destination the final destination flag
     * @return {@code true} if a resource was successfully routed
     */
    public boolean requestTransport(Flag source, ResourceType type, Flag destination) {
        for (ResourceStack rs : source.getResourceSlots()) {
            if (rs.getType() == type && rs.getTargetFlagId() == null) {
                rs.setTargetFlagId(destination.getId());
                return true;
            }
        }
        return false;
    }

    /**
     * Requests transport directly from a building's output inventory to a destination flag.
     *
     * @param building    the building holding the resource
     * @param destination the final destination flag
     * @return {@code true} if transport was successfully requested
     */
    public boolean requestTransportFromBuilding(ProductionBuilding building, Flag destination) {
        if (building.getOutputSlot() != null && building.getOutputSlot().getQuantity() > building.getOutputDestinations().size()) {
            building.getOutputDestinations().add(destination.getId());
            return true;
        }
        return false;
    }

    /**
     * Routes all unrouted resources on the given flag to the nearest warehouse or storage building.
     * This is a convenience method for automatic resource distribution.
     *
     * @param flag            the flag with unrouted resources
     * @param warehouseFlagId the UUID of the nearest warehouse's flag
     */
    public void routeUnroutedResources(Flag flag, UUID warehouseFlagId) {
        for (ResourceStack rs : flag.getResourceSlots()) {
            if (rs.getTargetFlagId() == null) {
                rs.setTargetFlagId(warehouseFlagId);
            }
        }
    }
}
