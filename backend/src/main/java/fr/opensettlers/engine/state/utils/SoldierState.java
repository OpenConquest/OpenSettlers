package fr.opensettlers.engine.state.utils;

/**
 * State machine states for military soldier units.
 */
public enum SoldierState {
    /** Soldier is walking from storage toward a military building to garrison. */
    WALKING_TO_GARRISON,

    /** Soldier is stationed inside a military building. */
    GARRISONED,

    /** Soldier is marching toward an enemy building to attack. */
    MARCHING_TO_ATTACK,

    /** Soldier is engaged in a 1v1 duel. */
    FIGHTING,

    /** Soldier has won and is waiting for the next defender. */
    WAITING_FOR_DEFENDER,

    /** Defender soldier is walking from nearby military building to reinforce. */
    WALKING_TO_DEFEND
}
