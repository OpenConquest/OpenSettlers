package fr.opensettlers.network;

import fr.opensettlers.engine.state.utils.BuildingName;
import fr.opensettlers.engine.state.utils.Coordinates;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Represents an incoming WebSocket message sent by the game client.
 */
@Data
public class GameMessage {
    /** The type of action to perform. */
    private MessageType type;
    
    /** The player's ID triggering the action. */
    private int playerId;
    
    // --- Attributes for BUILD_BUILDING ---
    private BuildingName buildingName;
    private Coordinates position;
    
    // --- Attributes for DESTROY_BUILDING / ATTACK_BUILDING ---
    /** The target building ID to destroy or attack. */
    private UUID targetId;
    
    // --- Attributes for LINK_FLAGS ---
    private UUID flagIdA;
    private UUID flagIdB;
    /** Intermediate path coordinates (excluding the two endpoint flags). */
    private List<Coordinates> path;
    
    public enum MessageType {
        BUILD_BUILDING,
        DESTROY_BUILDING,
        PLACE_FLAG,
        LINK_FLAGS,
        ATTACK_BUILDING
    }
}
