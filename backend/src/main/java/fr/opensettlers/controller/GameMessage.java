package fr.opensettlers.controller;

import fr.opensettlers.utils.BuildingName;
import fr.opensettlers.utils.Coordinates;
import fr.opensettlers.utils.ResourceType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Represents an incoming WebSocket message sent by the game client.
 *
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
    @com.fasterxml.jackson.annotation.JsonProperty("flagIdA")
    private UUID flagIdA;
    @com.fasterxml.jackson.annotation.JsonProperty("flagIdB")
    private UUID flagIdB;
    /** Intermediate path coordinates (excluding the two endpoint flags). */
    private List<Coordinates> path;

    // --- Attributes for SET_PRODUCTION / SET_COIN_DELIVERY ---
    /** Toggle value: true to produce / deliver coins, false to pause / stop. */
    private boolean enabled;

    // --- Attributes for SET_DISTRIBUTION ---
    /** The contested good whose distribution order is being set. */
    private ResourceType resourceType;
    /** Ordered consumer building types, highest priority first. */
    private List<BuildingName> priorities;

    // --- Attributes for SET_MILITARY ---
    /** Target garrison occupation percentage (0–100). */
    private int militaryOccupation;
}
