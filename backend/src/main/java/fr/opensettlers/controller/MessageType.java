package fr.opensettlers.controller;

/**
 * Defines the type of action to perform.
 *
 */
public enum MessageType {
    /**
     * Action to build a building.
     *
     */
    BUILD_BUILDING,

    /**
     * Action to destroy a building.
     *
     */
    DESTROY_BUILDING,

    /**
     * Action to place a flag.
     *
     */
    PLACE_FLAG,

    /**
     * Action to link flags with a road.
     *
     */
    LINK_FLAGS,

    /**
     * Action to attack a building.
     *
     */
    ATTACK_BUILDING,

    /**
     * Action to send a geologist survey the mountains around a flag.
     * Uses {@code targetId} as the destination flag ID.
     */
    SEND_GEOLOGIST,

    /**
     * Pauses or resumes production at one of the player's production buildings.
     * Uses {@code targetId} (the building) and {@code enabled} (true = produce).
     */
    SET_PRODUCTION,

    /**
     * Enables or disables gold-coin delivery to one of the player's military
     * buildings. Uses {@code targetId} (the building) and {@code enabled}.
     */
    SET_COIN_DELIVERY,

    /**
     * Sets the player's distribution priority order of consumer buildings for a
     * contested good. Uses {@code resourceType} and the ordered {@code priorities}.
     */
    SET_DISTRIBUTION,

    /**
     * Sets the player's target garrison occupation percentage (military strength).
     * Uses {@code militaryOccupation} (0–100).
     */
    SET_MILITARY
}