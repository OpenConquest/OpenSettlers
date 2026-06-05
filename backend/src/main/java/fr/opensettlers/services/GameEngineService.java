package fr.opensettlers.services;

import fr.opensettlers.engine.GameConfig;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.engine.state.Building;
import fr.opensettlers.engine.state.ConstructionSite;
import fr.opensettlers.engine.state.Flag;
import fr.opensettlers.engine.state.MilitaryBuilding;
import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.network.GameMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Service managing active game states and processing player actions.
 *
 */
@ApplicationScoped
public class GameEngineService {

    private static final Logger LOG = Logger.getLogger(GameEngineService.class);

    /** Map of active games, indexed by GameId. */
    private final Map<UUID, GameState> activeGames = new ConcurrentHashMap<>();

    /**
     * Retrieves a game state.
     *
     * @param gameId the unique identifier of the game to retrieve
     * @return the game state corresponding to the game ID, or null if not found
     */
    public GameState getGame(UUID gameId) {
        return activeGames.get(gameId);
    }

    /**
     * Initializes and registers a new game.
     *
     * @param gameId the unique identifier of the new game
     * @param state the initial state of the new game
     */
    public void createGame(UUID gameId, GameState state) {
        activeGames.put(gameId, state);
    }

    /**
     * Processes an incoming WebSocket message and applies the action to the GameState.
     *
     * @param gameId the unique identifier of the game to process the message for
     * @param message the incoming game message containing the player's action
     */
    public void processMessage(UUID gameId, GameMessage message) {
        GameState state = activeGames.get(gameId);
        if (state == null) {
            LOG.errorf("Game not found: %s", gameId);
            return;
        }

        switch (message.getType()) {
            case BUILD_BUILDING -> {
                // Validate special placement constraints
                if (message.getBuildingName() == BuildingName.FISHING_HUT) {
                    if (!state.hasWaterInRange(message.getPosition(), GameConfig.FISHERMAN_MAX_DISTANCE)) {
                        LOG.warnf("Cannot place Fishing Hut at %s: no water within %d tiles",
                                message.getPosition(), fr.opensettlers.engine.GameConfig.FISHERMAN_MAX_DISTANCE);
                        return;
                    }
                }

                // Create a Construction Site instead of building directly
                ConstructionSite site = new ConstructionSite(
                        message.getPlayerId(),
                        message.getPosition(),
                        message.getBuildingName()
                );
                state.getBuildings().add(site);
                
                // Explicitly register the attached flag to the road network
                state.getRoadNetwork().addFlag(site.getAttachedFlag());
                LOG.infof("Construction site for %s placed at %s", message.getBuildingName(), message.getPosition());
            }

            case DESTROY_BUILDING -> {
                Building b = state.getBuildings().stream()
                        .filter(x -> x.getId().equals(message.getTargetId()))
                        .findFirst()
                        .orElse(null);
                
                if (b != null && b.getPlayerId() == message.getPlayerId()) {
                    b.destroy(); // Flags and building marked as destroyed
                    LOG.infof("Building %s destroyed.", b.getId());
                }
            }

            case PLACE_FLAG -> {
                // Place a free-standing flag (e.g. for future crossroads)
                Flag flag = new Flag(UUID.randomUUID(), message.getPlayerId(), message.getPosition());
                state.getFlags().add(flag);
                state.getRoadNetwork().addFlag(flag);
                LOG.infof("Flag placed at %s", message.getPosition());
            }

            case LINK_FLAGS -> {
                Flag flagA = state.getRoadNetwork().getFlagById(message.getFlagIdA());
                Flag flagB = state.getRoadNetwork().getFlagById(message.getFlagIdB());
                
                if (flagA != null && flagB != null) {
                    state.getRoadNetwork().addRoad(flagA, flagB, message.getPath());
                    LOG.infof("Road created between %s and %s", flagA.getId(), flagB.getId());
                } else {
                    LOG.warn("Failed to link flags: a flag was not found.");
                }
            }

            case ATTACK_BUILDING -> {
                // TODO: Implement military targeting
                Building target = state.getBuildings().stream()
                        .filter(x -> x.getId().equals(message.getTargetId()))
                        .findFirst()
                        .orElse(null);
                
                if (target instanceof MilitaryBuilding) {
                    LOG.infof("Attack order sent to building %s", target.getId());
                    // Route soldiers to enemy building logic
                }
            }
        }
    }
}
