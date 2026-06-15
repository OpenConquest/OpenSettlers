package fr.opensettlers.service.commands;

import fr.opensettlers.state.GameState;

/**
 * A deferred player action queued on a {@link fr.opensettlers.state.GameSession}
 * and applied at the start of the next tick, on the game-loop thread. Running
 * commands on the loop thread keeps all {@link GameState} mutation
 * single-threaded, avoiding races with the simulation systems.
 */
public interface GameCommand {
    /**
     * Returns the player that issued this command.
     *
     * @return the player ID
     */
    int getPlayerId();

    /**
     * Applies this command to the game state. Called on the game-loop thread.
     *
     * @param state the game state to mutate
     */
    void execute(GameState state);
}
