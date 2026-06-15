package fr.opensettlers.systems;

import fr.opensettlers.state.GameState;

/**
 * A simulation system: one self-contained slice of game logic (production,
 * transport, combat, …) run once per tick by the {@link fr.opensettlers.service.GameEngine}.
 * Systems are stateless and operate exclusively on the {@link GameState} passed
 * to them, which lets the engine compose them in a fixed order each tick.
 */
public interface ISystem {
    /**
     * This function is called every tick by the engine to midify the game state.
     *
     * @param gameState Game state of the current session.
     */
    void process(GameState gameState);
}
