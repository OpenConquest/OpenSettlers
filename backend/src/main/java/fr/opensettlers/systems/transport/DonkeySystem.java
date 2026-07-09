package fr.opensettlers.systems.transport;

import fr.opensettlers.utils.GameConfig;
import fr.opensettlers.state.GameState;
import fr.opensettlers.state.RoadNetwork;
import fr.opensettlers.entities.building.Building;
import fr.opensettlers.entities.unit.Donkey;
import fr.opensettlers.entities.world.Flag;
import fr.opensettlers.entities.building.ProcessingBuilding;
import fr.opensettlers.entities.world.Road;
import fr.opensettlers.utils.enums.DonkeyState;
import fr.opensettlers.utils.enums.ResourceType;

import java.util.List;
import fr.opensettlers.systems.ISystem;

/**
 * System managing main roads and donkeys:
 * <ol>
 *   <li>Upgrades heavily used roads to level 2 (main roads) after
 *       {@link GameConfig#ROAD_UPGRADE_DELIVERIES} deliveries.</li>
 *   <li>Assigns donkeys to level-2 roads, reusing idle donkeys first, then
 *       taking freshly bred ones from a donkey breeder's output.</li>
 *   <li>Moves walking donkeys along the flag network; once on their road
 *       they double the carrier's speed (see TransportManager).</li>
 * </ol>
 */
public class DonkeySystem implements ISystem {

    /**
     * Processes road upgrades and the donkey lifecycle for the current tick.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        upgradeBusyRoads(gameState);
        releaseOrphanedDonkeys(gameState);
        assignDonkeysToMainRoads(gameState);
        moveWalkingDonkeys(gameState);
    }

    /**
     * Promotes level-1 roads that completed enough deliveries to level 2.
     *
     * @param state the current game state
     */
    private void upgradeBusyRoads(GameState state) {
        for (Road road : state.getRoadNetwork().getAllRoads()) {
            if (road.getLevel() == 1 && road.getTransportCount() >= GameConfig.ROAD_UPGRADE_DELIVERIES) {
                road.setLevel(2);
            }
        }
    }

    /**
     * Sets donkeys free when their assigned road no longer exists, making
     * them available for reassignment.
     *
     * @param state the current game state
     */
    private void releaseOrphanedDonkeys(GameState state) {
        RoadNetwork network = state.getRoadNetwork();
        for (Donkey donkey : state.getDonkeys()) {
            Road road = donkey.getAssignedRoad();
            if (road != null && network.getRoadById(road.getId()) == null) {
                donkey.setAssignedRoad(null);
                donkey.setPath(null);
                donkey.setState(DonkeyState.IDLE);
            }
        }
    }

    /**
     * Finds every level-2 road without a donkey (assisting or en route) and
     * assigns one: an idle donkey if available, otherwise a freshly bred one
     * from the nearest donkey breeder of the road's owner.
     *
     * @param state the current game state
     */
    private void assignDonkeysToMainRoads(GameState state) {
        for (Road road : state.getRoadNetwork().getAllRoads()) {
            if (road.getLevel() < 2 || hasDonkeyAssigned(state, road)) {
                continue;
            }

            int ownerId = road.getStartFlag().getPlayerId();
            Donkey donkey = findIdleDonkey(state, ownerId);
            if (donkey == null) {
                donkey = breedDonkey(state, ownerId);
            }
            if (donkey == null) {
                continue; // No donkey available for this road yet
            }

            donkey.setAssignedRoad(road);
            sendDonkeyToRoad(state, donkey, road);
        }
    }

