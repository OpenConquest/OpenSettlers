package fr.opensettlers.engine;

import fr.opensettlers.engine.state.Carrier;
import fr.opensettlers.engine.state.Flag;
import fr.opensettlers.engine.state.Road;
import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Getter;

import java.util.*;

/**
 * Graph structure representing the network of flags connected by roads.
 *
 * <p>Provides road/flag management and Dijkstra-based pathfinding to determine
 * the shortest route between any two flags in the network.</p>
 */
@Getter
public class RoadNetwork {
    /** All flags in the network, keyed by their UUID. */
    private final Map<UUID, Flag> flags = new HashMap<>();

    /** All roads in the network, keyed by their UUID. */
    private final Map<UUID, Road> roads = new HashMap<>();

    /**
     * Registers a flag in the network.
     *
     * @param flag the flag to add
     */
    public void addFlag(Flag flag) {
        flags.put(flag.getId(), flag);
    }

    /**
     * Removes a flag and all its connected roads from the network.
     *
     * @param flagId the UUID of the flag to remove
     */
    public void removeFlag(UUID flagId) {
        Flag flag = flags.get(flagId);
        if (flag == null) return;

        List<Road> roadsToRemove = new ArrayList<>(flag.getConnectedRoads());
        for (Road road : roadsToRemove) {
            removeRoad(road.getId());
        }
        flags.remove(flagId);
    }

    /**
     * Creates a new road between two flags with the given intermediate path,
     * and automatically assigns a carrier to it.
     *
     * @param flagA the flag at one end
     * @param flagB the flag at the other end
     * @param path  intermediate tile coordinates (excluding flag positions)
     * @return the created road
     * @throws IllegalArgumentException if either flag is not in the network
     */
    public Road addRoad(Flag flagA, Flag flagB, List<Coordinates> path) {
        if (!flags.containsKey(flagA.getId())) {
            throw new IllegalArgumentException("Flag A is not in the network");
        }
        if (!flags.containsKey(flagB.getId())) {
            throw new IllegalArgumentException("Flag B is not in the network");
        }

        Road road = new Road(UUID.randomUUID(), flagA, flagB, path);
        roads.put(road.getId(), road);

        flagA.connectRoad(road);
        flagB.connectRoad(road);

        Carrier carrier = new Carrier(UUID.randomUUID(), flagA.getPlayerId(), road);
        road.setCarrier(carrier);

        return road;
    }

    /**
     * Removes a road from the network and disconnects it from its flags.
     *
     * @param roadId the UUID of the road to remove
     */
    public void removeRoad(UUID roadId) {
        Road road = roads.get(roadId);
        if (road == null) return;

        road.getStartFlag().disconnectRoad(road);
        road.getEndFlag().disconnectRoad(road);
        roads.remove(roadId);
    }

    /**
     * Retrieves a flag by its UUID.
     *
     * @param flagId the flag UUID
     * @return the flag, or {@code null} if not found
     */
    public Flag getFlagById(UUID flagId) {
        return flags.get(flagId);
    }

    /**
     * Retrieves a road by its UUID.
     *
     * @param roadId the road UUID
     * @return the road, or {@code null} if not found
     */
    public Road getRoadById(UUID roadId) {
        return roads.get(roadId);
    }

    /**
     * Returns all roads in the network.
     *
     * @return unmodifiable collection of all roads
     */
    public Collection<Road> getAllRoads() {
        return Collections.unmodifiableCollection(roads.values());
    }

    /**
     * Returns all flags in the network.
     *
     * @return unmodifiable collection of all flags
     */
    public Collection<Flag> getAllFlags() {
        return Collections.unmodifiableCollection(flags.values());
    }

    /**
     * Finds the shortest path between two flags using Dijkstra's algorithm.
     * The weight of each edge is the road length (number of path segments + 1).
     *
     * @param source      the starting flag
     * @param destination the destination flag
     * @return ordered list of flags from source to destination (inclusive),
     *         or {@code null} if no path exists
     */
    public List<Flag> findPath(Flag source, Flag destination) {
        if (source == null || destination == null) return null;
        if (source.getId().equals(destination.getId())) return List.of(source);

        Map<UUID, Integer> distances = new HashMap<>();
        Map<UUID, UUID> previous = new HashMap<>();
        Set<UUID> visited = new HashSet<>();

        PriorityQueue<UUID> queue = new PriorityQueue<>(
                Comparator.comparingInt(id -> distances.getOrDefault(id, Integer.MAX_VALUE))
        );

        distances.put(source.getId(), 0);
        queue.add(source.getId());

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();

            if (!visited.add(currentId)) continue;

            if (currentId.equals(destination.getId())) break;

            Flag current = flags.get(currentId);
            if (current == null) continue;

            int currentDist = distances.get(currentId);

            for (Road road : current.getConnectedRoads()) {
                Flag neighbor = road.getOtherFlag(current);
                UUID neighborId = neighbor.getId();

                if (visited.contains(neighborId)) continue;

                int newDist = currentDist + road.getLength();

                if (newDist < distances.getOrDefault(neighborId, Integer.MAX_VALUE)) {
                    distances.put(neighborId, newDist);
                    previous.put(neighborId, currentId);
                    queue.add(neighborId);
                }
            }
        }

        if (!previous.containsKey(destination.getId())) return null;

        List<Flag> path = new ArrayList<>();
        UUID currentId = destination.getId();
        while (currentId != null) {
            path.add(0, flags.get(currentId));
            currentId = previous.get(currentId);
        }

        return path;
    }

    /**
     * Gets the next flag on the shortest path from current to destination.
     *
     * @param current     the flag to start from
     * @param destination the target flag
     * @return the next flag on the path, or {@code null} if no path exists
     *         or the current flag is already the destination
     */
    public Flag getNextFlag(Flag current, Flag destination) {
        List<Flag> path = findPath(current, destination);
        if (path == null || path.size() < 2) return null;
        return path.get(1);
    }

    /**
     * Checks whether two flags are directly connected by a road.
     *
     * @param flagA first flag
     * @param flagB second flag
     * @return {@code true} if a direct road exists between them
     */
    public boolean areDirectlyConnected(Flag flagA, Flag flagB) {
        return flagA.getConnectedRoads().stream()
                .anyMatch(road -> road.getOtherFlag(flagA).getId().equals(flagB.getId()));
    }

    /**
     * Checks whether a path exists between two flags.
     *
     * @param source      starting flag
     * @param destination target flag
     * @return {@code true} if a path exists
     */
    public boolean isReachable(Flag source, Flag destination) {
        return findPath(source, destination) != null;
    }
}
