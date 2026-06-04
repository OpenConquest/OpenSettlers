package fr.opensettlers.engine.systems;

import fr.opensettlers.engine.GameState;

public interface ISystem {
    /**
     * This function is called every tick by the engine to midify the game state.
     *
     * @param gameState Game state of the current session.
     */
    void process(GameState gameState);
}