    /**
     * Checks whether a donkey is already assisting or walking toward the road.
     *
     * @param state the current game state
     * @param road  the road to check
     * @return {@code true} if the road already has a donkey assigned
     */
    private boolean hasDonkeyAssigned(GameState state, Road road) {
        if (road.isDonkeyAssisted()) {
            return true;
        }
        for (Donkey donkey : state.getDonkeys()) {
            if (road.equals(donkey.getAssignedRoad())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds an idle donkey owned by the given player.
     *
     * @param state    the current game state
     * @param playerId the owner to match
     * @return an idle donkey, or {@code null}
     */
    private Donkey findIdleDonkey(GameState state, int playerId) {
        for (Donkey donkey : state.getDonkeys()) {
            if (donkey.getPlayerId() == playerId && donkey.getState() == DonkeyState.IDLE) {
                return donkey;
            }
        }
        return null;
    }

    /**
     * Takes one bred donkey out of a donkey breeder's output and spawns the
     * corresponding unit at the breeder.
     *
     * @param state    the current game state
     * @param playerId the owner to match
     * @return the spawned donkey, or {@code null} if no breeder has stock
     */
    private Donkey breedDonkey(GameState state, int playerId) {
        for (Building b : state.getBuildings()) {
            if (!(b instanceof ProcessingBuilding pb) || pb.isDestroyed()
                    || pb.getPlayerId() != playerId
                    || pb.getOutputSlot() == null
                    || pb.getOutputSlot().getType() != ResourceType.DONKEY
                    || pb.getOutputSlot().getQuantity() <= 0) {
                continue;
            }
            pb.getOutputSlot().removeResource();
            Donkey donkey = new Donkey(playerId, pb.getPosition());
            state.getDonkeys().add(donkey);
            return donkey;
        }
        return null;
    }

    /**
     * Computes the donkey's walking path (via flags) to its road's start flag.
     * If no path exists, the donkey is teleport-spawned on the road directly:
     * better a working main road than a stuck donkey.
     *
     * @param state  the current game state
     * @param donkey the donkey to route
     * @param road   the destination road
     */
    private void sendDonkeyToRoad(GameState state, Donkey donkey, Road road) {
        Flag sourceFlag = findClosestFlag(state.getRoadNetwork(), donkey);
        List<Flag> path = sourceFlag != null
                ? state.getRoadNetwork().findPath(sourceFlag, road.getStartFlag())
                : null;

        if (path != null && !path.isEmpty()) {
            donkey.setPath(path);
            donkey.setCurrentPathIndex(0);
            donkey.setState(DonkeyState.WALKING_TO_ROAD);
        } else {
            arriveAtRoad(donkey, road);
        }
    }

    /**
     * Advances walking donkeys one flag per tick; on arrival they start
     * assisting their road.
     *
     * @param state the current game state
     */
    private void moveWalkingDonkeys(GameState state) {
        for (Donkey donkey : state.getDonkeys()) {
            if (donkey.getState() != DonkeyState.WALKING_TO_ROAD) {
                continue;
            }
            List<Flag> path = donkey.getPath();
            if (path != null && donkey.getCurrentPathIndex() < path.size()) {
                Flag nextFlag = path.get(donkey.getCurrentPathIndex());
                donkey.setPosition(nextFlag.getCoordinates());
                donkey.setCurrentPathIndex(donkey.getCurrentPathIndex() + 1);
            } else {
                Road road = donkey.getAssignedRoad();
                if (road != null) {
                    arriveAtRoad(donkey, road);
                } else {
                    donkey.setState(DonkeyState.IDLE);
                }
            }
        }
    }

    /**
     * Marks the donkey as assisting its road.
     *
     * @param donkey the arriving donkey
     * @param road   the road being assisted
     */
    private void arriveAtRoad(Donkey donkey, Road road) {
        donkey.setPath(null);
        donkey.setPosition(road.getStartFlag().getCoordinates());
        donkey.setState(DonkeyState.ASSISTING);
        road.setDonkey(donkey);
    }

    /**
     * Resolves the closest flag to the donkey's current position.
     *
     * @param network the road network
     * @param donkey  the donkey to locate
     * @return the closest flag, or {@code null} if the network is empty
     */
    private Flag findClosestFlag(RoadNetwork network, Donkey donkey) {
        Flag closest = null;
        double minDist = Double.MAX_VALUE;
        for (Flag flag : network.getAllFlags()) {
            double dist = flag.getCoordinates().distanceTo(donkey.getPosition());
            if (dist < minDist) {
                minDist = dist;
                closest = flag;
            }
        }
        return closest;
    }
}
