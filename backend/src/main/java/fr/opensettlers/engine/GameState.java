package fr.opensettlers.engine;

import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.Flag;
import fr.opensettlers.engine.state.Soldier;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores all data in a game session and handles the game loop.
 */
@Data
public class GameState {
    /** Unique identifier of the game session. */
    private final UUID gameId;

    /** Unique identifiers of the players in the game. Should not contain duplicates. */
    private final List<UUID> playerIds;

    /** The road network managing flags, roads, and pathfinding. */
    private final RoadNetwork roadNetwork = new RoadNetwork();

    /** The transport manager handling resource logistics. */
    private final TransportManager transportManager = new TransportManager(roadNetwork);

    /** All building instances in the map. */
    private final List<Building> buildings = new ArrayList<>();
    /** All flag instances in the map. */
    private final List<Flag> flags = new ArrayList<>();
    /** All soldier instances in the map. */
    private final List<Soldier> soldiers = new ArrayList<>();

    /** Current tick since the start of the game. */
    private long currentTick = 0;

    /**
     * Handles player quitting. Soldiers and buildings aren't removed and stay autonomous.
     * @param playerId The Unique identifier of the player to remove.
     */
    public void playerQuits(UUID playerId) {
        if (!playerIds.contains(playerId)) {
            throw new RuntimeException(String.format("Player %s does not exist", playerId));
        }

        playerIds.remove(playerId);
    }

    /** Makes the game loop go forward one tick. */
    public void tick() {
        currentTick++;

        // Sync flags to road network (in case new ones were added to the list but not the network)
        for (Flag flag : flags) {
            if (roadNetwork.getFlagById(flag.getId()) == null) {
                roadNetwork.addFlag(flag);
            }
        }

        // Flags
        flags.removeIf(Flag::isDestroyed);

        // Buildings
        buildings.removeIf(Building::isDestroyed);

        // Soldiers
        soldiers.removeIf(Soldier::isDead);
    }
}
