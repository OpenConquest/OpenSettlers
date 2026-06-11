package fr.opensettlers.engine.state.utils;

/**
 * State machine states for soldier units.
 */
public enum SoldierState {
    /**
     * Soldier is walking toward a friendly military building to garrison it.
     */
    WALKING_TO_GARRISON,

    /**
     * Soldier is stationed inside a military building.
     */
    GARRISONED,

    /**
     * Soldier is marching toward an enemy building to attack it.
     */
    ATTACKING,

    /**
     * Soldier is engaged in combat with an enemy soldier.
     */
    FIGHTING
}
