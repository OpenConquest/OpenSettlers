package fr.opensettlers.systems;

import fr.opensettlers.state.GameState;

public interface ISystem {
    /**
     * This function is called every tick by the engine to midify the game state.
     *
     * @param gameState Game state of the current session.
     */
    void process(GameState gameState);
}
