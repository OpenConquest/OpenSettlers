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
    SEND_GEOLOGIST
}