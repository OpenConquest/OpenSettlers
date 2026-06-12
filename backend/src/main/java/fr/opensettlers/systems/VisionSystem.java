package fr.opensettlers.systems;

import fr.opensettlers.state.GameState;

/**
 * System refreshing the fog of war each tick from the players' buildings,
 * flags and units. Runs last so the broadcast sees up-to-date exploration.
 */
public class VisionSystem implements ISystem {

    /**
     * Updates the per-player explored tile sets.
     *
     * @param gameState the active game session state
     */
    @Override
    public void process(GameState gameState) {
        gameState.getFogOfWar().update(gameState);
    }
}
