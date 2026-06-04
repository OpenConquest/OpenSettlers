package fr.opensettlers.services;

import fr.opensettlers.engine.BuildingFactory;
import fr.opensettlers.engine.GameState;
import fr.opensettlers.entities.Building;
import fr.opensettlers.entities.Flag;
import fr.opensettlers.entities.MilitaryBuilding;
import fr.opensettlers.network.GameMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing active game states and processing player actions.
 */
@ApplicationScoped
public class GameEngineService {

    /** Map of active games, indexed by GameId. */
    private final Map<UUID, GameState> activeGames = new ConcurrentHashMap<>();

    /**
     * Retrieves a game state.
     */
    public GameState getGame(UUID gameId) {
        return activeGames.get(gameId);
    }

    /**
     * Initializes and registers a new game.
     */
    public void createGame(UUID gameId, GameState state) {
        activeGames.put(gameId, state);
    }

    /**
     * Processes an incoming WebSocket message and applies the action to the GameState.
     */
    public void processMessage(UUID gameId, GameMessage message) {
        GameState state = activeGames.get(gameId);
        if (state == null) {
            System.err.println("Game not found: " + gameId);
            return;
        }

        switch (message.getType()) {
            case BUILD_BUILDING -> {
                // Create the building (the attached Flag is generated automatically)
                Building building = BuildingFactory.createBuilding(
                        message.getBuildingName(), 
                        message.getPlayerId(), 
                        message.getPosition()
                );
                state.getBuildings().add(building);
                
                // Explicitly register the attached flag to the road network
                state.getRoadNetwork().addFlag(building.getAttachedFlag());
                System.out.println("Building " + message.getBuildingName() + " constructed at " + message.getPosition());
            }

            case DESTROY_BUILDING -> {
                Building b = state.getBuildings().stream()
                        .filter(x -> x.getId().equals(message.getTargetId()))
                        .findFirst()
                        .orElse(null);
                
                if (b != null && b.getPlayerId() == message.getPlayerId()) {
                    b.destroy(); // Flags and building marked as destroyed
                    System.out.println("Building " + b.getId() + " destroyed.");
                }
            }

            case PLACE_FLAG -> {
                // Place a free-standing flag (e.g. for future crossroads)
                Flag flag = new Flag(UUID.randomUUID(), message.getPlayerId(), message.getPosition());
                state.getFlags().add(flag);
                state.getRoadNetwork().addFlag(flag);
                System.out.println("Flag placed at " + message.getPosition());
            }

            case LINK_FLAGS -> {
                Flag flagA = state.getRoadNetwork().getFlagById(message.getFlagIdA());
                Flag flagB = state.getRoadNetwork().getFlagById(message.getFlagIdB());
                
                if (flagA != null && flagB != null) {
                    state.getRoadNetwork().addRoad(flagA, flagB, message.getPath());
                    System.out.println("Road created between " + flagA.getId() + " and " + flagB.getId());
                } else {
                    System.err.println("Failed to link flags: a flag was not found.");
                }
            }

            case ATTACK_BUILDING -> {
                // TODO: Implement military targeting
                Building target = state.getBuildings().stream()
                        .filter(x -> x.getId().equals(message.getTargetId()))
                        .findFirst()
                        .orElse(null);
                
                if (target instanceof MilitaryBuilding) {
                    System.out.println("Attack order sent to building " + target.getId());
                    // Route soldiers to enemy building logic
                }
            }
        }
    }
}
